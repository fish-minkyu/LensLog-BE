package com.example.LensLog.good.controller;

import com.example.LensLog.good.service.GoodService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Like Controller", description = "좋아요 기능 관련 API")
@RestController
@RequestMapping("/api/good")
@RequiredArgsConstructor
public class GoodController {
    private final GoodService goodService;

    // 좋아요 생성 및 삭제
    @PostMapping("/{photoId}")
    public boolean saveLike(
        @PathVariable Long photoId
    ) {
        return goodService.toggleLike(photoId);
    }
}
