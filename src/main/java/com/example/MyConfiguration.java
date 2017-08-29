package com.example;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.core.MessageSource;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.dsl.channel.MessageChannels;
import org.springframework.integration.dsl.core.Pollers;
import org.springframework.integration.endpoint.MethodInvokingMessageSource;

import java.util.concurrent.atomic.AtomicInteger;


// Java DSL described here
// https://github.com/spring-projects/spring-integration-java-dsl/wiki/spring-integration-java-dsl-reference


@Slf4j
@Configuration
@EnableIntegration
@EnableBatchProcessing
public class MyConfiguration {

    @Autowired
    private JobBuilderFactory jobBuilderFactory;

    @Autowired
    private StepBuilderFactory stepBuilderFactory;

    @Bean
    public MessageSource<?> integerMessageSource() {
        MethodInvokingMessageSource source = new MethodInvokingMessageSource();
        source.setObject(new AtomicInteger());
        source.setMethodName("getAndIncrement");
        return source;
    }

    @Bean
    public DirectChannel inputChannel() {
        return new DirectChannel();
    }

    @Bean
    public IntegrationFlow myFlow() {
        return IntegrationFlows.from(this.integerMessageSource(), c ->
                c.poller(Pollers.fixedRate(100)))
                .channel(this.inputChannel())
                .filter((Integer p) -> p > 0)
                .transform(Object::toString)
                .log()
                .channel(MessageChannels.queue())
                .get();
    }


    @Bean
    Job exampleJob() {
        return jobBuilderFactory.get("exampleJob").start(exampleStep()).build();
    }

    @Bean
    Step exampleStep() {
        return stepBuilderFactory.get("exampleStep").tasklet((contribution, chunkContext) -> {
                    log.info("step finished");
                    return RepeatStatus.FINISHED;
                }
            ).build();
    }
}
