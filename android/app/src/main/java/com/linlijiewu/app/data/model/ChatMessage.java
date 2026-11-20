package com.linlijiewu.app.data.model;

public final class ChatMessage {
    public enum Type { MINE, PEER, TIME, EVENT }

    public final Type type;
    public final String text;

    public ChatMessage(Type type, String text) {
        this.type = type;
        this.text = text;
    }
}
