package com.linlijiewu.app.ui.common;

import android.content.Context;

/** Last actual GCJ-02 location shared by the map and category search; no default invented coordinates. */
public final class SearchLocation {
    private SearchLocation() { }
    private static android.content.SharedPreferences prefs(Context c) { return c.getApplicationContext().getSharedPreferences("search_location", Context.MODE_PRIVATE); }
    public static boolean available(Context c) { return prefs(c).contains("lat"); }
    public static double lat(Context c) { return Double.longBitsToDouble(prefs(c).getLong("lat", 0)); }
    public static double lng(Context c) { return Double.longBitsToDouble(prefs(c).getLong("lng", 0)); }
    public static int radius(Context c) { return prefs(c).getInt("radius", 1000); }
    public static void save(Context c, double lat, double lng, int radius) {
        prefs(c).edit().putLong("lat", Double.doubleToLongBits(lat)).putLong("lng", Double.doubleToLongBits(lng)).putInt("radius", radius).apply();
    }
}
