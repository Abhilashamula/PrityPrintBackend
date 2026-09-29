package com.pingprint.printjob;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PrintJobService {
    private final PrintJobRepository jobs;

    public PrintJobService(PrintJobRepository jobs) {
        this.jobs = jobs;
    }

    @Transactional
    public PrintJob enqueueOnce(PrintOrder order) {
        return jobs.findByOrderId(order.getId()).orElseGet(() -> {
            try {
                return jobs.saveAndFlush(new PrintJob(order));
            } catch (DataIntegrityViolationException duplicate) {
                return jobs.findByOrderId(order.getId()).orElseThrow(() -> duplicate);
            }
        });
    }
}
