# kafka-client-java

카프카 클러스터에 책 데이터를 보내고 가져오는 샘플 어플리케이션

데이터는 avro 포맷(binary)으로 직렬화/역직렬화 하여 전송되고 테스트는 testcontainers의 도커 컴포즈 모듈을 사용하여 컨테이너 환경에서 실행된다.

## 요구사항

- Java 21
- Docker, Docker Compose

## 테스트 환경

- Amazon Corretto JDK 21 (OpenJDK)
- Spring Boot 3.2.0
- Docker 24.0.6
- Docker Compose 2.23.0

## 실행 방법

### default

서비스를 프로젝트 외부에서 실행한다.

```shell
### docker build
./gradlew clean build
docker build -f docker/Dockerfile -t kafka-client-java:0.0.1-SNAPSHOT .
### gradle bootBuildImage
./gradlew bootBuildImage --imageName=kafka-client-java:0.0.1-SNAPSHOT

### run
docker-compose -f docker-compose-kafka-local.yaml up -d
docker run -it --rm --network kafka-cluster kafka-client-java:0.0.1-SNAPSHOT
```

### local

프로파일을 local로 설정하면, docker-compose-kafka-local.yaml 파일을 사용한다.

### test

테스트를 실행하면, testcontainers에서 docker-compose-kafka-test.yaml 파일을 사용한다.

## 요청 흐름

클라이언트에서 컨트롤러에 요청을 보내면, 컨트롤러에서 kafkaTemplate(produer)을 사용하여 메시지를 전송한다.

```text
client (cURL) --------> ProduceController ----------->  kafka-cluster
          POST (book/books)             producer send()
             JSON data               Avro data (serialize)
```

컨슈머에서 구독하고 있는 토픽의 메시지를 읽고 데이터를 출력한다.

```text
       print <-------- BookConsumer <-----------  kafka-cluster
                data               consumer poll()
                                Avro data (deserialize)
```

## 요청 예제

```shell
### a book
curl -X POST -H "Content-Type: application/json" http://localhost:8080/api/v1/produce/books/11 -d '{"id":11,"title":"Kafka: The Definitive Guide, 2nd Edition","isbn":"978-1492043089","authors":["Gwen Shapira","Todd Palino","Rajini Sivaram","Krit Petty"],"publisher":"O'\''Reilly Media"}'
### books
curl -X POST -H "Content-Type: application/json" http://localhost:8080/api/v1/produce/books -d '[{"id":1,"title":"Kafka: The Definitive Guide, 2nd Edition","isbn":"978-1492043089","authors":["Gwen Shapira","Todd Palino","Rajini Sivaram","Krit Petty"],"publisher":"O'\''Reilly Media"},{"id":2,"title":"Kafka in Action","isbn":"978-1617295232","authors":["Dylan Scott, Viktor Gamov, Dave Klein"],"publisher":"Manning Publications"},{"id":3,"title":"Kafka Streams in Action","isbn":"978-1617294471","authors":["William P. Bejeck Jr."],"publisher":"Manning Publications"},{"id":4,"title":"Building Event-Driven Microservices","isbn":"978-1492057895","authors":["Adam Bellemare"],"publisher":"O'\''Reilly Media"},{"id":5,"title":"Designing Data-Intensive Applications","isbn":"978-1449373320","authors":["Martin Kleppmann"],"publisher":"O'\''Reilly Media"},{"id":6,"title":"Implementing Domain-Driven Design","isbn":"978-0321834577","authors":["Vaughn Vernon"],"publisher":"Addison-Wesley Professional"},{"id":7,"title":"Fundamentals of Software Architecture","isbn":"978-1492043454","authors":["Mark Richards","Neal Ford"],"publisher":"O'\''Reilly Media"},{"id":8,"title":"Clean Code","isbn":"978-0132350884","authors":["Robert C. Martin"],"publisher":"Pearson"},{"id":9,"title":"Clean Architecture","isbn":"978-0134494166","authors":["Robert C. Martin"],"publisher":"Pearson"},{"id":10,"title":"Design Patterns","isbn":"978-0201633610","authors":["Erich Gamma","Richard Helm","Ralph Johnson","John Vlissides"],"publisher":"Addison-Wesley Professional"}]'
```

## testcontainers 라이브러리

testcontainers를 사용할 때 추상 클래스로 환경을 구성하고 상속을 받거나, 상속을 사용하지 않고 컴포지션 방식으로 코드를 작성하여 사용할 수 있다.

컨테이너는 각 서비스 모듈을 사용하여 구성하거나, 도커 컴포즈 모듈을 사용하여 구성할 수 있다.

### Java (docker compose module)

```java

@ContextConfiguration(initializers = AbstractKafkaClusterSupport.Initializer.class)
abstract class AbstractKafkaClusterSupport {
    static DockerComposeContainer<?> environment =
            new DockerComposeContainer<>(new File("docker/docker-compose-kafka-local.yaml"))
                    .waitingFor("zookeeper", Wait.forHealthcheck())
                    .waitingFor("schema-registry", Wait.forHealthcheck())
                    .withLocalCompose(true);

    static class Initializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
        @Override
        public void initialize(@NotNull ConfigurableApplicationContext applicationContext) {
            environment.start();
        }
    }
}
```

### Kotlin (docker compose module)

```kotlin
@ContextConfiguration(initializers = [AbstractMysqlTestSupport.Initializer::class])
abstract class AbstractMysqlTestSupport {
    companion object {
        val environment = DockerComposeContainer<Nothing>(File("docker/docker-compose-mysql.yaml"))
            .apply { withExposedService("mysql_1", 3306, Wait.forListeningPort()) }
    }

    internal class Initializer : ApplicationContextInitializer<ConfigurableApplicationContext> {
        override fun initialize(configurableApplicationContext: ConfigurableApplicationContext) {
            environment.start()
        }
    }
}
```

### Kotlin (mysql module)

```kotlin
@ContextConfiguration(initializers = [AbstractMysqlTestSupport.Initializer::class])
abstract class AbstractMysqlTestSupport {
    companion object {
        val container = MySQLContainer<Nothing>("mysql:8").apply {
            portBindings = listOf("3306:3306")
            withDatabaseName("exampledb")
            withUsername("root")
            withPassword("test")
        }
    }

    internal class Initializer : ApplicationContextInitializer<ConfigurableApplicationContext> {
        override fun initialize(configurableApplicationContext: ConfigurableApplicationContext) {
            container.start()
        }
    }
}
```

## 참고

- [Testing Applications | Spring for Apache Kafka Reference](https://docs.spring.io/spring-kafka/reference/testing.html)
- [Spring for Apache Kafka | GitHub](https://github.com/spring-projects/spring-kafka)
- [Getting Started (Java) | Apache Avro](https://avro.apache.org/docs/1.11.1/getting-started-java/)
- [Docker Compose Module | Testcontainers](https://java.testcontainers.org/modules/docker_compose/)
- [testcontainers-java | GitHub](https://github.com/testcontainers/testcontainers-java)
