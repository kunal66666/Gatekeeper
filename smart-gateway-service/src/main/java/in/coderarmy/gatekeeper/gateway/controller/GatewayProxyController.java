package in.coderarmy.gatekeeper.gateway.controller;

import in.coderarmy.gatekeeper.gateway.dto.RouteDefinition;
import in.coderarmy.gatekeeper.gateway.service.RateLimiterService;
import in.coderarmy.gatekeeper.gateway.service.RouteRegistryService;
import in.coderarmy.gatekeeper.gateway.service.TelemetryProducerService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Enumeration;
import java.util.Optional;
import java.util.Set;

@RestController
@RequiredArgsConstructor
@Slf4j
public class GatewayProxyController {

    private static final Set<String> HOP_BY_HOP_HEADERS = Set.of(
        "connection", "keep-alive", "proxy-authenticate", "proxy-authorization",
        "te", "trailers", "transfer-encoding", "upgrade", "host", "content-length"
    );

    private final RouteRegistryService routeRegistry;
    private final RateLimiterService rateLimiter;
    private final TelemetryProducerService telemetryProducer;

    @RequestMapping(value = "/api/**")
    public ResponseEntity<byte[]> handleProxyRequest(
            HttpServletRequest request,
            @RequestBody(required = false) byte[] body) {

        long startTime = System.currentTimeMillis();
        String path = request.getRequestURI();
        String method = request.getMethod();
        String clientIp = resolveClientIp(request);

        // 1. Route Matching
        Optional<RouteDefinition> matchedRoute = routeRegistry.matchRoute(path, method);
        if (matchedRoute.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .contentType(MediaType.APPLICATION_JSON)
                .body(String.format("{\"error\": \"Not Found\", \"message\": \"No route configured for [%s %s]\"}",
                        method, path).getBytes());
        }

        RouteDefinition route = matchedRoute.get();

        // 2. Distributed Rate Limiting Check
        if (!rateLimiter.isAllowed(route, clientIp)) {
            long latency = System.currentTimeMillis() - startTime;
            telemetryProducer.record(route.routeId(), clientIp, method, path, HttpStatus.TOO_MANY_REQUESTS.value(), latency);

            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .contentType(MediaType.APPLICATION_JSON)
                .header("Retry-After", "60")
                .header("X-RateLimit-Limit", String.valueOf(route.requestsPerMinute()))
                .body(String.format("{\"error\": \"Too Many Requests\", \"message\": \"Rate limit of %d req/min exceeded for %s\"}",
                        route.requestsPerMinute(), route.routeId()).getBytes());
        }

        // 3. Resolve Target URL
        String targetUrl = buildTargetUrl(route, path, request.getQueryString());

        // 4. Construct Forwarding RestClient with Route-Specific Timeout
        int timeoutMs = route.responseTimeoutMs() > 0 ? route.responseTimeoutMs() : 5000;
        HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofMillis(timeoutMs))
            .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofMillis(timeoutMs));

        RestClient upstreamClient = RestClient.builder()
            .requestFactory(requestFactory)
            .build();

        try {
            RestClient.RequestBodySpec requestSpec = upstreamClient.method(HttpMethod.valueOf(method))
                .uri(targetUrl)
                .headers(headers -> copyHeaders(request, headers));

            if (body != null && body.length > 0) {
                requestSpec.body(body);
            }

            ResponseEntity<byte[]> upstreamResponse = requestSpec.exchange((clientReq, clientResp) -> {
                HttpHeaders responseHeaders = new HttpHeaders();
                clientResp.getHeaders().forEach((headerName, headerValues) -> {
                    if (!HOP_BY_HOP_HEADERS.contains(headerName.toLowerCase())) {
                        responseHeaders.addAll(headerName, headerValues);
                    }
                });

                byte[] responseBody = clientResp.getBody().readAllBytes();
                return new ResponseEntity<>(responseBody, responseHeaders, clientResp.getStatusCode());
            });

            long latency = System.currentTimeMillis() - startTime;
            int statusCode = upstreamResponse != null ? upstreamResponse.getStatusCode().value() : 200;
            telemetryProducer.record(route.routeId(), clientIp, method, path, statusCode, latency);

            return upstreamResponse;

        } catch (ResourceAccessException ex) {
            long latency = System.currentTimeMillis() - startTime;
            telemetryProducer.record(route.routeId(), clientIp, method, path, HttpStatus.GATEWAY_TIMEOUT.value(), latency);
            log.error("Upstream timeout for route {} targeting {}: {}", route.routeId(), targetUrl, ex.getMessage());

            return ResponseEntity.status(HttpStatus.GATEWAY_TIMEOUT)
                .contentType(MediaType.APPLICATION_JSON)
                .body(String.format("{\"error\": \"Gateway Timeout\", \"message\": \"Upstream target timed out after %d ms\"}",
                        timeoutMs).getBytes());

        } catch (RestClientResponseException ex) {
            long latency = System.currentTimeMillis() - startTime;
            telemetryProducer.record(route.routeId(), clientIp, method, path, ex.getStatusCode().value(), latency);

            return ResponseEntity.status(ex.getStatusCode())
                .contentType(MediaType.APPLICATION_JSON)
                .body(ex.getResponseBodyAsByteArray());

        } catch (Exception ex) {
            long latency = System.currentTimeMillis() - startTime;
            telemetryProducer.record(route.routeId(), clientIp, method, path, HttpStatus.BAD_GATEWAY.value(), latency);
            log.error("Proxy error for route {}: {}", route.routeId(), ex.getMessage());

            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .contentType(MediaType.APPLICATION_JSON)
                .body(String.format("{\"error\": \"Bad Gateway\", \"message\": \"%s\"}", ex.getMessage()).getBytes());
        }
    }

    private String buildTargetUrl(RouteDefinition route, String requestUri, String queryString) {
        // Strip the base path pattern prefix and map to target base URL + target prefix
        String cleanPatternPrefix = route.pathPattern().replace("/**", "").replace("/*", "");
        String remainingPath = requestUri.startsWith(cleanPatternPrefix)
            ? requestUri.substring(cleanPatternPrefix.length())
            : requestUri;

        if (!remainingPath.startsWith("/") && !remainingPath.isEmpty()) {
            remainingPath = "/" + remainingPath;
        }

        String prefix = route.targetPathPrefix() != null ? route.targetPathPrefix() : "";
        if (prefix.endsWith("/") && remainingPath.startsWith("/")) {
            prefix = prefix.substring(0, prefix.length() - 1);
        }

        StringBuilder urlBuilder = new StringBuilder(route.targetBaseUrl())
            .append(prefix)
            .append(remainingPath);

        if (queryString != null && !queryString.isBlank()) {
            urlBuilder.append("?").append(queryString);
        }

        return urlBuilder.toString();
    }

    private void copyHeaders(HttpServletRequest source, HttpHeaders target) {
        Enumeration<String> names = source.getHeaderNames();
        while (names != null && names.hasMoreElements()) {
            String name = names.nextElement();
            if (!HOP_BY_HOP_HEADERS.contains(name.toLowerCase())) {
                target.add(name, source.getHeader(name));
            }
        }
    }

    private String resolveClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr() != null ? request.getRemoteAddr() : "127.0.0.1";
    }
}
