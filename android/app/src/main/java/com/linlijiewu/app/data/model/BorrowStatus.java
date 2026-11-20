package com.linlijiewu.app.data.model;

import androidx.annotation.StringRes;
import com.linlijiewu.app.R;

/** 订单状态，与接口文档 BorrowStatus 对应。 */
public enum BorrowStatus {
    PENDING(R.string.borrow_pending),
    ACCEPTED(R.string.borrow_accepted),
    BORROWING(R.string.borrow_borrowing),
    RETURNING(R.string.borrow_returning),
    COMPLETED(R.string.borrow_completed),
    CANCELLED(R.string.borrow_cancelled);

    @StringRes public final int label;

    BorrowStatus(int label) {
        this.label = label;
    }

    /** 进行中的订单卡片顶部带等高线色带。 */
    public boolean isActive() {
        return this != COMPLETED && this != CANCELLED;
    }
}
