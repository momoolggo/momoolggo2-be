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

@Slf4j
@RestController
@RequestMapping("/api/review-photo")
@RequiredArgsConstructor
public class ReviewPhotoUploadController {

    private final OwnerService ownerService;

    @Value("${file.upload.review-path:C:/uploads/review/}")
    private String reviewUploadPath;

    @PostMapping("/upload")
    public ResultResponse<String> uploadReviewPhoto(@RequestParam("file") MultipartFile file) throws IOException {
        String imageUrl = ownerService.uploadImage(file, reviewUploadPath, "/uploads/review/");
        log.info("리뷰 사진 업로드: {}", imageUrl);
        return new ResultResponse<>("리뷰 사진 업로드 성공", imageUrl);
    }
}
