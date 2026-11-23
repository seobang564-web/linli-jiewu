package com.linlijiewu.server.borrow;

import java.util.List;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import com.linlijiewu.server.auth.AuthContext;
import com.linlijiewu.server.auth.RequireLogin;
import com.linlijiewu.server.common.ApiResponse;

@RestController
@RequireLogin
@RequestMapping("/api/borrows")
public class BorrowController {
    private final BorrowService service;
    private final BorrowQueries queries;
    private final BorrowTransactions transactions;

    public BorrowController(BorrowService service, BorrowQueries queries, BorrowTransactions transactions) {
        this.service = service; this.queries = queries; this.transactions = transactions;
    }

    @PostMapping
    public ApiResponse<BorrowModels.Detail> create(@Valid @RequestBody BorrowModels.Create body, HttpServletRequest request) {
        return ApiResponse.success(service.create(AuthContext.require(request), body));
    }

    @GetMapping
    public ApiResponse<BorrowModels.Page> list(@RequestParam String role, @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "20") int size, HttpServletRequest request) {
        return ApiResponse.success(queries.list(AuthContext.require(request), role, status, page, size));
    }

    @GetMapping("/{id}")
    public ApiResponse<BorrowModels.Detail> detail(@PathVariable long id, HttpServletRequest request) {
        return ApiResponse.success(queries.detail(id, AuthContext.require(request)));
    }

    @GetMapping("/{id}/logs")
    public ApiResponse<List<BorrowModels.Log>> logs(@PathVariable long id, HttpServletRequest request) {
        return ApiResponse.success(queries.logs(id, AuthContext.require(request)));
    }

    @PostMapping("/{id}/{action:accept|reject|cancel|handover|return|confirm-return|deny-return}")
    public ApiResponse<BorrowModels.Detail> change(@PathVariable long id, @PathVariable String action,
            @Valid @RequestBody(required = false) BorrowModels.Action body, HttpServletRequest request) {
        return ApiResponse.success(transactions.change(id, AuthContext.require(request), action, body));
    }
}
