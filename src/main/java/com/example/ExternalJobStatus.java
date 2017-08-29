package com.example;

import java.util.Date;

public class ExternalJobStatus {

    private Date date;
    private Status status;

    enum Status {
        FINISHED, STARTED
    }
}
