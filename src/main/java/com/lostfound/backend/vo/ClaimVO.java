package com.lostfound.backend.vo;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class ClaimVO {
    private Long claimId;
    private Long postId;
    private String postTitle;
    private Integer postType;
    private Integer postStatus;

    private String otherPartyName;
    private Long otherPartyId;

    private String myRole;

    private Integer claimStatus;

    private Integer ownerConfirmed;
    private Integer claimerConfirmed;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
