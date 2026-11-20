package com.linlijiewu.app.data.remote;

import android.app.Activity;
import androidx.annotation.NonNull;
import com.google.gson.Gson;
import com.linlijiewu.app.R;
import java.util.function.Consumer;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/** Shared envelope/error handling. UI callbacks only run while the activity is alive. */
public final class ApiCallback<T> implements Callback<ApiResponse<T>> {
    private final Activity activity;
    private final Consumer<T> success;
    private final Consumer<String> failure;
    private final Runnable complete;

    public ApiCallback(Activity activity, Consumer<T> success, Consumer<String> failure, Runnable complete) {
        this.activity = activity; this.success = success; this.failure = failure; this.complete = complete;
    }
    @Override
    public void onResponse(@NonNull Call<ApiResponse<T>> call, @NonNull Response<ApiResponse<T>> response) {
        if (!alive() || call.isCanceled()) { return; }
        complete.run();
        ApiResponse<T> body = response.body();
        if (response.isSuccessful() && body != null && body.getCode() == 0 && body.getData() != null) {
            success.accept(body.getData());
            return;
        }
        String message = activity.getString(R.string.network_response_failed);
        if (body != null && body.getMessage() != null) { message = body.getMessage(); }
        if (response.errorBody() != null) {
            try (var error = response.errorBody()) {
                ApiResponse<?> parsed = new Gson().fromJson(error.charStream(), ApiResponse.class);
                if (parsed != null && parsed.getMessage() != null) { message = parsed.getMessage(); }
            } catch (RuntimeException ignored) { /* Retain generic message for non-JSON errors. */ }
        }
        failure.accept(message);
    }
    @Override
    public void onFailure(@NonNull Call<ApiResponse<T>> call, @NonNull Throwable throwable) {
        if (!alive() || call.isCanceled()) { return; }
        complete.run();
        failure.accept(activity.getString(R.string.network_unavailable));
    }
    private boolean alive() { return !activity.isFinishing() && !activity.isDestroyed(); }
}
