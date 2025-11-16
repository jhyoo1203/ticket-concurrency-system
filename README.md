# 🎫 티켓 예매 동시성 제어 시스템(우테코 8기 프리코스 오픈미션)

> 저는 2주간의 오픈 미션 중 '고난도 문제 해커톤' 유형을 선택하였습니다.  
> 선착순 티켓 예매 시나리오에서 발생하는 **동시성 문제**를 단계별로 해결하는 과정을 담은 프로젝트입니다.  
> LV.1(문제 정의)부터 LV.4(Kafka 비동기 처리)까지 점진적으로 개선하며 각 기술의 Trade-off를 학습합니다.

<br>

## 📌 프로젝트 개요

**"100장의 티켓에 1000명이 동시에 접속하면 어떻게 될까?"**

이 질문에서 시작한 프로젝트입니다. 단순한 CRUD를 넘어, 대량의 동시 요청이 발생했을 때
- 왜 데이터 정합성이 깨지는지 (Race Condition)
- 어떤 기술로 해결할 수 있는지 (DB Lock, Redis, Kafka)
- 각 기술의 장단점은 무엇인지 (Trade-off)

를 코드로 증명하고 학습하는 것이 목표입니다.

<br>

## 🛠 기술 스택

| 분류 | 기술 |
|------|------|
| Language & Framework | Java 21, Spring Boot 3.5.7 |
| Database | PostgreSQL, Spring Data JPA |
| Distributed Lock | Redis, Redisson |
| Message Queue | Apache Kafka |
| Load Balancer | Nginx |
| Testing | JUnit 5, K6 |
| Container | Docker, Docker Compose |
| Monitoring | Prometheus, Grafana |

<br>

## 🎯 단계별 학습 로드맵

### [LV.1 - Race Condition 문제 정의](./lv1-race-condition/README.md)
**"동시성 문제가 뭔지 직접 확인하기"**

- 🔍 **목표**: 락 없이 구현했을 때 발생하는 Race Condition을 재현
- ⚠️ **결과**: 100개 재고에 1000명 요청 → 재고 음수, 중복 예매 발생
- 💡 **학습**: 동시성 문제의 본질 이해

**주요 내용**: 문제 상황 재현, 테스트 코드, K6 부하 테스트

---

### [LV.2 - DB Lock으로 해결](./lv2-db-lock/README.md)
**"데이터베이스의 힘을 빌려 정합성 보장"**

- 🔒 **기술**: Pessimistic Lock, Optimistic Lock
- ✅ **결과**: Race Condition 해결, 데이터 정합성 보장
- ⚖️ **Trade-off**
  - Pessimistic: DB 부하 증가, 대기 시간 발생
  - Optimistic: 충돌 많으면 재시도 비용 증가

**주요 내용**: 두 가지 락 방식 비교, 성능 테스트, 적용 시나리오

---

### [LV.3 - Redisson 분산 락](./lv3-redisson/README.md)
**"분산 환경에서도 동작하는 락"**

- 🌐 **기술**: Redisson (Redis 기반 분산 락)
- ✅ **장점**: 멀티 인스턴스 환경 지원, DB 부하 분산
- ⚠️ **착각 주의**: "Redis는 빠르다 ≠ Redisson이 더 빠르다"
  - 단일 서버에서는 Pessimistic Lock이 더 빠를 수 있음
  - Redisson의 진가는 **스케일 아웃**할 때 드러남

**주요 내용**: 분산 락 개념, 네트워크 오버헤드, 실무 적용 시 고려사항

---

### [LV.4 - Kafka 비동기 처리](./lv4-kafka/README.md)
**"트래픽 스파이크에도 죽지 않는 시스템"**

- 📨 **기술**: Kafka + Redisson 조합
- ✅ **장점**
  - 즉시 응답 (< 100ms)
  - 트래픽 스파이크 대응
  - 시스템 안정성 확보
