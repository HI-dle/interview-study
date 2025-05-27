# MongoDB

유연하고 확장성 높은 OpenSource Document 지향 Database 이다.

## 주요 특징

-   Schema 가 자유롭다.
-   HA와 Scale-Out Solution을 자체적으로 지원해서 확장이 쉽다.
-   Secondary Index를 지원하는 NoSQL이다.
-   다양한 종류의 Index를 제공한다.
-   응답 속도가 빠르다.
-   배우기 쉽고 간편하게 개발이 가능하다.

# SQL vs. NoSQL
![image](https://github.com/user-attachments/assets/03bec196-cd4e-4b09-91cb-0affbc316d4e)
-   이미지 출처:[https://meetup.nhncloud.com/posts/274](https://meetup.nhncloud.com/posts/274)

## 관계형 데이터베이스

#### 장점

-   정규화를 통해 데이터의 중복을 방지할 수 있다.
-   Join의 성능이 좋다.
-   복잡하고 다양한 쿼리가 가능하다.
-   잘못된 입력을 방지할 수 있다.

#### 단점

-   하나의 레코드를 확인하기 위해 여러 테이블을 Join 하여 가시성 혹은 성능이 떨어질 수 있다.
-   스키마가 엄격해서 변경에 대한 공수가 크다.
-   Scale-Out이 가능하긴 하지만 설정이 어렵다.
    -   자체적으로 지원하는 Scaling Out 솔루션이 없어서, thirdParty 솔루션을 활용하거나 자체 Application 레벨에서 작업이 필요하다.
        -   후자의 경우 확장할 때마다 Application 코드의 수정이 필요하다.
    -   전통적으로 Scale-Up 위주로 확장했다.

## NoSQL

-   관계형 데이터베이스로 처리하기 어려운 데이터를 저장하기 위해 등장했다.
-   **Document Store(MongoDB**), Key-Value Store, Wide-Column Store, Graph Store
    -   그 외 Search Engine, Time-Series Store

# MongoDB

#### 장점

-   데이터 접근성과 가시성이 좋다.
-   Join 없이 조회가 가능해서 응답 속도가 일반적으로 빠르다.
-   스키마 변경에 공수가 적다.
-   스키마가 유연해서 데이터 모델을 App의 요구사항에 맞게 데이터를 수용할 수 있다.

#### 단점

-   데이터의 중복이 발생한다.
-   스키마가 자유롭지만, 스키마 설계를 잘해야 성능 저하를 피할 수 있다.
-   Join을 지원하지 않는다.
-   제한적인 트랜잭션 기능

#### Scaling

-   HA 와 Sharding 에 대한 솔루션을 자체적으로 지원하고 있어 스케일 아웃이 간편하다.
-   확장 시, Application 의 변경사항이 없다.

### MongoDB 구조
![image](https://github.com/user-attachments/assets/bc63b305-8b2e-4fef-9539-4e28c3de6ee4)

### 기본 Database

-   루트 권한 유저일 때 아래 기본 데이터베이스들을 확인할 수 있다.

#### admin

-   인증과 권한 부여 역할을 한다.
-   일부 관리 작업을 하려면 admin Database 에 대한 접근 권한이 필요하다.

#### config

-   sharded cluster 에서 각 shard의 정보를 저장한다.

#### local

-   모든 mongod instance 는 local database를 소유한다.
-   oplog와 같은 replication 절차에 필요한 정보를 저장한다.
-   startup\_log와 같은 instance 진단 정보를 저장한다.
-   local database 자체는 복제되지 않는다.

### Collection 특징

-   동적 스키마를 갖고 있어서 스키마를 수정하려면 필드 값을 추가/수정/삭제하면 된다.
    -   운영 및 관리를 위해 최소한의 스키마는 유지할 필요가 있다.
-   Collection 단위로 index를 생성할 수 있다.
-   Collection 단위로 Shard를 나눌 수 있다.

### Document 특징

-   JSON 형태로 표현하고 BSON(Binary JSON) 형태로 저장한다.
-   모든 Document에는 "\_id" 필드가 있고, 해당 값 없이 생성하면 ObjectId 타입의 고유한 값을 저장한다.
-   생성 시, 상위 구조인 Database 나 Collection이 없다면, (자동으로) 먼저 생성한 다음 Document를 생성한다.
-   Document 의 최대 크기는 16MB 이다.

#### bson

-   binary json
-   json을 바이트 문자열로 표현할 수 있는 경량 바이너리 형식

# MongoDB 배포 형태

## StandAlone
-   학습이나 테스트 용도

## ReplicaSet
![image](https://github.com/user-attachments/assets/c8ca569b-184b-41ea-aeb8-d3bab897fbdd)

-   인스턴스 3대를 활용한다.
    -   하나의 Primary와 2 대의 복제본을 둔다.
    -   현업에서 가장 많이 사용되는 배포 형태
-   HA 고가용성을 보장한다.

## Sharded Cluster
![image](https://github.com/user-attachments/assets/d696794c-77e8-4847-b589-c499d688539e)

-   3개의 샤드로 나누어서 데이터를 저장하고 **트래픽을 분산**한다.
-   각 샤드는 ReplicaSet 으로 구성된다.
    -   HA 보장
-   출처
    -   패스트캠퍼스 대용량 데이터~ 강의 중
