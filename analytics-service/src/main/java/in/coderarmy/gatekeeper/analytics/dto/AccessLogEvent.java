package in.coderarmy.gatekeeper.analytics.dto;

import java.time.Instant;

public record AccessLogEvent(
    String eventId,
    String routeId,
    String clientIp,
    String httpMethod,
    String requestUri,
    int statusCode,
    long latencyMs,
    Instant timestamp
) {}
