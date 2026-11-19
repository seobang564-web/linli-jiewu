package com.linlijiewu.app.ui.main;

import android.os.Bundle;
import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.linlijiewu.app.R;
import com.linlijiewu.app.databinding.ActivityHomeBinding;

/**
 * 应用主页：底部导航（发现 / 发布 / 借还 / 我的）+ ViewPager2 页面容器。
 * 点按导航与左右滑动均可切换，两边状态互相同步。
 */
public class HomeActivity extends AppCompatActivity {

    private ActivityHomeBinding binding;
    private boolean syncingSelection;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityHomeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (view, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            view.setPadding(bars.left, bars.top, bars.right, 0);
            return insets;
        });

        binding.homePager.setAdapter(new HomePagerAdapter(this));
        binding.homePager.setOffscreenPageLimit(HomePagerAdapter.PAGE_COUNT - 1);
        binding.homePager.setUserInputEnabled(true);

        binding.homeNav.setOnItemSelectedListener(item -> {
            int position = HomePagerAdapter.positionFor(item.getItemId());
            if (position >= 0) {
                syncPagerTo(position);
            }
            return true;
        });
        binding.homePager.registerOnPageChangeCallback(new androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                if (syncingSelection) {
                    return;
                }
                syncingSelection = true;
                binding.homeNav.setSelectedItemId(HomePagerAdapter.itemIdFor(position));
                syncingSelection = false;
            }
        });
    }

    private void syncPagerTo(int position) {
        if (binding.homePager.getCurrentItem() != position) {
            syncingSelection = true;
            binding.homePager.setCurrentItem(position, false);
            syncingSelection = false;
        }
    }

    @Override
    public void onBackPressed() {
        // 非发现页时先回到发现页，再按一次退出，避免误触退出。
        if (binding != null && binding.homePager.getCurrentItem() != 0) {
            binding.homeNav.setSelectedItemId(HomePagerAdapter.itemIdFor(0));
            return;
        }
        super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        binding = null;
        super.onDestroy();
    }
}
