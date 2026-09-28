package in.coderarmy.gatekeeper.management.controller;

import in.coderarmy.gatekeeper.management.dto.responseDto.RouteSummaryResponse;
import in.coderarmy.gatekeeper.management.repository.ApiRouteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/internal/routes")
@RequiredArgsConstructor
public class InternalRouteController {

    private final ApiRouteRepository routeRepository;

    @GetMapping
    public ResponseEntity<List<RouteSummaryResponse>> getActiveRoutes() {
        List<RouteSummaryResponse> routes = routeRepository.findByActiveTrue().stream()
            .map(r -> new RouteSummaryResponse(
                r.getRouteId(),
                r.getName(),
                r.getPathPattern(),
                r.getTargetBaseUrl(),
                r.getTargetPathPrefix(),
                r.getRequestsPerMinute(),
                r.getResponseTimeoutMs(),
                r.isIdempotencyEnabled(),
                r.isActive(),
                r.getAllowedMethods()
            ))
            .toList();

        return ResponseEntity.ok(routes);
    }
}
