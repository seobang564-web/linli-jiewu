package com.linlijiewu.app.util;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.Toast;
import com.linlijiewu.app.R;
import java.util.Locale;

/**
 * 步行导航到交接点。坐标为 GCJ-02，优先调起高德地图 App；
 * 未安装时打开高德网页版导航（同样使用 GCJ-02，不需要做坐标转换）。
 */
public final class MapNavigator {
    private static final String AMAP_PACKAGE = "com.autonavi.minimap";

    private MapNavigator() {
    }

    public static void walkTo(Context context, double lat, double lng, String name) {
        String label = name == null || name.trim().isEmpty() ? context.getString(R.string.nav_default_name) : name.trim();
        Intent app = new Intent(Intent.ACTION_VIEW, Uri.parse(appUri(context, lat, lng, label)))
                .setPackage(AMAP_PACKAGE)
                .addCategory(Intent.CATEGORY_DEFAULT);
        try {
            context.startActivity(app);
            return;
        } catch (ActivityNotFoundException ignored) {
            // 未安装高德地图，改用网页版
        }
        try {
            context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(webUri(lat, lng, label))));
        } catch (ActivityNotFoundException e) {
            Toast.makeText(context, R.string.nav_unavailable, Toast.LENGTH_LONG).show();
        }
    }

    /** 高德 App 路线规划：dev=0 表示坐标已是 GCJ-02，t=2 为步行。 */
    static String appUri(Context context, double lat, double lng, String name) {
        return String.format(Locale.ROOT, "amapuri://route/plan/?sourceApplication=%s&dlat=%.6f&dlon=%.6f&dname=%s&dev=0&t=2",
                Uri.encode(context.getString(R.string.app_name)), lat, lng, Uri.encode(name));
    }

    /** 高德网页版导航：to 参数为 “经度,纬度,名称”。 */
    static String webUri(double lat, double lng, String name) {
        return String.format(Locale.ROOT, "https://uri.amap.com/navigation?to=%.6f,%.6f,%s&mode=walk&coordinate=gaode&callnative=1",
                lng, lat, Uri.encode(name));
    }
}
