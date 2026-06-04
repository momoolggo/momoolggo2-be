package com.green.mmg.admin.report.ai;

import com.green.mmg.admin.report.event.ReviewReportSubmittedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class ReviewReportAiProcessor {

    private final ReviewReportAiProcessService processService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async("aiTaskExecutor")
    public void onReportSubmitted(ReviewReportSubmittedEvent event) {
        processService.process(event.reportId());
    }
}
