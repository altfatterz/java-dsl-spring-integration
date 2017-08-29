CREATE TABLE external_batch_job_execution (
  end_date DATE        NOT NULL,
  status   VARCHAR(50) NOT NULL
);

INSERT INTO external_batch_job_execution VALUES ('2017-08-27', 'FINISHED');
