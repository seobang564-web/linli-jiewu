package com.linlijiewu.app.util;

import android.content.Context;
import androidx.annotation.Nullable;
import com.amap.api.maps.AMapUtils;
import com.amap.api.maps.model.LatLng;
import com.amap.api.services.core.AMapException;
import com.amap.api.services.core.LatLonPoint;
import com.amap.api.services.core.PoiItem;
import com.amap.api.services.core.PoiItemV2;
import com.amap.api.services.geocoder.GeocodeResult;
import com.amap.api.services.geocoder.GeocodeSearch;
import com.amap.api.services.geocoder.RegeocodeAddress;
import com.amap.api.services.geocoder.RegeocodeQuery;
import com.amap.api.services.geocoder.RegeocodeResult;
import com.amap.api.services.poisearch.PoiResultV2;
import com.amap.api.services.poisearch.PoiSearchV2;
import com.amap.api.services.poisearch.VisualSearchResult;
import java.util.ArrayList;
import java.util.List;

/**
 * 高德地点服务的薄封装：逆地理（坐标 → 地址与周边地点）和关键词地点搜索。
 * 调用前必须已同意隐私政策（见 {@link AmapPrivacy}）。回调在主线程执行。
 * 每次新请求会让旧请求的回调失效，调用方不必自己判断结果是否过期。
 */
public final class PlaceSearch {
    /** 高德搜索服务成功返回码。 */
    public static final int CODE_OK = AMapException.CODE_AMAP_SUCCESS;

    public static final class Place {
        public final String name;
        public final String address;
        public final double lat;
        public final double lng;
        /** 距离查询中心的直线距离（米）。 */
        public final int distanceM;

        Place(String name, String address, double lat, double lng, int distanceM) {
            this.name = name;
            this.address = address;
            this.lat = lat;
            this.lng = lng;
            this.distanceM = distanceM;
        }
    }

    public static final class Address {
        public final String city;
        public final String formatted;
        /** 用于标题的短名称：优先 AOI（如“云南大学呈贡校区”），其次街道/乡镇。 */
        public final String shortName;
        public final List<Place> nearby;

        Address(String city, String formatted, String shortName, List<Place> nearby) {
            this.city = city;
            this.formatted = formatted;
            this.shortName = shortName;
            this.nearby = nearby;
        }
    }

    public interface Callback<T> {
        void onResult(@Nullable T result, int code);
    }

    private final Context context;
    private int regeoSeq;
    private int poiSeq;

    public PlaceSearch(Context context) {
        this.context = context.getApplicationContext();
    }

    /** 逆地理：返回地址与半径内的周边地点（按距离排序）。 */
    public void reverse(LatLng point, int radiusM, Callback<Address> callback) {
        int seq = ++regeoSeq;
        // 每次新建：监听器挂在实例上，复用实例时旧请求的结果会被新监听器当成新结果
        GeocodeSearch geocoder;
        try {
            AmapPrivacy.syncToSdk(context);
            geocoder = new GeocodeSearch(context);
        } catch (AMapException e) {
            callback.onResult(null, e.getErrorCode());
            return;
        }
        geocoder.setOnGeocodeSearchListener(new GeocodeSearch.OnGeocodeSearchListener() {
            @Override
            public void onRegeocodeSearched(RegeocodeResult result, int code) {
                if (seq != regeoSeq) {
                    return;
                }
                if (code != CODE_OK || result == null || result.getRegeocodeAddress() == null) {
                    callback.onResult(null, code == CODE_OK ? -1 : code);
                    return;
                }
                callback.onResult(toAddress(result.getRegeocodeAddress(), point), code);
            }

            @Override
            public void onGeocodeSearched(GeocodeResult result, int code) {
            }
        });
        RegeocodeQuery query = new RegeocodeQuery(new LatLonPoint(point.latitude, point.longitude), radiusM, GeocodeSearch.AMAP);
        query.setExtensions(GeocodeSearch.EXTENSIONS_ALL);
        geocoder.getFromLocationAsyn(query);
    }

    /** 关键词搜索，优先返回离 near 近的结果；city 为空时全国范围。 */
    public void keyword(String keyword, @Nullable String city, LatLng near, Callback<List<Place>> callback) {
        int seq = ++poiSeq;
        PoiSearchV2.Query query = new PoiSearchV2.Query(keyword, "", city == null ? "" : city);
        query.setPageSize(20);
        query.setPageNum(1);
        query.setCityLimit(city != null && !city.isEmpty());
        query.setLocation(new LatLonPoint(near.latitude, near.longitude));
        query.setDistanceSort(true);
        PoiSearchV2 search;
        try {
            AmapPrivacy.syncToSdk(context);
            search = new PoiSearchV2(context, query);
        } catch (AMapException e) {
            callback.onResult(null, e.getErrorCode());
            return;
        }
        search.setOnPoiSearchListener(new PoiSearchV2.OnPoiSearchListener() {
            @Override
            public void onPoiSearched(PoiResultV2 result, int code) {
                if (seq != poiSeq) {
                    return;
                }
                if (code != CODE_OK || result == null) {
                    callback.onResult(null, code == CODE_OK ? -1 : code);
                    return;
                }
                List<Place> places = new ArrayList<>();
                if (result.getPois() != null) {
                    for (PoiItemV2 poi : result.getPois()) {
                        LatLonPoint p = poi.getLatLonPoint();
                        if (p == null) {
                            continue;
                        }
                        places.add(new Place(poi.getTitle(), poi.getSnippet(), p.getLatitude(), p.getLongitude(),
                                distance(near, p)));
                    }
                }
                callback.onResult(places, code);
            }

            @Override
            public void onPoiItemSearched(PoiItemV2 item, int code) {
            }

            @Override
            public void onVisualSearched(VisualSearchResult result, int code) {
            }
        });
        search.searchPOIAsyn();
    }

    /** 让尚未返回的请求全部失效，页面销毁时调用。 */
    public void cancel() {
        regeoSeq++;
        poiSeq++;
    }

    private static Address toAddress(RegeocodeAddress a, LatLng center) {
        List<Place> nearby = new ArrayList<>();
        if (a.getPois() != null) {
            for (PoiItem poi : a.getPois()) {
                LatLonPoint p = poi.getLatLonPoint();
                if (p == null) {
                    continue;
                }
                nearby.add(new Place(poi.getTitle(), poi.getSnippet(), p.getLatitude(), p.getLongitude(), distance(center, p)));
            }
        }
        nearby.sort((x, y) -> Integer.compare(x.distanceM, y.distanceM));
        String city = notEmpty(a.getCity()) ? a.getCity() : a.getProvince(); // 直辖市 city 为空
        String shortName = a.getAois() != null && !a.getAois().isEmpty() && notEmpty(a.getAois().get(0).getAoiName())
                ? a.getAois().get(0).getAoiName()
                : notEmpty(a.getTownship()) ? a.getTownship() : a.getDistrict();
        return new Address(city, a.getFormatAddress(), shortName, nearby);
    }

    private static int distance(LatLng from, LatLonPoint to) {
        return Math.round(AMapUtils.calculateLineDistance(from, new LatLng(to.getLatitude(), to.getLongitude())));
    }

    private static boolean notEmpty(@Nullable String s) {
        return s != null && !s.trim().isEmpty();
    }
}
