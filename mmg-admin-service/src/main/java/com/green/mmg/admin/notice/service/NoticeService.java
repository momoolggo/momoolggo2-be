package com.green.mmg.admin.notice.service;

import com.green.mmg.admin.notice.dto.NoticeReq;
import com.green.mmg.admin.notice.entity.Notice;
import com.green.mmg.admin.notice.repository.NoticeRepository;
import com.green.mmg.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NoticeService {

    private final NoticeRepository noticeRepository;

    // 공지 목록 조회
    public List<Notice> getNoticeList() {
        return noticeRepository.findAll();
    }

    @Transactional
    public void createNotice(NoticeReq req) {
        noticeRepository.save(new Notice(req));
    }

    // 공지 수정
    @Transactional
    public void updateNotice(Long noticeId, NoticeReq req) {
        Notice notice = noticeRepository.findById(noticeId)
                .orElseThrow(() -> new ResourceNotFoundException("공지사항을 찾을 수 없습니다."));
        notice.update(req);
    }

    // 공지 삭제
    @Transactional
    public void deleteNotice(Long noticeId) {
        noticeRepository.findById(noticeId)
                .orElseThrow(() -> new ResourceNotFoundException("공지사항을 찾을 수 없습니다."));
        noticeRepository.deleteById(noticeId);
    }
}