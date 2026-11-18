package com.linlijiewu.app.util;

import android.content.Context;
import android.content.SharedPreferences;
import com.amap.api.location.AMapLocationClient;
import com.amap.api.maps.MapsInitializer;
import com.amap.api.services.core.ServiceSettings;

/**
 * 高德 SDK 隐私合规状态。
 * 高德要求在创建 MapView、定位或搜索对象之前，先告知隐私政策并同步用户是否同意，
 * 否则 SDK 不工作。同意结果保存在本地，仅同意后才初始化地图。
 */
public final class AmapPrivacy {
    private static final String PREFS = "amap_privacy";
    private static final String KEY_AGREED = "agreed";

    private AmapPrivacy() {
    }

    public static boolean isAgreed(Context context) {
        return prefs(context).getBoolean(KEY_AGREED, false);
    }

    /** 用户在隐私弹窗中点击“同意”后调用。 */
    public static void agree(Context context) {
        prefs(context).edit().putBoolean(KEY_AGREED, true).apply();
        syncToSdk(context);
    }

    /**
     * 把隐私弹窗已展示、用户已同意的状态同步给地图、定位、搜索三个 SDK。
     * 每次进程启动后、首次使用高德 SDK 前都要调用。
     */
    public static void syncToSdk(Context context) {
        Context app = context.getApplicationContext();
        boolean agreed = isAgreed(app);
        MapsInitializer.updatePrivacyShow(app, true, true);
        MapsInitializer.updatePrivacyAgree(app, agreed);
        AMapLocationClient.updatePrivacyShow(app, true, true);
        AMapLocationClient.updatePrivacyAgree(app, agreed);
        ServiceSettings.updatePrivacyShow(app, true, true);
        ServiceSettings.updatePrivacyAgree(app, agreed);
    }

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }
}
