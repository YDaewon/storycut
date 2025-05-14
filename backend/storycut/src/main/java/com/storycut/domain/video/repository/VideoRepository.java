package com.storycut.domain.video.repository;

import com.storycut.domain.video.entity.Video;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VideoRepository extends JpaRepository<Video, Long> {

    @Query("""
        SELECT v FROM Video v
        WHERE v.memberId = :memberId
        AND (
            (:isOriginal = true AND v.originalVideoId IS NULL)
            OR
            (:isOriginal = false AND v.originalVideoId IS NOT NULL)
        )
    """)
    List<Video> findByMemberIdAndIsOriginal(
        @Param("memberId") Long memberId,
        @Param("isOriginal") boolean isOriginal
    );
    
    List<Video> findByOriginalVideoId(Long originalVideoId);
    
}
