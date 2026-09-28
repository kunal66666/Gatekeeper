package in.coderarmy.gatekeeper.management.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "api_routes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApiRoute {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @Column(name = "route_id", nullable = false, length = 100)
    private String routeId;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(name = "path_pattern", nullable = false, length = 500)
    private String pathPattern;

    @Column(name = "target_base_url", nullable = false, length = 1000)
    private String targetBaseUrl;

    @Column(name = "target_path_prefix", nullable = false, length = 500)
    private String targetPathPrefix;

    @Column(name = "requests_per_minute")
    private Integer requestsPerMinute;

    @Column(name = "response_timeout_ms", nullable = false)
    private Integer responseTimeoutMs;

    @Column(name = "idempotency_enabled", nullable = false)
    private boolean idempotencyEnabled;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "configuration_version", nullable = false)
    private Long configurationVersion;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "route_allowed_methods", joinColumns = @JoinColumn(name = "route_pk"))
    @Column(name = "http_method", nullable = false, length = 10)
    @Builder.Default
    private Set<String> allowedMethods = new HashSet<>();

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.configurationVersion == null) {
            this.configurationVersion = 1L;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
        if (this.configurationVersion != null) {
            this.configurationVersion++;
        }
    }
}
