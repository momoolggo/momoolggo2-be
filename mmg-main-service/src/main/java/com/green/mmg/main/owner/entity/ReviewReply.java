package com.green.mmg.main.owner.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "review_reply")
@Getter
@Setter
@NoArgsConstructor
public class ReviewReply {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "reply_id")
    private Long replyId;

    @Column(name = "review_id", nullable = false)
    private Long reviewId;

    @Column(name = "owner_id", nullable = false)
    private Long ownerId;

    @Column(name = "content", length = 1000)
    private String content;

    @Column(name = "written_at", insertable = false, updatable = false)
    private LocalDateTime writtenAt;
}
