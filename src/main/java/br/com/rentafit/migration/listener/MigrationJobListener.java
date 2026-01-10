package br.com.rentafit.migration.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Listener para eventos do Job de migração
 * Fornece logging e hooks para ações antes/depois do job
 */
@Component
public class MigrationJobListener implements JobExecutionListener {

    private static final Logger log = LoggerFactory.getLogger(MigrationJobListener.class);

    @Override
    public void beforeJob(JobExecution jobExecution) {
        log.info("=".repeat(60));
        log.info("STARTING MIGRATION JOB");
        log.info("=".repeat(60));
        log.info("Job ID: {}", jobExecution.getId());
        log.info("Job Name: {}", jobExecution.getJobInstance().getJobName());
        log.info("Job Parameters: {}", jobExecution.getJobParameters());
        log.info("Start Time: {}", jobExecution.getStartTime());
        log.info("=".repeat(60));
    }

    @Override
    public void afterJob(JobExecution jobExecution) {
        log.info("=".repeat(60));
        log.info("MIGRATION JOB COMPLETED");
        log.info("=".repeat(60));
        log.info("Job ID: {}", jobExecution.getId());
        log.info("Job Status: {}", jobExecution.getStatus());
        log.info("Start Time: {}", jobExecution.getStartTime());
        log.info("End Time: {}", jobExecution.getEndTime());
        log.info("Duration: {} ms",
            Duration.between(jobExecution.getStartTime(), jobExecution.getEndTime()).toMillis());

        if (jobExecution.getStatus() == BatchStatus.COMPLETED) {
            log.info("✓ JOB COMPLETED SUCCESSFULLY");

            jobExecution.getStepExecutions().forEach(stepExecution -> {
                log.info("  Step: {} - Read: {}, Write: {}, Skip: {}",
                    stepExecution.getStepName(),
                    stepExecution.getReadCount(),
                    stepExecution.getWriteCount(),
                    stepExecution.getSkipCount());
            });
        } else if (jobExecution.getStatus() == BatchStatus.FAILED) {
            log.error("✗ JOB FAILED");

            jobExecution.getAllFailureExceptions().forEach(exception -> {
                log.error("Exception: ", exception);
            });
        } else {
            log.warn("⚠ JOB STATUS: {}", jobExecution.getStatus());
        }

        log.info("=".repeat(60));
    }
}

