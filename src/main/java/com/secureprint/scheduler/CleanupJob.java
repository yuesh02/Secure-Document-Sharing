package com.secureprint.scheduler;

import com.secureprint.entity.Document;
import com.secureprint.repository.DocumentRepository;
import com.secureprint.service.CleanupService;
import java.time.Clock;
import java.time.Instant;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class CleanupJob {
    private final DocumentRepository documents;
    private final CleanupService cleanup;
    private final Clock clock;
    public CleanupJob(DocumentRepository documents, CleanupService cleanup, Clock clock) {
        this.documents=documents; this.cleanup=cleanup; this.clock=clock;
    }
    @Scheduled(fixedRateString = "${app.cleanup.interval-ms:60000}")
    public void run() {
        for (Document document : documents.findDocumentsForCleanup(Instant.now(clock))) {
            try {
                cleanup.cleanup(document);
            } catch (RuntimeException failure) {
                cleanup.logFailure(document, failure);
            }
        }
    }
}
