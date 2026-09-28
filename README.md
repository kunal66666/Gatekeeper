# Gatekeeper - Distributed API Gateway & Management Platform

A production-grade, microservices-based API Gateway and Management Platform built with **Spring Boot 4 / Java 21**, **Apache Kafka**, **Redis**, **OAuth2 Security**, and **Flyway**.

---

## 🏛 System Architecture

The platform consists of three core microservices:

| Service | Port | Description | Tech Stack |
| :--- | :---: | :--- | :--- |
| **`smart-gateway-service`** | `8080` | High-throughput edge API gateway handling request routing, distributed rate-limiting, and OAuth2 security. | Spring Boot, Redis, Kafka, OAuth2, AOP |
| **`api-management-service`** | `8081` | Centralized control plane managing users, API routes, roles, and schema migrations. | Spring Boot, MySQL, Flyway, H2 (Test), Jenkins CI/CD |
| **`analytics-service`** | `8082` | Asynchronous metric ingestion and traffic auditing consuming telemetry events from Kafka. | Spring Boot, Kafka, AOP |

---

## 🚀 Key Features

- **Distributed Rate Limiting**: Redis-backed token bucket algorithm to protect downstream services from spikes.
- **Asynchronous Telemetry Pipeline**: Non-blocking Kafka producer streaming request logs to `analytics-service`.
- **Database Migrations**: Automated version-controlled schema evolution using **Flyway**.
- **Automated CI/CD**: Declarative `Jenkinsfile` automating compile, test, package, deployment, and smoke tests.

---

## 🛠 Prerequisites

- Java 21+ (e.g. Amazon Corretto 23 / OpenJDK 21)
- Maven 3.9+
- MySQL (for `api-management-service`)
- Redis & Apache Kafka (for gateway rate-limiting and analytics)

---

## 🏃 Running the Services

```bash
# 1. Start Smart Gateway Service (Port 8080)
cd smart-gateway-service
./mvnw spring-boot:run

# 2. Start API Management Service (Port 8081)
cd ../api-management-service
./mvnw spring-boot:run

# 3. Start Analytics Service (Port 8082)
cd ../analytics-service
./mvnw spring-boot:run
```
