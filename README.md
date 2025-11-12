# 🚀 우아한테크코스 8기 오픈 미션: 대용량 트래픽 티켓 예매 시스템 (점진적 개선 로드맵 LV.1 ~ LV.4)

> 2주간의 오픈 미션 중 '고난도 문제 해커톤' 유형을 선택하여, '선착순 티켓 예매' 시나리오에서 발생하는 동시성 문제를 정의하고, 이를 LV.1(단순 API)부터 LV.4(메시지 큐)까지 점진적으로 개선하는 과정을 구현했습니다.

<br>

## 1. 미션 소개

### 🎯 주제 선정 이유 (The Why)
'선착순', '한정 재고' 키워드는 대용량 트래픽 환경에서 백엔드 개발자가 반드시 마주하는 도전적인 과제입니다.

단순히 기능을 완성하는 것을 넘어, 대량의 동시 요청이 발생했을 때 왜 데이터 정합성이 깨지는지(Race Condition)를 코드로 증명하고 싶었습니다.

그리고 이 문제를 해결하기 위해 현업에서 사용되는 다양한 기술(DB Lock, Redis, Message Queue)을 단계별로 적용해 보며, 각 기술이 가지는 명확한 장점과 한계(Trade-off)를 직접 경험하고 학습하는 것을 목표로 삼았습니다.

<br>

## 2. 기술 스택

- **Language**: `Java 21`
- **Framework**: `Spring Boot 3.5.7`
- **ORM**: `Spring Data JPA`
- **Database**: `PostgreSQL`
- **In-Memory**: `Redis`
- **Distributed Lock**: `Redisson`
- **Message Queue**: `Kafka`
- **Test**: `JUnit 5`, `K6`
- **Container**: `Docker`, `Docker Compose`

<br>

