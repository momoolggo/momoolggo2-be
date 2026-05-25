package com.green.mmg.main.greenpoint;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "green_point_log")
@Getter
@NoArgsConstructor
public class GreenPointLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "green_point_log_id")
    private Long greenPointLogId;

    @Column(name = "order_id", nullable = false, unique = true)
    private Long orderId;

    @Column(name = "user_no", nullable = false)
    private Long userNo;

    @Column(name = "point", nullable = false)
    private Integer point;

    @Column(name = "reason", nullable = false, length = 100)
    private String reason;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "retry_count", nullable = false)
    private Integer retryCount;

    @Column(name = "last_error", length = 500)
    private String lastError;

    public static GreenPointLog pending(Long orderId, Long userNo, Integer point, String reason) {
        GreenPointLog log = new GreenPointLog();
        log.orderId = orderId;
        log.userNo = userNo;
        log.point = point;
        log.reason = reason;
        log.status = "PENDING";
        log.retryCount = 0;
        return log;
    }

    public void markSuccess() {
        this.status = "SUCCESS";
        this.lastError = null;
    }

    public void markFailed(String error) {
        this.status = "FAILED";
        this.retryCount = this.retryCount + 1;
        this.lastError = error == null ? null : error.substring(0, Math.min(error.length(), 500));
    }
}