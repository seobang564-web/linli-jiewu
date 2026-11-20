package com.linlijiewu.app.data.model;

/** 订单卡片所需字段。primaryAction / secondaryAction 为空表示不显示该按钮。 */
public final class Order {
    public final long id;
    public final long itemId;
    public final boolean asOwner;
    public final String peerLabel;
    public final BorrowStatus status;
    public final String meta;
    public final String hint;
    public final boolean warn;
    public final String secondaryAction;
    public final String primaryAction;

    public Order(long id, long itemId, boolean asOwner, String peerLabel, BorrowStatus status, String meta,
                 String hint, boolean warn, String secondaryAction, String primaryAction) {
        this.id = id;
        this.itemId = itemId;
        this.asOwner = asOwner;
        this.peerLabel = peerLabel;
        this.status = status;
        this.meta = meta;
        this.hint = hint;
        this.warn = warn;
        this.secondaryAction = secondaryAction;
        this.primaryAction = primaryAction;
    }
}
