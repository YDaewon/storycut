package com.storycut.domain.video.dto.request;

import lombok.Getter;

@Getter
public class UploadComplate {
    private Long videoId;
    private String videoUrl;
    private String thumbnail;

}
