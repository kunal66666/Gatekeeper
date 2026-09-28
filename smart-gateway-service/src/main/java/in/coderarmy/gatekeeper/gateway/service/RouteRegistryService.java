package in.coderarmy.gatekeeper.gateway.service;

import in.coderarmy.gatekeeper.gateway.dto.RouteDefinition;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
@EnableScheduling
public class RouteRegistryService {

    private final AntPathMatcher pathMatcher = new AntPathMatcher();
    private final List<RouteDefinition> routes = new CopyOnWriteArrayList<>();
    private final RestClient restClient;

    public RouteRegistryService(@Value("${management.service.url:http://localhost:8081}") String managementUrl) {
        this.restClient = RestClient.builder()
            .baseUrl(managementUrl)
            .build();
    }

    @PostConstruct
    public void init() {
        // Initialize with default fallback routes
        routes.addAll(List.of(
            new RouteDefinition(
                "route-users", "JSONPlaceholder Users", "/api/users/**",
                "https://jsonplaceholder.typicode.com", "/users",
                30, 5000, false, true, Set.of("GET", "POST")
            ),
            new RouteDefinition(
                "route-posts", "JSONPlaceholder Posts", "/api/posts/**",
                "https://jsonplaceholder.typicode.com", "/posts",
                60, 5000, false, true, Set.of("GET", "POST")
            ),
            new RouteDefinition(
                "route-test-limit", "Rate Limit Test Route", "/api/test-limit/**",
                "https://httpbin.org", "/get",
                5, 5000, false, true, Set.of("GET")
            )
        ));

        // Attempt initial sync from management service
        refreshRoutes();
    }

    @Scheduled(fixedDelay = 30000)
    public void refreshRoutes() {
        try {
            List<RouteDefinition> fetched = restClient.get()
                .uri("/internal/routes")
                .retrieve()
                .body(new ParameterizedTypeReference<List<RouteDefinition>>() {});

            if (fetched != null && !fetched.isEmpty()) {
                routes.clear();
                routes.addAll(fetched.stream().filter(RouteDefinition::active).toList());
            }
        } catch (Exception ignored) {
            // Retain existing cached routes if management service is unavailable
        }
    }

    public Optional<RouteDefinition> matchRoute(String path, String method) {
        return routes.stream()
            .filter(r -> r.active() && r.allowedMethods().contains(method.toUpperCase()))
            .filter(r -> pathMatcher.match(r.pathPattern(), path))
            .findFirst();
    }

    public List<RouteDefinition> getAllRoutes() {
        return List.copyOf(routes);
    }
}
