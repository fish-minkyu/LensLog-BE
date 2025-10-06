package com.example.LensLog.photo.repo;

import com.example.LensLog.photo.entity.Photo;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PhotoRepository extends JpaRepository<Photo, Long>, QPhotoRepository {
    // 해시 값으로 이미지 조회
    Optional<Photo> findByHashValue(String hashValue);

    // 해시 값으로 이미지를 조회하면서 해당 레코드에 락을 거는 쿼리
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Photo p WHERE p.hashValue = :hashValue")
    Optional<Photo> findForUpdateByHashValue(@Param("hashValue") String hashValue);
}
