package in.coderarmy.gatekeeper.analytics.consumer;

import in.coderarmy.gatekeeper.analytics.document.AccessEventDocument;
import in.coderarmy.gatekeeper.analytics.dto.AccessLogEvent;
import in.coderarmy.gatekeeper.analytics.repository.AccessEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
@RequiredArgsConstructor
@Slf4j
public class AccessLogConsumer {

    private final AccessEventRepository eventRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @KafkaListener(topics = "gateway-access-logs", groupId = "analytics-group")
    public void consumeAccessLog(String message) {
        try {
            AccessLogEvent event = objectMapper.readValue(message, AccessLogEvent.class);
            log.info("Received telemetry event for route {}: status={}, latency={}ms",
                    event.routeId(), event.statusCode(), event.latencyMs());

            AccessEventDocument document = AccessEventDocument.builder()
                .id(event.eventId())
                .routeId(event.routeId())
                .clientIp(event.clientIp())
                .httpMethod(event.httpMethod())
                .requestUri(event.requestUri())
                .statusCode(event.statusCode())
                .latencyMs(event.latencyMs())
                .timestamp(event.timestamp())
                .build();

            eventRepository.save(document);
        } catch (Exception ex) {
            log.error("Failed to process access event from Kafka: {}", ex.getMessage());
        }
    }
}
