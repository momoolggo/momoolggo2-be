package com.green.mmg.main.owner;

import com.green.mmg.main.owner.entity.ReviewReply;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ReviewReplyRepository extends JpaRepository<ReviewReply, Long> {

    Optional<ReviewReply> findByReplyIdAndOwnerId(Long replyId, Long ownerId);

    boolean existsByReviewId(Long reviewId);

}
