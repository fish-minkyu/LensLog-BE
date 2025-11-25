package com.example.LensLog.category.service;

import com.example.LensLog.category.dto.CategoryDto;
import com.example.LensLog.category.entity.Category;
import com.example.LensLog.category.repo.CategoryRepository;
import com.example.LensLog.photo.entity.Photo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CategoryService {
    private final CategoryRepository categoryRepository;

    // 카테고리 생성
    public void makeCategory(CategoryDto dto) {
        Category category = Category.builder()
            .categoryName(dto.getCategoryName())
            .build();

        categoryRepository.save(category);
    }

    // 카테고리 리스트 조회
    public List<CategoryDto> getCategoryList() {
        List<CategoryDto> results = new ArrayList<>();

        List<Category> categories = categoryRepository.findAll();
        for (Category entity : categories) {
            results.add(CategoryDto.fromEntity(entity));
        }

        return results;
    }

    // 카테고리 수정
    public void updateCategory(Long categoryId, Category entity) {
        Category targetCategory = categoryRepository.findById(categoryId)
            .orElseThrow(() -> new IllegalStateException(
                "해당하는 카테고리 ID가 없습니다: " + categoryId
            ));

        Category newCate = Category.builder()
            .categoryId(targetCategory.getCategoryId())
            .categoryName(entity.getCategoryName())
            .build();

        categoryRepository.save(newCate);
    }

    // 카테고리 삭제
    public void deleteCategory(Long categoryId) {
        Category targetCategory = categoryRepository.findById(categoryId)
            .orElseThrow(() -> new IllegalStateException(
                "해당하는 카테고리 ID가 없습니다: " + categoryId
            ));

        categoryRepository.delete(targetCategory);
    }
}
