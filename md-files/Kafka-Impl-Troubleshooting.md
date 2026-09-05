# Kafka-Impl-Troubleshooting (2026-09-06)

## Summary

Post-implementation bring-up of the Kafka pipeline. Five distinct issues discovered and fixed before consumer groups registered successfully.

---

## Issues Found and Fixed

### 1. `KafkaTemplate<String, OrderEvent>` Bean Not Found

**Symptom:** `CartService` and `OrderService` both failed to start.

**Error:**
```
No qualifying bean of type 'KafkaTemplate<String, OrderEvent>' available
```

**Root cause:** Spring Framework 7 enforces strict generic type matching for `@Autowired` injection. The Spring Boot auto-configured bean is `KafkaTemplate<Object, Object>`, which does **not** satisfy a `KafkaTemplate<String, OrderEvent>` injection point — even though it did in Spring Boot 3.

**Fix:** Declared typed beans explicitly in `order/config/KafkaConfig.kt`:

```kotlin
@Bean
fun orderEventProducerFactory(): ProducerFactory<String, OrderEvent> =
    DefaultKafkaProducerFactory(kafkaProperties.buildProducerProperties())

@Bean
fun orderEventKafkaTemplate(pf: ProducerFactory<String, OrderEvent>): KafkaTemplate<String, OrderEvent> =
    KafkaTemplate(pf)
```

---

### 2. spring-kafka 4.x Breaking Changes

**Symptom:** Services failed to start with `ClassNotFoundException` for serializer/deserializer classes.

**Root cause:** spring-kafka 4.x (Jackson 3 transition) renamed two classes and moved `KafkaProperties` to a different package.

| What changed | Old (spring-kafka 3.x) | New (spring-kafka 4.x) |
|---|---|---|
| JSON serializer | `JsonSerializer` | `JacksonJsonSerializer` |
| JSON deserializer | `JsonDeserializer` | `JacksonJsonDeserializer` |
| `KafkaProperties` package | `org.springframework.boot.autoconfigure.kafka` | `org.springframework.boot.kafka.autoconfigure` |

**Fix:** Updated all three `application.yml` files and the `KafkaConfig.kt` import:

```yaml
# order-service application.yml
value-serializer: org.springframework.kafka.support.serializer.JacksonJsonSerializer

# store-service / notification-service application.yml
value-deserializer: org.springframework.kafka.support.serializer.JacksonJsonDeserializer
```

```kotlin
// KafkaConfig.kt
import org.springframework.boot.kafka.autoconfigure.KafkaProperties
```

---

### 3. Bitnami Kafka Helm Chart — Image Not on Docker Hub

**Symptom:** `baemin-kafka-controller-0` pod stuck in `Init:ImagePullBackOff`.

**Error:**
```
manifest for docker.io/bitnami/kafka:4.0.0-debian-12-r10 not found
```

**Root cause:** Bitnami stopped publishing images to Docker Hub after November 2023. All new tags are only available at `oci.bitnami.com`, which requires OCI registry support. The cluster's internal DNS could not resolve `oci.bitnami.com`.

**Attempts that failed:**
1. Set `global.imageRegistry: oci.bitnami.com` → DNS resolution failure inside cluster
2. Downgraded to Bitnami chart 26.x (Kafka 3.6.1) → `bitnami/kafka:3.6.1-debian-12-r12` also not on Docker Hub (post-migration tag)

**Fix:** Replaced the Bitnami subchart wrapper entirely with a standalone Helm chart using the official `apache/kafka:3.7.1` image from Docker Hub (KRaft mode).

```
helm/kafka/
  Chart.yaml          ← no dependencies; appVersion: "3.7.1"
  values.yaml         ← image: apache/kafka:3.7.1
  templates/
    statefulset.yaml  ← KRaft env vars; service name baemin-kafka
    service.yaml      ← ClusterIP on port 9092
```

Files changed: `helm/kafka/Chart.yaml`, `helm/kafka/values.yaml`, `helm/kafka/templates/statefulset.yaml` (new), `helm/kafka/templates/service.yaml` (new).

---

### 4. Orphaned Kubernetes Resources from Failed Helm Installs

