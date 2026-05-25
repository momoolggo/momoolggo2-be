package com.green.mmg.main.attendance.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 2026-05-25 9건 트랙 #8 부채 Step B — 출석 로그 (my_mmg_main.attendance_log).
 *
 * <p>UNIQUE (user_no, attendance_date): 하루 1회 출석 보장 (race 시 DataIntegrityViolationException).</p>
 */
@Entity
@Table(name = "attendance_log",
        uniqueConstraints = @UniqueConstraint(name = "uk_user_date",
                columnNames = {"user_no", "attendance_date"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AttendanceLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "attendance_id")
    private Long attendanceId;

    @Column(name = "user_no", nullable = false)
    private Long userNo;

    @Column(name = "attendance_date", nullable = false)
    private LocalDate attendanceDate;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    public AttendanceLog(Long userNo, LocalDate attendanceDate) {
        this.userNo = userNo;
        this.attendanceDate = attendanceDate;
    }
}
