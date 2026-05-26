package com.green.mmg.main.mypage;

public record GreenGradeRes(
        String gradeName,
        Integer currentPoint,
        Integer minPoint,
        Integer maxPoint,
        String nextGradeName,
        Integer pointToNextGrade,
        Integer progressPercent
) {
}
