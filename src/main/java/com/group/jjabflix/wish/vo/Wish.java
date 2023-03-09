package com.group.jjabflix.wish.vo;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class Wish {

    private long wishId;
    private long profileId;
    private long contentId;
    private LocalDateTime createdAt;

    public Wish(long profileId, long contentId) {
        this.profileId = profileId;
        this.contentId = contentId;
    }

}
