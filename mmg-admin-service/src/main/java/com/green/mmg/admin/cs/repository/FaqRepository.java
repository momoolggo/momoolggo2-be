package com.green.mmg.admin.cs.repository;

import com.green.mmg.admin.common.enums.FaqCategory;
import com.green.mmg.admin.cs.entity.Faq;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FaqRepository extends JpaRepository<Faq, Long> {
    // 카테고리별 조회
    List<Faq> findByType(FaqCategory type);
}