package in.coderarmy.gatekeeper.analytics.controller;

import in.coderarmy.gatekeeper.analytics.document.AccessEventDocument;
import in.coderarmy.gatekeeper.analytics.repository.AccessEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AccessEventRepository eventRepository;

    @GetMapping("/events")
    public ResponseEntity<List<AccessEventDocument>> getRecentEvents() {
        return ResponseEntity.ok(eventRepository.findTop50ByOrderByTimestampDesc());
    }

    @GetMapping("/routes/{routeId}")
    public ResponseEntity<Map<String, Object>> getRouteStats(@PathVariable String routeId) {
        List<AccessEventDocument> events = eventRepository.findByRouteIdOrderByTimestampDesc(routeId);

        long totalRequests = events.size();
        double avgLatency = events.stream()
            .mapToLong(AccessEventDocument::getLatencyMs)
            .average()
            .orElse(0.0);

        long rateLimitedCount = events.stream()
            .filter(e -> e.getStatusCode() == 429)
            .count();

        long successCount = events.stream()
            .filter(e -> e.getStatusCode() >= 200 && e.getStatusCode() < 400)
            .count();

        Map<String, Object> stats = new HashMap<>();
        stats.put("routeId", routeId);
        stats.put("totalRequests", totalRequests);
        stats.put("averageLatencyMs", Math.round(avgLatency * 100.0) / 100.0);
        stats.put("successRequests", successCount);
        stats.put("rateLimitedRequests", rateLimitedCount);
        stats.put("recentEvents", events.stream().limit(10).toList());

        return ResponseEntity.ok(stats);
    }
}
