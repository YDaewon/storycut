package com.storycut.domain.room.dto.response;

import com.storycut.domain.room.entity.RoomMember;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class RoomMemberResponse {
    
    private Long memberId;
    private Long roomId;
    private LocalDateTime joinedAt;
    
    public static RoomMemberResponse from(RoomMember member) {
        return RoomMemberResponse.builder()
                .memberId(member.getMemberId())
                .roomId(member.getRoom().getId())
                .joinedAt(member.getCreatedAt())
                .build();
    }
}
