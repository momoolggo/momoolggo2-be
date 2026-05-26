package com.green.mmg.main.chatbot.entity;

import com.green.mmg.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "chat_sessions")
@Getter
@NoArgsConstructor
public class ChatSession extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "session_id")
    private Long sessionId;

    @Column(name = "user_no", nullable = false)
    private Long userNo;

    @Column(name = "pet_no")
    private Long petNo;

    @Enumerated(EnumType.STRING)
    @Column(name = "entry_point", nullable = false)
    private EntryPoint entryPoint;

    @Enumerated(EnumType.STRING)
    @Column(name = "tone_mode", nullable = false)
    private ToneMode toneMode;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private SessionStatus status;

    @Column(name = "escalated_at")
    private LocalDateTime escalatedAt;

    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    public ChatSession(Long userNo, Long petNo, EntryPoint entryPoint, ToneMode toneMode) {
        this.userNo = userNo;
        this.petNo = petNo;
        this.entryPoint = entryPoint;
        this.toneMode = toneMode == null ? ToneMode.PLAYFUL : toneMode;
        this.status = SessionStatus.ACTIVE;
    }

    /** P-7 에스컬레이션: ACTIVE → ESCALATED 전이. CLOSED는 차단. */
    public void escalate() {
        if (this.status == SessionStatus.CLOSED) return;
        if (this.status == SessionStatus.ESCALATED) return;
        this.status = SessionStatus.ESCALATED;
        this.escalatedAt = LocalDateTime.now();
    }

    public void close() {
        if (this.status == SessionStatus.CLOSED) return;
        this.status = SessionStatus.CLOSED;
        this.closedAt = LocalDateTime.now();
    }

    public void changeTone(ToneMode newTone) {
        if (newTone == null) return;
        this.toneMode = newTone;
    }
}
