package com.linlijiewu.app.data.repository;

import com.linlijiewu.app.data.remote.ApiResponse;
import com.linlijiewu.app.data.remote.HelloData;
import com.linlijiewu.app.data.remote.LinliApi;
import retrofit2.Call;

public final class BootstrapRepository {
    private final LinliApi api;

    public BootstrapRepository(LinliApi api) {
        this.api = api;
    }

    public Call<ApiResponse<HelloData>> hello() {
        return api.hello();
    }
}
