package in.coderarmy.gatekeeper.gateway.service;

import in.coderarmy.gatekeeper.gateway.dto.AccessLogEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.UUID;

@Service
@EnableAsync
@RequiredArgsConstructor
@Slf4j
public class TelemetryProducerService {

    public static final String TELEMETRY_TOPIC = "gateway-access-logs";

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Async
    public void record(String routeId, String clientIp, String method, String uri, int statusCode, long latencyMs) {
        AccessLogEvent event = new AccessLogEvent(
            UUID.randomUUID().toString(),
            routeId,
            clientIp,
            method,
            uri,
            statusCode,
            latencyMs,
            Instant.now()
        );

        try {
            String jsonPayload = objectMapper.writeValueAsString(event);
            kafkaTemplate.send(TELEMETRY_TOPIC, routeId, jsonPayload).whenComplete((result, ex) -> {
                if (ex != null) {
                    log.warn("Telemetry delivery to Kafka failed: {}", ex.getMessage());
                } else {
                    log.debug("Telemetry event published for route: {}, status: {}", routeId, statusCode);
                }
            });
        } catch (Exception ex) {
            log.warn("Unable to dispatch telemetry event to Kafka: {}", ex.getMessage());
        }
    }
}
