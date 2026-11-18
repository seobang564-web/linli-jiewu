package com.linlijiewu.app.ui.discover;

import android.os.Bundle;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import com.linlijiewu.app.R;

/**
 * 发现页的临时宿主。底部导航接入后，直接把 {@link DiscoverFragment} 放进导航，可删除本类。
 */
public class DiscoverActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_discover);
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.discoverContainer, new DiscoverFragment())
                    .commit();
        }
    }
}
