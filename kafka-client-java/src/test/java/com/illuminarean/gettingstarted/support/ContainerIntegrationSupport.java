package com.illuminarean.gettingstarted.support;

import org.jetbrains.annotations.NotNull;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.testcontainers.containers.DockerComposeContainer;
import org.testcontainers.containers.wait.strategy.Wait;

import java.io.File;

@ContextConfiguration(initializers = ContainerIntegrationSupport.Initializer.class)
public abstract class ContainerIntegrationSupport {
    protected static DockerComposeContainer<?> environment =
            new DockerComposeContainer<>(new File("docker/docker-compose-kafka-local.yaml"))
                    .waitingFor("zookeeper", Wait.forHealthcheck())
                    .waitingFor("schema-registry", Wait.forHealthcheck())
                    .withLocalCompose(true);

    protected static class Initializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
        @Override
        public void initialize(@NotNull ConfigurableApplicationContext applicationContext) {
            environment.start();
        }
    }
}
