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
 * 라이더 가입 면허증 사진 업로드 — 2026-05-28 트랙.
 *
 * <p>CLAUDE.md §5 박제 일관: 이미지는 main-service 단독 책임. 라이더 FE → Gateway → main 직접 업로드.
 * owner signup-doc/upload 박제 일관 (가입 *이전* permitAll). 토큰 없는 상태에서 호출.</p>
 *
 * <p>SecurityConfig {@code /api/rider-license/upload} POST = permitAll.
 * OwnerService.uploadImage 재사용 (범용 thumbnail + UUID 저장 로직, DeliveryPhotoUploadController 박제 일관).</p>
 */
@Slf4j
@RestController
@RequestMapping("/api/rider-license")
@RequiredArgsConstructor
public class RiderLicenseUploadController {

    private final OwnerService ownerService;

    @Value("${file.upload.rider-license-path:C:/uploads/rider-license/}")
    private String riderLicenseUploadPath;

    @PostMapping("/upload")
    public ResultResponse<String> uploadRiderLicense(@RequestParam("file") MultipartFile file) throws IOException {
        String imageUrl = ownerService.uploadImage(file, riderLicenseUploadPath, "/uploads/rider-license/");
        log.info("라이더 면허증 사진 업로드: {}", imageUrl);
        return new ResultResponse<>("면허증 사진 업로드 성공", imageUrl);
    }
}
