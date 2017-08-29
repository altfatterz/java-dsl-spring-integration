package com.example;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.sql.Date;

@AllArgsConstructor
@Getter
public class ExternalBatchJob {

    private Date endDate;
    private String status;

}
