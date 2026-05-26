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

    /** "claimer" 或 "owner" */
    private String myRole;

    /** 0=待确认, 1=一方已确认, 2=已完结, 3=已取消 */
    private Integer claimStatus;

    /** 发布者是否已确认 */
    private Integer ownerConfirmed;
    /** 认领者是否已确认 */
    private Integer claimerConfirmed;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
