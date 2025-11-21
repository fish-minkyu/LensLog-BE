package com.example.LensLog.category.dto;

import com.example.LensLog.category.entity.Category;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryDto {
    private Long categoryId;
    private String categoryName;

    public static CategoryDto fromEntity(Category entity) {
        return CategoryDto.builder()
            .categoryId(entity.getCategoryId())
            .categoryName(entity.getCategoryName())
            .build();
    }
}
