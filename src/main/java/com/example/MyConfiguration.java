package com.example;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameter;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.SimpleJob;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.integration.launch.JobLaunchRequest;
import org.springframework.batch.integration.launch.JobLaunchingGateway;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.mapping.PassThroughLineMapper;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.integration.annotation.IntegrationComponentScan;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.core.MessageSource;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.dsl.core.Pollers;
import org.springframework.integration.endpoint.MethodInvokingMessageSource;
import org.springframework.integration.file.FileReadingMessageSource;
import org.springframework.integration.file.FileReadingMessageSource.WatchEventType;
import org.springframework.integration.file.filters.SimplePatternFileListFilter;
import org.springframework.integration.handler.LoggingHandler;

import java.io.File;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicLong;


// Java DSL described here
// https://github.com/spring-projects/spring-integration-java-dsl/wiki/spring-integration-java-dsl-reference

// BatchAutoConfiguration -- from Spring Boot
// SimpleBatchConfiguration - from Spring Batch Core

// Spring Batch integration using job launching gateway
// https://stackoverflow.com/questions/27770377/spring-batch-integration-job-launching-gateway

// decoupled event-driven execution of the JobLauncher

// Related examples
// https://github.com/pakmans/spring-batch-integration-example


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

    @Bean
    public MessageSource<File> fileReadingMessageSource() {
        FileReadingMessageSource source = new FileReadingMessageSource();
        source.setDirectory(new File("dropfolder"));
        source.setFilter(new SimplePatternFileListFilter("*.txt"));
        source.setUseWatchService(true);
        source.setWatchEvents(WatchEventType.CREATE);
        return source;
    }

    @Bean
    public IntegrationFlow myFlow() {
        return IntegrationFlows.from(fileReadingMessageSource(),
                c -> c.poller(Pollers.fixedRate(5000, 2000)))
                .transform(fileMessageToJobLaunchRequest())
                .handle(jobLaunchingGateway)
                .handle(logger())
                .get();
    }

    @Bean
    FileMessageToJobLaunchRequest fileMessageToJobLaunchRequest() {
        FileMessageToJobLaunchRequest transformer = new FileMessageToJobLaunchRequest();
        transformer.setJob(exampleJob());
        transformer.setFileParameterName("file_path");
        return transformer;
    }

    @Bean
    LoggingHandler logger() {
        return new LoggingHandler("INFO");
    }

    @Bean
    JobLaunchingGateway jobLaunchingGateway(JobLauncher jobLauncher) {
        return new JobLaunchingGateway(jobLauncher);
    }

    // ----------------------------------------------------------------------------------------- //

    @Bean
    Job exampleJob() {
        return jobBuilderFactory.get("exampleJob")
                .start(exampleStep())
                .build();
    }

    @Bean
    Step exampleStep() {
        return stepBuilderFactory.get("exampleStep")
                .<String, String>chunk(5)
                .reader(itemReader(null))
                .writer(i -> i.stream().forEach(j -> System.out.println(j)))
                .build();
    }

    @Bean
    @StepScope
    FlatFileItemReader<String> itemReader(@Value("#{jobParameters[file_path]}") String filePath) {
        FlatFileItemReader<String> reader = new FlatFileItemReader<>();
        FileSystemResource fileResource = new FileSystemResource(filePath);
        reader.setResource(fileResource);
        reader.setLineMapper(new PassThroughLineMapper());
        return reader;
    }


}
