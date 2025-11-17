package com.example.LensLog.category.entity;

import com.example.LensLog.photo.entity.Photo;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Category {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long categoryId;

    private String categoryName;

    // 하나의 카테고리는 여러 개의 사진을 가질 수 있다.
    @OneToMany(mappedBy = "category")
    @Builder.Default
    private List<Photo> photos = new ArrayList<>();
}
