package com.linlijiewu.app.ui.bootstrap;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.linlijiewu.app.BuildConfig;
import com.linlijiewu.app.R;
import com.linlijiewu.app.data.remote.ApiClient;
import com.linlijiewu.app.data.remote.ApiResponse;
import com.linlijiewu.app.data.remote.HelloData;
import com.linlijiewu.app.data.repository.BootstrapRepository;
import com.linlijiewu.app.databinding.ActivityMainBinding;
import com.linlijiewu.app.ui.main.HomeActivity;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {
    private ActivityMainBinding binding;
    private BootstrapRepository repository;
    private Call<ApiResponse<HelloData>> activeCall;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (view, insets) -> {
            androidx.core.graphics.Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });

        // Login and token persistence are implemented in week 2.
        repository = new BootstrapRepository(ApiClient.create(BuildConfig.SERVER_BASE_URL, () -> null));
        binding.serverAddress.setText(getString(R.string.server_address, BuildConfig.SERVER_BASE_URL));
        binding.checkConnection.setOnClickListener(view -> checkConnection());
        binding.openDiscover.setOnClickListener(view ->
                startActivity(new Intent(this, HomeActivity.class)));
    }

    private void checkConnection() {
        binding.checkConnection.setEnabled(false);
        binding.connectionProgress.setVisibility(View.VISIBLE);
        setStatus(getString(R.string.connection_loading), R.color.text_secondary);
        activeCall = repository.hello();
        activeCall.enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<HelloData>> call,
                                   @NonNull Response<ApiResponse<HelloData>> response) {
                if (isFinishing() || isDestroyed()) {
                    return;
                }
                finishLoading();
                ApiResponse<HelloData> body = response.body();
                if (response.isSuccessful() && body != null && body.getCode() == 0
                        && body.getData() != null && body.getData().getMessage() != null) {
                    setStatus(getString(R.string.connection_success, body.getData().getMessage()), R.color.success);
                } else {
                    setStatus(getString(R.string.connection_server_error), R.color.error);
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<HelloData>> call, @NonNull Throwable throwable) {
                if (call.isCanceled() || isFinishing() || isDestroyed()) {
                    return;
                }
                finishLoading();
                setStatus(getString(R.string.connection_failed), R.color.error);
            }
        });
    }

    private void finishLoading() {
        binding.checkConnection.setEnabled(true);
        binding.connectionProgress.setVisibility(View.GONE);
    }

    private void setStatus(String text, int colorResource) {
        binding.connectionStatus.setText(text);
        binding.connectionStatus.setTextColor(ContextCompat.getColor(this, colorResource));
    }

    @Override
    protected void onDestroy() {
        if (activeCall != null) {
            activeCall.cancel();
        }
        super.onDestroy();
    }
}
