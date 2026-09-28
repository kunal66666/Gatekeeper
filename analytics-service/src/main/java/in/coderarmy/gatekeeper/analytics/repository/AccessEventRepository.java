package in.coderarmy.gatekeeper.analytics.repository;

import in.coderarmy.gatekeeper.analytics.document.AccessEventDocument;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AccessEventRepository extends MongoRepository<AccessEventDocument, String> {
    List<AccessEventDocument> findTop50ByOrderByTimestampDesc();
    List<AccessEventDocument> findByRouteIdOrderByTimestampDesc(String routeId);
    long countByRouteId(String routeId);
}
