-- Prevent duplicate review reports from the same reporter.
-- Run after cleaning existing duplicates, if any.
ALTER TABLE report
    ADD CONSTRAINT uq_report_reporter_target
        UNIQUE (reporter_no, target_type, target_no);

-- Keep one admin blind record per review so concurrent AI processing cannot
-- create duplicate blind rows for the same review.
ALTER TABLE blind
    ADD CONSTRAINT uq_blind_review_no
        UNIQUE (review_no);
