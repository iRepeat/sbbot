package com.zh.sbbot.custom.event;

import com.alibaba.fastjson2.annotation.JSONField;
import com.mikuac.shiro.dto.event.notice.NoticeEvent;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder(toBuilder = true)
@EqualsAndHashCode(callSuper = true)
public class EssenceNoticeEvent extends NoticeEvent {

    @JSONField(name = "group_id")
    private Long groupId;

    @JSONField(name = "message_id")
    private Integer messageId;

    @JSONField(name = "sender_id")
    private Long senderId;

    @JSONField(name = "operator_id")
    private Long operatorId;

    @JSONField(name = "sub_type")
    private String subType;
}
