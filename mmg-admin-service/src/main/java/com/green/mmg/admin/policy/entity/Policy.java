package com.green.mmg.admin.policy.entity;

import com.green.mmg.admin.policy.dto.PolicyReq;
import com.green.mmg.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Entity
@Table(name = "policy")
public class Policy extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "policy_id")
    private Long policyId;

    @Column(name = "type", nullable = false, length = 20)
    private String type;

    @Column(name = "version")
    private Integer version;

    @Column(name = "title", nullable = false, length = 100)
    private String title;

    @Column(name = "content", nullable = false, length = 1000)
    private String content;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive;

    // 생성자
    public Policy(PolicyReq req) {
        this.type = req.getType();
        this.version = 1;
        this.title = req.getTitle();
        this.content = req.getContent();
        this.isActive = true;
    }

    // 수정 (버전 +1)
    public void update(PolicyReq req) {
        this.version = this.version + 1;
        this.title = req.getTitle();
        this.content = req.getContent();
    }

    // 비활성화
    public void deactivate() {
        this.isActive = false;
    }
}