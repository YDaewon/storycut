package com.storycut.domain.room.entity;

import com.storycut.global.model.entity.BaseEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;


import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "room")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Room extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "room_id")
    private Long id;

    @Column(name = "host_id", nullable = false)
    private Long hostId;

    @Column(name = "room_title", nullable = false)
    private String title;

    @Column(name = "room_context", nullable = false)
    private String context;

    @Column(name = "room_password")
    private String password;

    @Column(name = "room_thumbnail")
    private String thumbnail;

    @OneToMany(mappedBy = "room", cascade = CascadeType.ALL)
    private List<RoomMember> roomMembers = new ArrayList<>();

    @Builder
    public Room(Long hostMemberId, String title, String password, String context, String thumbnail) {
        this.hostId = hostMemberId;
        this.title = title;
        this.password = password;
        this.context = context;
        this.thumbnail = thumbnail;
    }

    public void updateRoom(String title, String password, String context) {
        this.title = title;
        this.password = password;
        this.context = context;
    }

    public void updateThumbnail(String thumbnail) {
        this.thumbnail = thumbnail;
    }
    
    public void updateHostId(Long newHostId) {
        this.hostId = newHostId;
    }

    public boolean isHost(Long memberId) {
        return this.hostId.equals(memberId);
    }
}
