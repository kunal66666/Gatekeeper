package in.coderarmy.gatekeeper.management.dto.responseDto;

import java.util.Set;

public record RouteSummaryResponse(
    String routeId,
    String name,
    String pathPattern,
    String targetBaseUrl,
    String targetPathPrefix,
    Integer requestsPerMinute,
    Integer responseTimeoutMs,
    boolean idempotencyEnabled,
    boolean active,
    Set<String> allowedMethods
) {}
