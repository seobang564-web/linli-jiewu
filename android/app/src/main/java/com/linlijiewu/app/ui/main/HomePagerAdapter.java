package com.linlijiewu.app.ui.main;

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import com.linlijiewu.app.R;
import com.linlijiewu.app.ui.borrow.BorrowFragment;
import com.linlijiewu.app.ui.discover.DiscoverFragment;
import com.linlijiewu.app.ui.mine.MineFragment;
import com.linlijiewu.app.ui.publish.PublishFragment;

/**
 * 主页四个 Tab 的页面适配器。Tab 顺序固定：发现、发布、借还、我的。
 */
public class HomePagerAdapter extends FragmentStateAdapter {

    public static final int PAGE_COUNT = 4;

    private static final int[] ITEM_IDS = {
            R.id.nav_discover,
            R.id.nav_publish,
            R.id.nav_borrow,
            R.id.nav_mine,
    };

    public HomePagerAdapter(@NonNull HomeActivity activity) {
        super(activity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 1:
                return new PublishFragment();
            case 2:
                return new BorrowFragment();
            case 3:
                return new MineFragment();
            case 0:
            default:
                return new DiscoverFragment();
        }
    }

    @Override
    public int getItemCount() {
        return PAGE_COUNT;
    }

    public static int positionFor(@IdRes int itemId) {
        for (int i = 0; i < ITEM_IDS.length; i++) {
            if (ITEM_IDS[i] == itemId) {
                return i;
            }
        }
        return -1;
    }

    @IdRes
    public static int itemIdFor(int position) {
        return ITEM_IDS[Math.max(0, Math.min(position, ITEM_IDS.length - 1))];
    }
}
