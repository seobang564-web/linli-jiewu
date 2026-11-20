package com.linlijiewu.app.data.model;

/** 会话：以“物品 + 对方”唯一确定。 */
public final class Conversation {
    public final long id;
    public final String peerName;
    public final int avatarColor;
    public final long itemId;
    public final String lastMessage;
    public final String time;
    public final int unread;

    public Conversation(long id, String peerName, int avatarColor, long itemId, String lastMessage, String time, int unread) {
        this.id = id;
        this.peerName = peerName;
        this.avatarColor = avatarColor;
        this.itemId = itemId;
        this.lastMessage = lastMessage;
        this.time = time;
        this.unread = unread;
    }
}
