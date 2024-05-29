package com.zh.sbbot.model.event;

import com.alibaba.fastjson2.annotation.JSONField;
import com.mikuac.shiro.dto.event.notice.NoticeEvent;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder(toBuilder = true)
@EqualsAndHashCode(callSuper = true)
public class GroupMsgEmojiLikeNoticeEvent extends NoticeEvent {

    @JSONField(name = "user_id")
    private Long userId;

    @JSONField(name = "group_id")
    private Long groupId;

    @JSONField(name = "message_id")
    private Integer messageId;

    @JSONField(name = "likes")
    private List<Likes> likes;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Likes {
        @JSONField(name = "emoji_id")
        private String emojiId;

        @JSONField(name = "count")
        private Integer count;
    }

}
