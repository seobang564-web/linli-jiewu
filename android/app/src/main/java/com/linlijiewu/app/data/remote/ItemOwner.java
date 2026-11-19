package com.linlijiewu.app.data.remote;

/**
 * 物品物主的公开信息（接口文档：仅 id 与 nickname 对外）。
 */
public final class ItemOwner {
    private long id;
    private String nickname;

    public long getId() {
        return id;
    }

    public String getNickname() {
        return nickname;
    }
}
