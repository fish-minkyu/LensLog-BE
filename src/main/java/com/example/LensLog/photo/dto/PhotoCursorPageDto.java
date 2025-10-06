package com.example.LensLog.photo.dto;

import com.example.LensLog.photo.entity.Photo;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class PhotoCursorPageDto {
    private List<Photo> photos;
    private Long nextCursorId;
    private boolean hasNext;

    public PhotoCursorPageDto(List<Photo> photos, Long nextCursorId, boolean hasNext) {
        this.photos = new ArrayList<>(photos);
        this.nextCursorId = nextCursorId;
        this.hasNext = hasNext;
    }
}
