package com.green.mmg.admin.cs.entity;

import com.green.mmg.admin.common.enums.FaqCategory;
import com.green.mmg.admin.cs.dto.FaqReq;
import com.green.mmg.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Entity
@Table(name = "faq")
public class Faq extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "faq_id")
    private Long faqId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", length = 20)
    private FaqCategory type;

    @Column(name = "question", nullable = false, length = 300)
    private String question;

    @Column(name = "answer", nullable = false, length = 200)
    private String answer;

    @Column(name = "is_active")
    private Boolean isActive = true;

    // 생성자
    public Faq(FaqReq req) {
        this.type = req.getType();
        this.question = req.getQuestion();
        this.answer = req.getAnswer();
        this.isActive = true;
    }

    // 수정
    public void update(FaqReq req) {
        this.type = req.getType();
        this.question = req.getQuestion();
        this.answer = req.getAnswer();
    }

    // 노출여부 토글
    public void toggleVisible() {
        this.isActive = !this.isActive;
    }
}