**Symptom:** After switching to the standalone chart, `helm install` failed with:
```
Service "baemin-kafka" already exists
```

`kubectl get all -n baemin` revealed orphaned resources from the multiple failed Bitnami installs:
- `StatefulSet/baemin-kafka-controller` (3 replicas, from Bitnami chart)
- `StatefulSet/baemin-zookeeper`
- `Service/baemin-kafka` (leftover)
- Provisioning `Job` and `Pod`
- Multiple `PVC`s

**Root cause:** Helm tracks releases in a Secret, but orphaned Kubernetes resources from failed installs remain unless explicitly deleted.

**Fix:** Manually cleaned up all orphaned resources, then used `helm upgrade` (not `install`) to reconcile the failed release state:

```bash
helm uninstall baemin -n baemin
kubectl delete statefulset baemin-kafka-controller baemin-zookeeper -n baemin
kubectl delete service baemin-kafka -n baemin
kubectl delete pvc --all -n baemin
kubectl delete job --all -n baemin

# Re-install under the correct release name that owns the kafka resources
helm upgrade baemin helm/kafka -n baemin
```

**Result:** `baemin-kafka-0` — 1/1 Running. Topic `baemin.order.events` auto-created by `KafkaAdmin`.

---

### 5. Consumer Groups Not Registering — Missing `spring-boot-starter-kafka`

**Symptom:** `kafka-consumer-groups.sh --list` returned empty. `baemin-kafka-0` was running and the topic existed, but no consumer groups appeared.

**Diagnosis:** `store-service` logs showed a completely clean startup with **zero Kafka-related lines** — not even a broker connection attempt. `KAFKA_BOOTSTRAP_SERVERS=baemin-kafka:9092` was correctly injected via the Helm deployment.

**Root cause:** In Spring Boot 4, `KafkaAutoConfiguration` was moved into a separate module. `spring-kafka` alone **does not activate auto-configuration**. Without `spring-boot-starter-kafka`, `KafkaAutoConfiguration` never runs and no `ConsumerFactory` or `KafkaListenerContainerFactory` beans are created — so `@KafkaListener` methods are silently ignored.

Both `store-service` and `notification-service` only had:
```kotlin
implementation("org.springframework.kafka:spring-kafka")  // not enough
```

**Fix:** Added the starter to both `build.gradle.kts` files:
```kotlin
implementation("org.springframework.boot:spring-boot-starter-kafka")
implementation("org.springframework.kafka:spring-kafka")
```

Note: `order-service` (producer) also requires the starter for `KafkaAutoConfiguration` to create the `KafkaAdmin` bean that registers the topic.

---

## Final Verification Commands

```bash
# Confirm broker is up and topic exists
kubectl exec -n baemin baemin-kafka-0 -- \
  /opt/kafka/bin/kafka-topics.sh --bootstrap-server localhost:9092 --list

# Confirm consumer groups registered (after store/notification redeploy)
kubectl exec -n baemin baemin-kafka-0 -- \
  /opt/kafka/bin/kafka-consumer-groups.sh --bootstrap-server localhost:9092 --list
# Expected: notification-service, store-service

# Inspect a consumer group's lag
kubectl exec -n baemin baemin-kafka-0 -- \
  /opt/kafka/bin/kafka-consumer-groups.sh \
  --bootstrap-server localhost:9092 \
  --describe --group notification-service
```

---

## Key Lessons

| # | Lesson |
|---|---|
| 1 | Spring Framework 7 strict generic matching: always declare typed `ProducerFactory<K,V>` + `KafkaTemplate<K,V>` beans explicitly |
| 2 | spring-kafka 4.x: use `JacksonJsonSerializer` / `JacksonJsonDeserializer`; `KafkaProperties` is now `org.springframework.boot.kafka.autoconfigure` |
| 3 | Bitnami images are no longer on Docker Hub (post-Nov 2023); use `apache/kafka` from Docker Hub instead |
| 4 | After failed `helm install`, use `helm upgrade` or manually delete orphaned resources before retrying |
| 5 | In Spring Boot 4, `spring-kafka` alone is not enough for consumers — `spring-boot-starter-kafka` is required to activate `KafkaAutoConfiguration` |
