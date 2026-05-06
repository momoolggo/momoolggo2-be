package com.green.mmg.admin.notice.repository;

import com.green.mmg.admin.notice.entity.Notice;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NoticeRepository extends JpaRepository<Notice,Long> {
}
