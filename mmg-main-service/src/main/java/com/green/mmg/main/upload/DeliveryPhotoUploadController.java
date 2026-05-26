package com.green.mmg.main.upload;

import com.green.mmg.common.dto.ResultResponse;
import com.green.mmg.main.owner.OwnerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * NOTE: endpoint path = {@code /api/delivery-photo/upload} (Gateway `/**` 매칭 안전성 일관).
 */

/**
 * 라이더 배달 완료 사진 업로드 — 자잘 에러 트랙 #9 (2026-05-23).
 *
 * <p>CLAUDE.md §5 박제 일관: 이미지는 main-service 단독 책임. 라이더 FE → Gateway → main 직접 업로드.
 * 저장된 URL을 받아 라이더 FE가 deliveryService.complete(deliveredPhotoUrl=URL)에 박는 흐름.</p>
 *
 * <p>SecurityConfig {@code /api/delivery-photo/**} = RIDER role 가드.
 * OwnerService.uploadImage 재사용 (범용 thumbnail + UUID 저장 로직).</p>
 */
@Slf4j
@RestController
@RequestMapping("/api/delivery-photo")
@RequiredArgsConstructor
public class DeliveryPhotoUploadController {

    private final OwnerService ownerService;

    @Value("${file.upload.delivery-path:C:/uploads/delivery/}")
    private String deliveryUploadPath;

    @PostMapping("/upload")
    public ResultResponse<String> uploadDeliveryPhoto(@RequestParam("file") MultipartFile file) throws IOException {
        String imageUrl = ownerService.uploadImage(file, deliveryUploadPath, "/uploads/delivery/");
        log.info("배달 완료 사진 업로드: {}", imageUrl);
        return new ResultResponse<>("배달 완료 사진 업로드 성공", imageUrl);
    }
}
