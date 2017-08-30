package com.example;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.integration.launch.JobLaunchRequest;
import org.springframework.messaging.Message;

import java.util.Date;
import java.util.List;

public class ListMessageToJobLaunchRequest {

    private Job job;

    public void setJob(Job job) {
        this.job = job;
    }

    public JobLaunchRequest toRequest(Message<List<ExternalBatchJob>> message) {
        JobParametersBuilder jobParametersBuilder = new JobParametersBuilder();

        jobParametersBuilder.addString("end_date", message.getPayload().get(0).getEndDate().toString());
        jobParametersBuilder.addDate("dummy", new Date());

        return new JobLaunchRequest(job, jobParametersBuilder.toJobParameters());
    }
}
