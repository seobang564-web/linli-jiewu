package com.linlijiewu.app.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.snackbar.Snackbar;
import com.linlijiewu.app.R;
import com.linlijiewu.app.databinding.ActivityAuthBinding;
import com.linlijiewu.app.ui.common.LoadingDialog;
import com.linlijiewu.app.ui.common.Motion;
import com.linlijiewu.app.ui.common.Session;
import com.linlijiewu.app.ui.common.Ui;
import com.linlijiewu.app.ui.main.HomeActivity;
import com.linlijiewu.app.BuildConfig;
import com.linlijiewu.app.data.remote.ApiClient;
import com.linlijiewu.app.data.remote.ApiCallback;
import com.linlijiewu.app.data.remote.ApiResponse;
import com.linlijiewu.app.data.remote.AuthData;
import retrofit2.Call;

/**
 * 启动页兼登录注册页。已登录或已选择游客时直接进入首页。
 * 注册、登录由服务端验证，成功后保存 Token。
 */
public class AuthActivity extends AppCompatActivity {
    private static final String USERNAME_PATTERN = "[A-Za-z0-9_]{4,20}";

    private ActivityAuthBinding binding;
    private boolean registerMode = true;
    private Call<ApiResponse<AuthData>> activeCall;
    private LoadingDialog loading;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        boolean relogin = getIntent().getBooleanExtra("relogin", false);
        if (!relogin && Session.hasEntered(this)) {
            enterHome(false);
            return;
        }
        EdgeToEdge.enable(this);
        binding = ActivityAuthBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        Ui.padTop(binding.content);
        Ui.padBottom(binding.content);

        binding.tabRegister.setOnClickListener(v -> setMode(true));
        binding.tabLogin.setOnClickListener(v -> setMode(false));
        binding.submit.setOnClickListener(v -> submit());
        binding.guest.setOnClickListener(v -> {
            Session.guest(this);
            enterHome(true);
        });
        setMode(!relogin);
        Motion.stagger(binding.content, 80);
    }

    private void setMode(boolean register) {
        registerMode = register;
        style(binding.tabRegister, register);
        style(binding.tabLogin, !register);
        binding.registerFields.setVisibility(register ? View.VISIBLE : View.GONE);
        binding.submit.setText(register ? R.string.auth_submit_register : R.string.auth_submit_login);
    }

    private void style(TextView tab, boolean on) {
        tab.setSelected(on);
        tab.setTextSize(on ? 20 : 17);
        tab.setTypeface(null, on ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
    }

    private void submit() {
        String username = binding.username.getText().toString().trim();
        String password = binding.password.getText().toString();
        String nickname = binding.nickname.getText().toString().trim();
        if (!username.matches(USERNAME_PATTERN)) {
            error(binding.username, R.string.auth_err_username);
            return;
        }
        if (password.length() < 6 || password.length() > 32) {
            error(binding.password, R.string.auth_err_password);
            return;
        }
        if (registerMode && (nickname.isEmpty() || nickname.length() > 32)) {
            error(binding.nickname, R.string.auth_err_nickname);
            return;
        }
        if (!binding.agree.isChecked()) {
            Snackbar.make(binding.getRoot(), R.string.auth_err_agree, Snackbar.LENGTH_SHORT).show();
            return;
        }
        if (password.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72) {
            error(binding.password, R.string.auth_password_bytes);
            return;
        }
        setBusy(true);
        loading = new LoadingDialog(this);
        loading.show(registerMode ? R.string.loading_register : R.string.loading_login);
        var api = ApiClient.create(BuildConfig.SERVER_BASE_URL, () -> null);
        activeCall = registerMode ? api.register(new AuthData.Register(username, password, nickname,
                binding.contact.getText().toString().trim()))
                : api.login(new AuthData.Login(username, password));
        activeCall.enqueue(new ApiCallback<>(this, data -> {
            if (data.token == null || data.token.isEmpty() || data.user == null || data.user.id <= 0
                    || data.expiresIn <= 0 || data.user.username == null || data.user.nickname == null) {
                Snackbar.make(binding.getRoot(), R.string.network_response_failed, Snackbar.LENGTH_LONG).show();
                return;
            }
            Session.login(this, data);
            enterHome(true);
        }, message -> Snackbar.make(binding.getRoot(), message, Snackbar.LENGTH_LONG).show(), () -> {
            setBusy(false);
            loading.dismiss();
        }));
    }

    private void setBusy(boolean busy) {
        binding.submit.setEnabled(!busy);
        binding.guest.setEnabled(!busy);
        binding.tabLogin.setEnabled(!busy);
        binding.tabRegister.setEnabled(!busy);
        binding.username.setEnabled(!busy);
        binding.password.setEnabled(!busy);
        binding.nickname.setEnabled(!busy);
        binding.contact.setEnabled(!busy);
    }

    @Override
    protected void onDestroy() {
        if (activeCall != null) { activeCall.cancel(); }
        if (loading != null) { loading.dismiss(); }
        super.onDestroy();
    }

    private void error(TextView field, int message) {
        field.setError(getString(message));
        field.requestFocus();
        if (Motion.enabled(this)) {
            binding.formCard.animate().translationX(Ui.dp(this, 8)).setDuration(60)
                    .withEndAction(() -> binding.formCard.animate().translationX(0)
                            .setInterpolator(Motion.spring(this)).setDuration(360).start()).start();
        }
    }

    private void enterHome(boolean animate) {
        startActivity(new Intent(this, HomeActivity.class));
        if (animate) {
            overridePendingTransition(R.anim.tab_enter, R.anim.tab_exit);
        } else {
            overridePendingTransition(0, 0);
        }
        finish();
    }
}
