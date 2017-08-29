package com.example;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.integration.launch.JobLaunchRequest;
import org.springframework.integration.annotation.Transformer;
import org.springframework.messaging.Message;

import java.util.Date;
import java.util.List;

public class ListMessageToJobLaunchRequest {

    private Job job;
    private String parameterName;

    public void setJob(Job job) {
        this.job = job;
    }

    public void setParameterName(String parameterName) {
        this.parameterName = parameterName;
    }

    @Transformer
    public JobLaunchRequest toRequest(Message<List<ExternalBatchJob>> message) {
        JobParametersBuilder jobParametersBuilder = new JobParametersBuilder();

        jobParametersBuilder.addString("end_date", message.getPayload().get(0).getEndDate().toString());
        jobParametersBuilder.addDate("dummy", new Date());

        return new JobLaunchRequest(job, jobParametersBuilder.toJobParameters());
    }
}
