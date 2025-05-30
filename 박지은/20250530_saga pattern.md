기존의 Monolithic 환경에서는 DBMS가 기본적으로 제공해주는 트랜잭선 기능을 통해서 데이터 commit이나 rollback을 통해서 일관성있게 관리할 수 있다.

하지만 Applcation 과 DB가 분산되면서 해당 트랜잭션 처리를 단일 DBMS에서 제공하는 기능으로는 해결할 수 없게 된다.

## Two-Phase Commit (2PC)

Two-Phase Commit (2PC)은 분산 시스템에서 여러 노드(또는 데이터베이스)가 참여하는 트랜잭션의 원자성(atomicity)을 보장하기 위해 사용하는 분산 트랜잭션 프로토콜

- 여러 시스템이 하나의 트랜잭션을 함께 처리할 때, 모두 성공하거나 모두 실패하도록 만드는 방법
* 목적: 모두 성공 또는 모두 실패 보장
* 단점: Coordinator 장애 시 블로킹 위험
- 동작 원리: 2단계로 나누어 진행 
  - 1단계: 준비 (Prepare / Voting Phase): 모든 참여자에게 커밋 가능한지 확인
    * Coordinator (조정자)가 각 참여자(participant)에게 "이 트랜잭션 커밋할 준비 됐어?"라고 물어본다.
    * 각 참여자는 다음 중 하나를 응답한다:
      * **Yes (Prepared / Vote to Commit)** → 커밋 준비 완료
      * **No (Vote to Abort)** → 문제 발생, 롤백 요청
      > 이 단계에서는 실제로 데이터에 반영되지 않고, 준비만 한다. (예: 로그 저장, 잠금 설정)
  - 2단계: 커밋 또는 롤백 (Commit / Abort Phase): 모든 참여자가 OK하면 커밋, 아니면 롤백
    * **모든 참여자**가 Yes를 보냈다면 → Coordinator가 "**커밋**" 명령을 내림
    * **하나라도 No**를 보냈다면 → Coordinator가 "**롤백**" 명령을 내림
    * 모든 참여자는 Coordinator의 명령에 따라 실제 커밋 or 롤백 수행.
    > #### 📌 예시
    > 
    > - 상황: 은행 송금 (계좌 A → 계좌 B)
    >   * A 서버: 잔액 차감
    >   * B 서버: 잔액 증가
    > 
    > - 1단계: 준비
    >   * Coordinator → A: 준비 됐어? → A: OK
    >   * Coordinator → B: 준비 됐어? → B: OK
    > 
    > - 2단계: 커밋
    >   * 모두 OK → Coordinator → A, B: Commit!



# SAGA 패턴

SAGA 패턴: 하나의 큰 트랜잭션을 여러 개의 작은 로컬 트랜잭션(Local Transaction)으로 나누고,
각 단계가 성공하면 다음 단계로 넘어가며(마이크로서비스들끼리 이벤트를 통신), 실패 시 보상(complemetary) 트랜잭션을 실행해서 이전 작업들을 취소하여 분산 환경에서 원자성(atomicity)을 보장하는 패턴

> 보상 트랜잭션이란?
>
> - 실제 DB 트랜잭션을 롤백하는 게 아니라, "그에 상응하는 반대 작업"을 새로 실행해서 취소하는 것
> - ex) 주문을 DB에서 삭제하거나 상태를 CANCELED로 바꾸기

- 성공
  
  ![success](20250530_saga pattern/success.png)
- 실패
  
  ![fail](20250530_saga pattern/fail.png)
    - 실패 이벤트를 처리해주어야 한다.
- 각 서비스의 로컬 트랜잭션을 순차적으로 처리한다.
- SAGA 패턴의 핵심은 트랜잭션의 관리주체가 DBMS에 있는 것이 아닌 Application에 있다.
  - 각 Applicatin은 하위에 존재하는 DB는 local 트랜잭션만 담당
    > - local 트랜잭션: 하나의 DB에만 영향을 미치는 트랜잭션. 단일 서비스 내에서 처리<br>
    > - global 트랜잭션: 여러 DB, 여러 서비스에 걸쳐 수행되는 트랜잭션. 분산 트랜잭션이라고도 함
    >   - 전체 작업을 하나의 트랜잭션처럼 처리하려면 2PC나 SAGA 같은 방식이 필요
    > > 분산 시스템에서는 여러 개의 서비스(Application)가 각각의 DB를 따로 관리하는데, 각 서비스는 자신의 DB만 알고 있고, 자신의 DB에만 트랜잭션을 실행한다는 것을 "local 트랜잭션만 담당한다"고 표현 
  - ➡️ 각각의 Application의 트랜잭션 요청의 실패로 인한 Rollback 처리(보상 트랜잭션)은 Application에서 구현
    > SAGA는 2PC(Two-Phase Commit)처럼 DB에서 직접 롤백을 하지 않고, 어떤 서비스에서 트랜잭션이 실패했을 때, 앞에서 성공한 작업을 '되돌리는 작업'을 Application이 직접 구현한 API 또는 로직을 호출해야 한다.
  - 이러한 과정을 통해서 순차적으로 트랜잭션이 처리되며, 마지막 트랜잭션이 끝났을 때 데이터가 완전히 영속되었음을 확인하고 종료한다. 
    - 이 방법을 통해서 최종 일관성(Eventually Consistency)를 달성할 수 있다.

