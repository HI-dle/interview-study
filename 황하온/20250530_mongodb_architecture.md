# MongoDB Architecture

# Replica Set
![image](https://github.com/user-attachments/assets/09b23c9c-2b77-4023-b7f2-8d6e26f921bf)

-   Replica set은 HA 솔루션이다.
    -   <-> Sharding 은 스케일 아웃을 위한 솔루션이다.
-   데이터를 들고 있는 멤버의 상태는 Primary와 Secondary가 있다.
-   데이터가 모두 동일하게 저장되어 한 대가 죽어도 데이터가 유지될 수 있다.
-   장애 발생 시 투표를 위해 과반수가 살아있어야 하므로 홀수 개로 구성해야 한다.
    -   Replica Set 구성 시 최소 3대로 구성한다.
-   Failover 시에 자동으로 투표를 통해 Secondary 중 하나가 Primary가 된다.
-   서로 heartbeat을 주고 받으며 상태를 체크한다.
-   동일한 크기의 데이터를 저장할 때, RDB와 비교하여 장비 수가 매우 많이 필요하고 메모리도 더 많이 필요하다는 단점이 존재한다.
-   하나의 Replica Set에 데이터를 2TB까지만 저장하는 것을 권장하며, 그 이상일 때는 Sharding으로 장비를 나누도록 권장된다.

### Primary

-   Read/Write 요청을 모두 처리할 수 있다.
-   Write를 처리하는 유일한 멤버이다.
-   ReplicaSet에 하나만 존재할 수 있다.

### Secondary

-   Read에 대한 요청만 처리할 수 있다.
-   복제를 통해 Primary와 동일한 데이터 셋을 유지한다.
-   Replica Set에 여러 개가 존재할 수 있다.

## PSS 구성: Primary, Secondary, Secondary
![image](https://github.com/user-attachments/assets/c4cba855-7d3f-4513-b386-aae910913ce4)

-   Secondary는 Primary의 oplog와 dataset을 복제한다.
    -   oplog: Replica Set의 데이터 동기화를 위해 내부에서 발생하는 모든 동작의 로그를 기록한 것으로 local database 에 저장되는 Collection이다.
-   Primary가 사용 불가능한 경우 새로운 Primary가 될 Secondary를 뽑기위해 투표를 진행하게 된다.

## PSA 구성: Primary, Secondary, Arbiter
![image](https://github.com/user-attachments/assets/29f503bc-5d54-4fa7-8bae-beb08d4b06e1)

-   Primary와 Secondary 하나가 있지만 비용 제약 등으로 인해 Secondary를 더 추가할 수 없을 때 Arbiter로 mongod 인스턴스를 Replica Set에 추가할 수 있다.
    -   Arbiter는 데이터를 가지지 않고 Primary 장애 시 투표에만 참여한다.
    -   저가의 장비로 구성이 가능하다.
    -   Arbiter는 항상 Arbiter이지만 Secondary는 투표를 통해 Primary가 될 수 있다.
-   psa 의 경우 secondary에 장애가 발생하면 primary에 부하(읽기 요청까지 처리해야 되므로)가 발생할 가능성이 크므로, 사용이 크게 권장되지는 않는다.

# Sharded Cluster

-   데이터를 분산 저장하는 솔루션이다. 이를 통해 수평적 확장이 가능하다.
    -   스케일 아웃을 위한 솔루션
![image](https://github.com/user-attachments/assets/48398729-ac2e-4314-8362-77623df2ddb8)
![image](https://github.com/user-attachments/assets/69baca96-51f2-478a-bb56-41663c724922)

### 구성요소

-   Shard: Sharding된 데이터의 하위 집합을 포함하며 각 Shard는 Replica Set으로 구성된다.
    -   Datanode는 데이터 공간으로 2테라 이상이 필요하므로 VM 보다는 물리머신 사용을 권장한다.
    -   하나 이상의 Shard Replica Set이 완전히 장애가 나도 그 외의 Shard에 대해 부분 읽기 및 쓰기를 계속 진행할 수 있다.
-   Config Server: 어떤 데이터가 어디에 저장되어있는지의 정보를 가지며 Replica Set으로 구성된다.
    -   Config Server의 전체 Set에 장애가 발생하면 데이터는 각 샤드에 존재하겠지만 어디에 어떤 데이터가 존재하는지 확인이 불가능해진다.
    -   Config Server는 PSA 구성이 아닌 PSS 구성을 사용하도록 권장된다.
    -   Config 서버는 메타 데이터가 1기가가 잘 넘어가지 않고, 메모리도 많이 필요하지 않으므로 VM 사용 권장한다.
-   mongos(Router): 쿼리 라우터 역할을 하며 클라이언트에서 쿼리를 받아와 Config Server를 통해 데이터가 어디에 있는지 찾고 Shard에서 찾아온 데이터를 조합하여 클라이언트에 돌려준다.
    -   이때 라우터가 SPOF가 될 수 있으므로 Replica Set으로 구성하도록 한다.
    -   클라이언트는 꼭 모든 라우터 서버의 IP를 가지고 있도록 설정하고 랜덤으로 쿼리를 던지도록 설정한다.
    -   Query Router는 매번 Shard 정보를 찾는 동작을 최적화 하기 위해 Config Server의 metadata를 cache로 저장해둔다.

### 그 외 특징

-   Sharding 구성 시 총 서버는 최소 10대가 필요할 수 있다.
-   Sharding을 계속 늘릴수는 있지만 Sharding된 컬렉션을 Unshard 하는 것은 불가능하다.
-   Sharding은 컬렉션 단위로 수행되며, 컬렉션에 따라 샤딩 처리하지 않을 수도 있다.

#### 타겟 쿼리

-   쿼리 조건에 Sharding Key가 존재할 경우 해당 Sharding Key가 포함된 Chunk 정보(샤딩되는 단위)를 라우터의 캐시에서 검색하여 해당 Shard 서버로만 사용자 쿼리를 요청한다.브로드캐스트 쿼리
-   쿼리 조건에 Sharding Key가 없을 경우 모든 Shard 서버로 사용자의 쿼리를 요청한다.

### 장점

-   용량의 한계를 극복할 수 있다.
-   데이터 규모와 부하가 크더라도 처리량이 좋다.
-   고가용성을 보장한다.
-   하드웨어에 대한 제약을 해결할 수 있다.

### 단점

-   관리가 비교적 복잡하다.
-   Replica Set과 비교해서 쿼리가 느리다.

### Sharding

-   하나의 큰 데이터를 여러 서브셋(chunk)으로 나누어 `여러 인스턴스`에 분산 저장하는 기술을 말한다.

#### 참고) Partitioning

-   하나의 큰 데이터를 여러 서브셋으로 나누어 `하나의 인스턴스 내에 여러 테이블`로 나누어 저장하는 기술을 말한다.

### Ranged Sharding
![image](https://github.com/user-attachments/assets/efb8f3ad-b430-4327-b5f6-33b2501403d6)

-   `shard key 값`에 따라서 range를 나눠서 chunks를 분배하는 방식의 sharding이다.
    -   인덱스 필드를 샤드 키로 지정할 수 있다.
    -   데이터의 값으로 샤딩을 수행하기 때문에 타겟 쿼리(데이터가 존재하는 샤드에만 쿼리 수행 요청, 빠르고 효율적)가 가능하다.
-   균등 분배가 불가능하다는 단점이 존재한다.
-   자주 사용되지 않음, 활용되는 경우는 아래와 같다.
    -   해시 샤딩이 불가능한 경우
    -   샤드키가 잘 분산되어 있는 경우
    -   타겟 쿼리가 필수인 경우

### Hashed Sharding
![image](https://github.com/user-attachments/assets/592b4f92-b830-4d9b-b2e8-df69e66af71d)

-   해시드 인덱스가 될 필드를 지정하고, 그 필드들을 기준으로 샤딩을 수행한다.
    -   shard key의 값의 해시 값을 기준으로 샤딩한다.
-   균등하게 분산된다는 장점이 있다.
    -   카디널리티가 낮은 경우(동일 데이터가 많은 경우)에는 어쩔 수 없이 편중이 발생할 수 있다.
-   범위 검색을 수행하는 경우 브로드캐스트 쿼리(모든 샤드에 쿼리 수행 요청)가 발생하며, 성능이 떨어진다는 단점이 있다.
-   가장 일반적으로 사용된다.

### Zone Sharding
![image](https://github.com/user-attachments/assets/199c24de-e224-439a-8aeb-0f93e4bd1406)

-   Ranged, Hashed 샤딩과 함께 활용한다.
-   값에 대해서 존을 생성하여 특정 샤드로 할당한다.
-   글로벌하게 지역적으로 데이터를 분산하여 서비스해야 하는 경우에 활용한다.
    -   ip별로 분산 저장을 하도록 해서 네트워크 레이턴시를 줄이는 방향으로 서비스를 제공해 줄 수 있다.

### Sharded Cluster Balancer

-   Shard Balancer는 모든 Shard 컬렉션에 Chunks를 균등하게 재분배하는 역할(Chunks를 Split하고 Migration하는 역할)을 수행한다.
-   Chunks 분배를 위해서 Balancer는 Chunks가 많은 Shard에서 Chunks 수가 적은 Shard로 Migration을 하는데, 이작업을 모든 Shard에 Chunks가 고르게 분포 될 때까지 수행한다.
-   기본적으로 Shard Balancer는 활성화 상태로 유지된다.
    -   Shard Balancer를 비활성화하는 것도 가능하다.
  
![image](https://github.com/user-attachments/assets/840ea8eb-bb36-44f8-8d5e-385ca8c7945a)

### 참고 자료

-   패스트캠퍼스 대용량 데이터 ~ 강의
-   [https://velog.io/@inhwa1025/MongoDB-MongoDB%EB%9E%80-Replica-Set-Sharding](https://velog.io/@inhwa1025/MongoDB-MongoDB%EB%9E%80-Replica-Set-Sharding)
-   [https://ozofweird.tistory.com/entry/MongoDB-Sharding](https://ozofweird.tistory.com/entry/MongoDB-Sharding)
-   [https://jiku90.tistory.com/13](https://jiku90.tistory.com/13)
