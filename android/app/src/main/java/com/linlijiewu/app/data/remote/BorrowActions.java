package com.linlijiewu.app.data.remote;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** Available server operations for the current participant. The server remains authoritative. */
public final class BorrowActions {
    private BorrowActions() { }
    public static List<String> forUser(BorrowData order, long userId) {
        boolean owner = order.owner != null && order.owner.id == userId;
        boolean borrower = order.borrower != null && order.borrower.id == userId;
        if (!owner && !borrower) { return Collections.emptyList(); }
        switch (order.status) {
            case "PENDING": return owner ? Arrays.asList("accept", "reject", "cancel") : Collections.singletonList("cancel");
            case "ACCEPTED": return owner ? Arrays.asList("handover", "cancel") : Collections.singletonList("cancel");
            case "BORROWING": return borrower ? Collections.singletonList("return") : Collections.emptyList();
            case "RETURNING": return owner ? Arrays.asList("confirm-return", "deny-return") : Collections.emptyList();
            default: return Collections.emptyList();
        }
    }
}
