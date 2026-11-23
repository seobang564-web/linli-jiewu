package com.linlijiewu.server.borrow;

import java.util.Locale;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

@Service
public class BorrowService {
    private final BorrowMapper borrows;
    private final BorrowTransactions transactions;
    private final BorrowQueries queries;

    public BorrowService(BorrowMapper borrows, BorrowTransactions transactions, BorrowQueries queries) {
        this.borrows = borrows; this.transactions = transactions; this.queries = queries;
    }

    public BorrowModels.Detail create(long borrower, BorrowModels.Create request) {
        String requestId = request.requestId().toLowerCase(Locale.ROOT);
        BorrowRow existing = borrows.findRequest(borrower, requestId);
        if (existing != null) { return queries.detail(existing.id, borrower); }
        try { return transactions.create(borrower, request, requestId); }
        catch (DuplicateKeyException exception) {
            // A concurrent request for another item may win the unique key. Read only after our transaction rolls back.
            existing = borrows.findRequest(borrower, requestId);
            if (existing == null) { throw exception; }
            return queries.detail(existing.id, borrower);
        }
    }
}
