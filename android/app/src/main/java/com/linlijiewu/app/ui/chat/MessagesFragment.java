package com.linlijiewu.app.ui.chat;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.linlijiewu.app.data.model.Conversation;
import com.linlijiewu.app.data.model.Item;
import com.linlijiewu.app.data.sample.SampleData;
import com.linlijiewu.app.databinding.FragmentMessagesBinding;
import com.linlijiewu.app.databinding.ItemConversationBinding;
import com.linlijiewu.app.ui.common.Motion;
import com.linlijiewu.app.ui.common.Ui;
import com.linlijiewu.app.ui.main.HomeActivity;

/** 消息列表（GET /conversations）：每个会话以“物品 + 对方”区分，头像右下叠物品缩略图。 */
public class MessagesFragment extends Fragment {
    private FragmentMessagesBinding binding;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle state) {
        binding = FragmentMessagesBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle state) {
        Ui.padTop(binding.content);
        binding.notice.setOnClickListener(v -> startActivity(HomeActivity.intent(requireContext(), HomeActivity.TAB_ORDERS)));
        LayoutInflater inflater = getLayoutInflater();
        for (int i = 0; i < SampleData.CONVERSATIONS.size(); i++) {
            Conversation c = SampleData.CONVERSATIONS.get(i);
            Item item = SampleData.item(c.itemId);
            ItemConversationBinding row = ItemConversationBinding.inflate(inflater, binding.list, false);
            row.avatar.setText(c.peerName.substring(0, 1));
            row.avatar.setBackgroundTintList(ColorStateList.valueOf(c.avatarColor));
            Ui.thumb(row.itemThumb, item, 8, 0.12f);
            row.itemThumb.setForeground(requireContext().getDrawable(com.linlijiewu.app.R.drawable.bg_thumb_ring));
            row.name.setText(c.peerName);
            row.itemTag.setText(item.title);
            row.time.setText(c.time);
            row.last.setText(c.lastMessage);
            row.unread.setText(String.valueOf(c.unread));
            row.unread.setVisibility(c.unread > 0 ? View.VISIBLE : View.GONE);
            if (i > 0) {
                View divider = new View(requireContext());
                divider.setBackgroundColor(0xFFECECE6);
                ViewGroup.MarginLayoutParams lp = new ViewGroup.MarginLayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1);
                lp.setMarginStart(Ui.dp(requireContext(), 16));
                lp.setMarginEnd(Ui.dp(requireContext(), 16));
                binding.list.addView(divider, lp);
            }
            row.getRoot().setOnClickListener(v -> {
                row.unread.setVisibility(View.GONE);
                startActivity(ChatActivity.intent(requireContext(), c.itemId));
            });
            binding.list.addView(row.getRoot());
        }
        Motion.stagger(binding.content, 100);
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (!hidden && binding != null) {
            Motion.stagger(binding.content, 40);
        }
    }

    @Override
    public void onDestroyView() {
        binding = null;
        super.onDestroyView();
    }
}
