package in.coderarmy.gatekeeper.gateway.dto;

import java.util.Set;

public record RouteDefinition(
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
