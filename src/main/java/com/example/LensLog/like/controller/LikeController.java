package com.example.LensLog.like.controller;

import com.example.LensLog.like.service.LikeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/like")
@RequiredArgsConstructor
public class LikeController {
    private final LikeService likeService;

    // 좋아요 생성
    @PostMapping("/good")
    public void saveLike(
        // Post 방식이지만 photoId만 필요하므로 간단하게 @RequestParam 사용
        @RequestParam("photoId") Long photoId
    ) {
        likeService.saveLike(photoId);
    }

    // 좋아요 삭제
    @DeleteMapping("/delete")
    public void deleteLike(
        // Delete 방식이지만 photoId만 필요하므로 간단하게 @RequestParam 사용
        @RequestParam("photoId") Long photoId
    ) {
        likeService.deleteLike(photoId);
    }
}
