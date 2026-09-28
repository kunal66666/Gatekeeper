INSERT INTO organizations (name, slug, status, created_at, updated_at)
VALUES ('Default Organization', 'default-org', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO api_routes (
    organization_id, route_id, name, path_pattern, target_base_url, target_path_prefix,
    requests_per_minute, response_timeout_ms, idempotency_enabled, active, configuration_version,
    created_at, updated_at
)
SELECT id, 'route-users', 'JSONPlaceholder Users', '/api/users/**', 'https://jsonplaceholder.typicode.com', '/users',
       30, 5000, FALSE, TRUE, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM organizations WHERE slug = 'default-org';

INSERT INTO route_allowed_methods (route_pk, http_method)
SELECT id, 'GET' FROM api_routes WHERE route_id = 'route-users';
INSERT INTO route_allowed_methods (route_pk, http_method)
SELECT id, 'POST' FROM api_routes WHERE route_id = 'route-users';

INSERT INTO api_routes (
    organization_id, route_id, name, path_pattern, target_base_url, target_path_prefix,
    requests_per_minute, response_timeout_ms, idempotency_enabled, active, configuration_version,
    created_at, updated_at
)
SELECT id, 'route-posts', 'JSONPlaceholder Posts', '/api/posts/**', 'https://jsonplaceholder.typicode.com', '/posts',
       60, 5000, FALSE, TRUE, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM organizations WHERE slug = 'default-org';

INSERT INTO route_allowed_methods (route_pk, http_method)
SELECT id, 'GET' FROM api_routes WHERE route_id = 'route-posts';
INSERT INTO route_allowed_methods (route_pk, http_method)
SELECT id, 'POST' FROM api_routes WHERE route_id = 'route-posts';

INSERT INTO api_routes (
    organization_id, route_id, name, path_pattern, target_base_url, target_path_prefix,
    requests_per_minute, response_timeout_ms, idempotency_enabled, active, configuration_version,
    created_at, updated_at
)
SELECT id, 'route-test-limit', 'Rate Limit Test Route', '/api/test-limit/**', 'https://httpbin.org', '/get',
       5, 5000, FALSE, TRUE, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM organizations WHERE slug = 'default-org';

INSERT INTO route_allowed_methods (route_pk, http_method)
SELECT id, 'GET' FROM api_routes WHERE route_id = 'route-test-limit';
