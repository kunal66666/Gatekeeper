package in.coderarmy.gatekeeper.analytics.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "access_events")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccessEventDocument {

    @Id
    private String id;

    @Indexed
    private String routeId;

    private String clientIp;
    private String httpMethod;
    private String requestUri;
    private int statusCode;
    private long latencyMs;

    @Indexed
    private Instant timestamp;
}
