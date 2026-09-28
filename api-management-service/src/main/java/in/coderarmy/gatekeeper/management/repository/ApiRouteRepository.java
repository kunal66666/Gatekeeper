package in.coderarmy.gatekeeper.management.repository;

import in.coderarmy.gatekeeper.management.entity.ApiRoute;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ApiRouteRepository extends JpaRepository<ApiRoute, Long> {
    List<ApiRoute> findByActiveTrue();
    Optional<ApiRoute> findByRouteId(String routeId);
    boolean existsByRouteId(String routeId);
}