## SAGA 패턴의 종류
### Choreography based SAGA pattern
Choreography-based Saga 패턴: 각 서비스가 이벤트를 구독하고 처리하며, 다음 서비스에게 이벤트를 발행함으로써 트랜잭션 흐름이 이루어지는 방식

- 보유한 서비스 내의 Local 트랜잭션을 관리하며 트랜잭션이 종료하게 되면 완료 Event를 발행
  - 다음 수행해야할 트랜잭션이 있으면 해당 트랜잭션을 수행해야하는 App으로 이벤트를 보내고, 해당 App은 완료 Event를 수신받아 다음 작업을 진행. 이를 순차적으로 수행
  - 특정 서비스(예: Inventory)가 실패했을 때, 해당 서비스가 실패 사실을 알리는 이벤트(예: InventoryFailed)를 발행하고, 이를 통해 앞 단계 서비스들이 자신의 작업을 취소(보상 트랜잭션)하도록 유도
  
    > ex: Inventory Service에서 재고 부족 등의 이유로 실패한 상황
    > 
    > 실패한 주체인 Inventory Service가 InventoryFailed 같은 이벤트를 발행해서, 이전 단계의 서비스들이 자신이 한 작업을 되돌리도록 유도함.
    > - Order Service: 주문 생성 → OrderCreated 이벤트 발행
    > - Payment Service: 결제 성공 → PaymentCompleted 이벤트 발행
    > - Inventory Service: 재고 차감 시도
    > - Inventory에서 재고 부족 → InventoryFailed 보상 이벤트 발행
    > - Payment Service는 InventoryFailed를 듣고 → CancelPayment 보상 트랜잭션 실행
    > - Order Service는 CancelPayment 이벤트를 듣고 → 주문 취소 처리 보상 트랜잭션 실행
- 각 App별로 트랜잭션을 관리하는 로직이 있다. 
- 중앙 집중식 조정자 없이 트랜잭션 흐름이 이벤트만으로 자동으로 이어진다.
- Event는 Kafka와 같은 메시지 큐를 통해서 비동기 방식으로 전달할 수 있다.
- 장점
  - 서비스 간 결합도가 낮음
  - 확장성과 유지보수성 좋음
  - 이벤트 기반 아키텍처와 잘 맞음
- 단점
  - 운영자 입장에서 트랜잭션의 현재 상태를 확인하기 어려움
    - 트랜잭션 흐름이 명시적으로 보이지 않음 (디버깅 어려움)
  - 이벤트 폭발 가능성
  - 모든 보상 로직을 직접 구현해야 함


### Orchestration based SAGA pattern

Orchestration-Based Saga 패턴: 중앙에서 오케스트레이터(orchestrator)가 전체 트랜잭션 흐름을 관리하고,
각 서비스에게 작업을 명령하는 방식

- 트랜잭션 처리를 위한 Saga 인스턴스(Manager)가 별도로 존재
  - 모든 관리를 Manager가 호출하기 때문에 분산트랜잭션의 중앙 집중화가 이루어진다.
    - 트랜잭션에 관여하는 모든 App이 Manager에 의해 점진적으로 트랜잭션을 수행되며 결과를 Manager에게 전달
    - 비지니스 로직상 마지막 트랜잭션이 끝나면 Manager를 종료해서 전체 트랜잭션 처리를 종료
    - 실패 시에는 오케스트레이터가 직접 보상 트랜잭션도 순차적으로 지시하여 일관성 유지
  > 예시: 주문 처리 플로우
  > 
  >  - Orchestrator가 전체 흐름을 관리
  > - 트랜잭션 단계:
  >   1. Order Service에 주문 생성 요청
  >   2. Payment Service에 결제 요청
  >   3. Inventory Service에 재고 차감 요청
  >   
  > - Inventory가 실패하면?
  >   - Orchestrator가 CancelInventory, CancelPayment, CancelOrder 등 보상 트랜잭션 순서대로 호출
- 주요 개념
    - Orchestrator: 트랜잭션 전체 흐름을 조율하는 중심 서비스
    - 각 서비스: 자신의 로컬 트랜잭션만 수행하고, 결과를 오케스트레이터에 보고
    - 보상 트랜잭션: 실패 시, Orchestrator가 "되돌려라"라고 지시

- 장점
  - 서비스간의 복잡성이 줄어들어서 구현 및 테스트가 쉬워진다.
  - 트랜잭션의 현재 상태를 Manager가 알고 있으므로 롤백을 하기 쉽다.
- 단점
  - 관리를 해야하는 Orchestrator 서비스가 추가되어야하기 때문에 인프라 구현이 복잡하다.


### Choreography vs Orchestration
| 항목         | Choreography                     | Orchestration                              |
| ---------- | -------------------------------- | ------------------------------------------ |
| 트랜잭션 흐름 관리 | 각 서비스가 이벤트를 듣고 다음 이벤트를 발행        | 중앙의 오케스트레이터(조정자)가 흐름 제어                    |
| 중앙 컨트롤러    | 없음                               | 있음                                         |
| 서비스 간 결합도  | 낮음 (서비스들이 이벤트 기반으로 연결됨)          | 상대적으로 높음 (조정자가 각 서비스 호출)                   |
| 예시         | Kafka, RabbitMQ 등 이벤트 메시징 시스템 기반 | 하나의 orchestrator 클래스나 서비스가 명시적으로 다른 서비스 호출 |
