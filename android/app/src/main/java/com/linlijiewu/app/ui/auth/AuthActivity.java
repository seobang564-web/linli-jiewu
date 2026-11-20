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

/**
 * 启动页兼登录注册页。已登录或已选择游客时直接进入首页。
 * 校验规则与接口文档一致；/auth/register、/auth/login 上线后替换 {@link #submit()} 里的模拟请求。
 */
public class AuthActivity extends AppCompatActivity {
    private static final String USERNAME_PATTERN = "[A-Za-z0-9_]{4,20}";

    private ActivityAuthBinding binding;
    private boolean registerMode = true;

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
        if (registerMode && nickname.isEmpty()) {
            error(binding.nickname, R.string.auth_err_nickname);
            return;
        }
        if (!binding.agree.isChecked()) {
            Snackbar.make(binding.getRoot(), R.string.auth_err_agree, Snackbar.LENGTH_SHORT).show();
            return;
        }
        String shownName = registerMode ? nickname : username;
        LoadingDialog.run(this, registerMode ? R.string.loading_register : R.string.loading_login, () -> {
            Session.login(this, username, shownName);
            enterHome(true);
        });
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
