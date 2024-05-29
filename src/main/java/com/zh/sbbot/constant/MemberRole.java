package com.zh.sbbot.constant;

import lombok.Getter;

/**
 * 群成员类型
 */
@Getter
public enum MemberRole {
    OTHER,
    OWNER,
    ADMIN,
    MEMBER;

    public static MemberRole of(String name) {
        try {
            return MemberRole.valueOf(name.toUpperCase());
        } catch (IllegalArgumentException e) {
            return OTHER;
        }
    }

}

