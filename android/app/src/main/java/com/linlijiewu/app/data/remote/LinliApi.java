package com.linlijiewu.app.data.remote;

import retrofit2.Call;
import retrofit2.http.GET;

public interface LinliApi {
    @GET("hello")
    Call<ApiResponse<HelloData>> hello();
}
