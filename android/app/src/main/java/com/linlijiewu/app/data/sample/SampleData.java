package com.linlijiewu.app.data.sample;

import com.linlijiewu.app.R;
import com.linlijiewu.app.data.model.BorrowStatus;
import com.linlijiewu.app.data.model.Category;
import com.linlijiewu.app.data.model.ChatMessage;
import com.linlijiewu.app.data.model.Conversation;
import com.linlijiewu.app.data.model.Item;
import com.linlijiewu.app.data.model.ItemStatus;
import com.linlijiewu.app.data.model.Order;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 界面原型阶段的示例数据，与 docs/ui-prototype.html 一致。
 * 物品、借用、聊天接口上线后，由 repository 返回真实数据替换本类。
 */
public final class SampleData {
    public static final long DEMO_ITEM_ID = 12;

    public static final List<Item> ITEMS = Collections.unmodifiableList(Arrays.asList(
            new Item(6, "《算法导论》第三版", Category.BOOK, 75, "楠苑综合服务中心", ItemStatus.AVAILABLE,
                    R.color.tone_sand, "有少量笔记，课设查资料够用", "一诺", -40, 63),
            new Item(4, "斯伯丁 7 号篮球", Category.SPORTS, 128, "楠苑篮球场", ItemStatus.AVAILABLE,
                    R.color.tone_peach, "气很足，晚上打球可借", "子涵", -110, -65),
            new Item(2, "博世电钻套装", Category.TOOL, 210, "楠苑 3 栋楼下", ItemStatus.AVAILABLE,
                    R.color.tone_sky, "含 12 个钻头，装书架挂钩都行", "骐泽", 150, 147),
            new Item(3, "24 寸行李箱", Category.LUGGAGE, 352, "梓苑食堂门口", ItemStatus.RESERVED,
                    R.color.tone_rose, "万向轮，放假回家用", "一诺", -270, 225),
            new Item(DEMO_ITEM_ID, "双人帐篷（带防潮垫）", Category.CAMPING, 486, "东门快递站旁", ItemStatus.AVAILABLE,
                    R.color.tone_olive, "牧高笛双人帐，搭过三次，防潮垫一起借。周末去滇池边、捞鱼河露营都合适。还的时候晾干就行。",
                    "舒煜", 400, -276),
            new Item(5, "富士拍立得 mini 12", Category.ELECTRONICS, 640, "图书馆北门", ItemStatus.LENT,
                    R.color.tone_sky, "相纸需自备", "召然", -560, -310),
            new Item(7, "铝合金折叠梯", Category.TOOL, 820, "梓苑 1 栋", ItemStatus.AVAILABLE,
                    R.color.tone_sand, "1.5 米，换灯泡用", "小林", 640, 512)));

    public static final List<Order> BORROWER_ORDERS = Arrays.asList(
            new Order(1024, DEMO_ITEM_ID, false, "物主 舒煜", BorrowStatus.ACCEPTED, "2026-11-19 20:12 · 东门快递站旁",
                    "约定 11-22 18:00 前归还", false, "取消借用", "去交接点"),
            new Order(1019, 4, false, "物主 子涵", BorrowStatus.BORROWING, "2026-11-18 19:30 · 楠苑篮球场",
                    "还剩 1 天 3 小时", false, "联系", "申请归还"),
            new Order(1027, 2, false, "物主 骐泽", BorrowStatus.PENDING, "2026-11-19 09:02 · 楠苑 3 栋楼下",
                    "11:42:08 后物主未确认将自动取消", true, "取消申请", null),
            new Order(988, 5, false, "物主 召然", BorrowStatus.COMPLETED, "2026-10-28 15:17 · 图书馆北门",
                    "已于 10-30 归还", false, "评价一下", "再借一次"),
            new Order(975, 3, false, "物主 一诺", BorrowStatus.CANCELLED, "2026-10-20 11:20 · 梓苑食堂门口",
                    "超时未确认，已自动取消", false, null, null));

    public static final List<Order> OWNER_ORDERS = Arrays.asList(
            new Order(1031, 7, true, "借用者 小林", BorrowStatus.PENDING, "2026-11-19 19:40 · 梓苑 1 栋",
                    "备注：宿舍换灯泡，明早就还", true, "拒绝", "接受"),
            new Order(1012, 5, true, "借用者 召然", BorrowStatus.RETURNING, "2026-11-15 13:10 · 图书馆北门",
                    "对方已申请归还，请检查物品", false, "验收不通过", "确认归还"),
            new Order(1025, DEMO_ITEM_ID, true, "借用者 骐泽", BorrowStatus.ACCEPTED, "2026-11-19 20:12 · 东门快递站旁",
                    "当面交给对方后点确认", false, null, "确认交接"));

    public static final List<Conversation> CONVERSATIONS = Arrays.asList(
            new Conversation(8, "舒煜", 0xFF6A8A14, DEMO_ITEM_ID, "明天下午 5 点东门快递站见？", "20:31", 2),
            new Conversation(9, "小林", 0xFFC2613E, 7, "好的，明早 8 点前还你", "19:52", 1),
            new Conversation(10, "召然", 0xFF5C7EA8, 5, "相机已经放回图书馆北门了", "昨天", 0),
            new Conversation(11, "骐泽", 0xFF8A7A4A, 2, "钻头在盒子第二层", "周一", 0));

    public static final String[][] PICKUP_POIS = {
            {"东门快递站旁", "云南大学呈贡校区东门北侧", "120"},
            {"楠苑 3 栋楼下", "楠苑学生公寓 3 栋", "260"},
            {"楠苑综合服务中心", "楠苑综合服务中心大楼一层", "310"},
            {"图书馆北门", "呈贡校区图书馆", "540"}};

    private SampleData() {
    }

    public static Item item(long id) {
        for (Item item : ITEMS) {
            if (item.id == id) {
                return item;
            }
        }
        return ITEMS.get(0);
    }

    public static List<Item> itemsWithin(int radiusM) {
        List<Item> result = new ArrayList<>();
        for (Item item : ITEMS) {
            if (item.distanceM <= radiusM) {
                result.add(item);
            }
        }
        return result;
    }

    public static List<Item> itemsIn(Category category) {
        if (category == null) {
            return ITEMS;
        }
        List<Item> result = new ArrayList<>();
        for (Item item : ITEMS) {
            if (item.category == category) {
                result.add(item);
            }
        }
        return result;
    }

    public static List<ChatMessage> demoChat() {
        return new ArrayList<>(Arrays.asList(
                new ChatMessage(ChatMessage.Type.TIME, "今天 20:12"),
                new ChatMessage(ChatMessage.Type.MINE, "你好，帐篷周六能借吗？想去捞鱼河露营"),
                new ChatMessage(ChatMessage.Type.EVENT, "你提交了借用申请 · 待确认"),
                new ChatMessage(ChatMessage.Type.PEER, "可以的，防潮垫也一起给你"),
                new ChatMessage(ChatMessage.Type.EVENT, "舒煜 接受了申请 · 待交接"),
                new ChatMessage(ChatMessage.Type.PEER, "明天下午 5 点东门快递站见？"),
                new ChatMessage(ChatMessage.Type.MINE, "好的，到了给你发消息")));
    }
}
