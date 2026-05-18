package com.green.mmg.admin.report.dto;

import java.util.List;

public record AiReviewJudgement(
        boolean shouldBlind,
        String confidence,   // HIGH, MEDIUM, LOW
        String reason,
        List<String> violations
) {}
