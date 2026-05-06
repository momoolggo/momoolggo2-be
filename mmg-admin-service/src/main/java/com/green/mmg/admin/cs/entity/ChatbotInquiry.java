package com.green.mmg.admin.cs.entity;

import com.green.mmg.admin.common.enums.InquiryStatus;
import com.green.mmg.admin.common.enums.InquiryUserType;
import com.green.mmg.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@Entity
@Table(name = "chatbot_inquiry")
public class ChatbotInquiry extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "inquiry_id")
    private Long inquiryId;

    @Column(name = "user_no", nullable = false)
    private Long userNo;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", length = 20)
    private InquiryUserType category;

    @Column(name = "content", length = 500)
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(name = "state", length = 20)
    private InquiryStatus state;

    @Column(name = "minimum_content", length = 500)
    private String minimumContent;

    @Column(name = "answer", length = 200)
    private String answer;

    @Column(name = "answered_at")
    private LocalDateTime answeredAt;

    // 답변 등록
    public void reply(String answer) {
        this.answer = answer;
        this.answeredAt = LocalDateTime.now();
        this.state = InquiryStatus.RESOLVED;
    }
}