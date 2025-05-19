package com.storycut.domain.room.service;

import com.storycut.global.exception.BusinessException;
import com.storycut.global.model.dto.BaseResponseStatus;
import java.time.Duration;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RoomInviteService {
    private final RedisTemplate<String, String> redisTemplate;

    private static final String INVITE_KEY_PREFIX = "invite:";

    @Transactional
    public String generateInviteCode(Long roomId) {
        String inviteCode = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        redisTemplate.opsForValue().set(INVITE_KEY_PREFIX + inviteCode, roomId.toString(), Duration.ofMinutes(10));
        return inviteCode;
    }

    @Transactional
    public Long decodeInviteCode(String inviteCode) {
        String roomId = redisTemplate.opsForValue().get(INVITE_KEY_PREFIX + inviteCode);
        if (roomId == null) {
            throw new BusinessException(BaseResponseStatus.INVALID_INVITE_CODE);
        }
        return Long.valueOf(roomId);
    }
}
