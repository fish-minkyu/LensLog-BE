package com.example.LensLog.category.controller;

import com.example.LensLog.category.entity.Category;
import com.example.LensLog.category.service.CategoryService;
import com.example.LensLog.photo.dto.PhotoCursorPageDto;
import com.example.LensLog.photo.service.PhotoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/category")
@RequiredArgsConstructor
public class CategoryController {
    private final CategoryService categoryService;
    private final PhotoService photoService;

    // 카테고리 생성
    @PostMapping
    public ResponseEntity<String> makeCategory(@RequestBody Category entity) {
        categoryService.makeCategory(entity);
        return ResponseEntity.ok("카테고리 생성 완료.");
    }

    // 카테고리 리스트 조회
    @GetMapping
    public List<Category> getCategoryList() {
        return categoryService.getCategoryList();
    }

    // 카테고리별 photo 조회
    @GetMapping("/{categoryId}/getList")
    public PhotoCursorPageDto getPhotoListGroupByCategory(
        @PathVariable Long categoryId,
        @RequestParam(name = "lastPhotoId", required = false) Long lastPhotoId,
        @RequestParam(name = "pageSize", defaultValue = "10") int pageSize
    ) {
        return  photoService.getListPhotoCursor(categoryId, lastPhotoId, pageSize);
    }

    // 카테고리 수정
    @PutMapping("/update/{categoryId}")
    public void updateCategory(
        @PathVariable Long categoryId,
        @RequestBody Category entity
    ) {
        categoryService.updateCategory(categoryId, entity);
    }

    // 카테고리 삭제
    @DeleteMapping("/delete/{categoryId}")
    public void deleteCategory(@PathVariable Long categoryId) {
        categoryService.deleteCategory(categoryId);
    }
}