- ⚠️ **착각 주의**: "비동기 != 빠르다"
  - API 응답은 빠르지만, 실제 완료는 더 오래 걸림
  - 사용자는 결과를 나중에 받음 (최종 일관성)

**주요 내용**: Producer-Consumer 패턴, 메시지 보장, 8가지 개선 방안 (멱등성 키, DLQ, Circuit Breaker 등)

<br>

## 📊 레벨별 비교

| 레벨 | 기술 | 응답 시간 | 정합성 | 분산 환경 | 시스템 안정성 | 복잡도 |
|------|------|----------|--------|----------|-------------|--------|
| **LV.1** | 락 없음 | 🚀 빠름 | ❌ 깨짐 | ❌ | ❌ | 낮음 |
| **LV.2** | DB Lock | 🐢 느림 | ✅ 보장 | ✅ | ⚠️ DB 부하 | 낮음 |
| **LV.3** | Redisson | 🐢 보통 | ✅ 보장 | ✅ | ✅ 좋음 | 중간 |
| **LV.4** | Kafka + Redisson | 🚀 API만 빠름 | ✅ 보장 | ✅ | ✅ 최고 | 높음 |

> **💡 핵심 인사이트**  
> 각 기술은 "더 좋은 것"이 아니라 "상황에 맞는 것"입니다.  
> - 소규모 서비스: LV.2 (DB Lock)로 충분
> - 분산 환경: LV.3 (Redisson)
> - 대규모 트래픽: LV.4 (Kafka)

<br>

## 💡 주요 학습 내용

### 1. 동시성 문제의 본질
- **Race Condition**: 여러 스레드가 동시에 같은 자원에 접근할 때 발생
- **Lost Update**: 한 스레드의 업데이트가 다른 스레드에 의해 덮어씌워짐
- **해결의 핵심**: 임계 영역(Critical Section)을 보호하는 것

### 2. 락의 종류와 특성
- **Pessimistic Lock**: 비관적으로 미리 잠금 → DB 부하
- **Optimistic Lock**: 낙관적으로 충돌 시 재시도 → 충돌 많으면 비효율
- **Distributed Lock**: 분산 환경에서 동작 → 네트워크 비용

### 3. 동기 vs 비동기
- **동기(LV.1-3)**: 즉시 결과 확인 가능, 트래픽에 취약
- **비동기(LV.4)**: 시스템 안정적, 사용자는 대기 필요

### 4. Trade-off 사고
- 정합성 ↔ 성능
- 단순함 ↔ 확장성
- 즉시 일관성 ↔ 최종 일관성
- **정답은 없고, 상황에 맞는 선택만 있다**

<br>

## 🔗 참고 자료

### 공식 문서
- [Spring Data JPA - Locking](https://docs.spring.io/spring-data/jpa/reference/jpa/locking.html)
- [Redisson Documentation](https://redisson.org/)
- [Apache Kafka Documentation](https://kafka.apache.org/documentation/)

### 블로그 & 아티클
- [재고 시스템으로 알아보는 동시성 제어](https://techblog.woowahan.com/2631/)
- [분산 락 구현하기](https://hyperconnect.github.io/2019/11/15/redis-distributed-lock-1.html)

<br>

## 📝 마무리

이 프로젝트는 **"문제를 정의하고, 해결하고, 한계를 인정하는"** 과정을 담았습니다.

- LV.1: 문제가 무엇인지 명확히 정의
- LV.2: 가장 기본적인 해결책 적용
- LV.3: 분산 환경으로 확장
- LV.4: 대규모 트래픽 대응

각 레벨마다 명확한 **Trade-off**가 있으며, "더 좋은 기술"은 없습니다.  
**상황에 맞는 기술**을 선택하는 것이 아키텍처의 핵심입니다.

> **"적절한 기술을 적절한 곳에"** - 이것이 이 프로젝트의 핵심 메시지입니다.
