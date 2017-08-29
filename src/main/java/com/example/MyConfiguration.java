package com.example;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameter;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.job.SimpleJob;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.integration.launch.JobLaunchRequest;
import org.springframework.batch.integration.launch.JobLaunchingGateway;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.core.MessageSource;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.dsl.core.Pollers;
import org.springframework.integration.endpoint.MethodInvokingMessageSource;
import org.springframework.integration.handler.LoggingHandler;

import java.util.Collections;
import java.util.concurrent.atomic.AtomicLong;


// Java DSL described here
// https://github.com/spring-projects/spring-integration-java-dsl/wiki/spring-integration-java-dsl-reference

// BatchAutoConfiguration -- from Spring Boot
// SimpleBatchConfiguration - from Spring Batch Core

// Spring Batch integration using job launching gateway
// https://stackoverflow.com/questions/27770377/spring-batch-integration-job-launching-gateway

// decoupled event-driven execution of the JobLauncher

@Slf4j
@Configuration
@EnableIntegration
@EnableBatchProcessing
public class MyConfiguration {

    @Autowired
    private JobBuilderFactory jobBuilderFactory;

    @Autowired
    private StepBuilderFactory stepBuilderFactory;

    @Autowired
    private JobLaunchingGateway jobLaunchingGateway;

    @Autowired
    private JobRepository jobRepository;

    @Bean
    public MessageSource<?> integerMessageSource() {
        MethodInvokingMessageSource source = new MethodInvokingMessageSource();
        source.setObject(new AtomicLong());
        source.setMethodName("getAndIncrement");
        return source;
    }

    @Bean
    public DirectChannel inputChannel() {
        return new DirectChannel();
    }

//    @Bean
//    public IntegrationFlow myFlow() {
//       return IntegrationFlows.from(this.integerMessageSource(),
//                c -> c.poller(Pollers.fixedRate(1000))).get();
//

//        return IntegrationFlows.from(this.integerMessageSource(),
//                c -> c.poller(Pollers.fixedRate(1000)))
//                .channel(this.inputChannel())
//                .filter((Integer p) -> p > 0)
//                .transform(message -> new JobLaunchRequest(new SimpleJob("exampleJob"),
//                        new JobParameters(Collections.singletonMap("key", new JobParameter(message.toString())))))
//                .handle(jobLaunchingGateway)
//                .log()
//                .channel(MessageChannels.queue())
//                .get();


//
//    }

    @Bean
    public IntegrationFlow myFlow() {
        System.out.println(jobRepository);

        return IntegrationFlows.from(this.integerMessageSource(),
                c -> c.poller(Pollers.fixedRate(10000)))
                .<Long, JobLaunchRequest>transform(message -> new
                        JobLaunchRequest(
                        new SimpleJob("exampleJob"),
                        new JobParameters(Collections.singletonMap("key", new JobParameter(message)))))
                .handle(jobLaunchingGateway)
                //.handle(logger())
                .get();

    }

    @Bean
    LoggingHandler logger() {
        return new LoggingHandler("INFO");
    }

    @Bean
    Job exampleJob() {
        return jobBuilderFactory.get("exampleJob").start(exampleStep()).build();
    }

    @Bean
    Step exampleStep() {
        return stepBuilderFactory.get("exampleStep").tasklet(
                (contribution, chunkContext) -> {
                    log.info("step finished");
                    return RepeatStatus.FINISHED;
                }
        ).build();
    }

    @Bean
    JobLaunchingGateway jobLaunchingGateway(JobLauncher jobLauncher) {
        return new JobLaunchingGateway(jobLauncher);
    }
}
