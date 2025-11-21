package com.example.LensLog.photo.dto;

import com.example.LensLog.photo.entity.Photo;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PhotoCursorPageDto {
    private List<PhotoDto> photos;
    private Long nextCursorId;
    private boolean hasNext;
}
