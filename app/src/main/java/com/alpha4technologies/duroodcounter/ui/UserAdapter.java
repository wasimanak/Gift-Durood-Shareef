package com.alpha4technologies.duroodcounter.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.alpha4technologies.duroodcounter.R;
import com.alpha4technologies.duroodcounter.model.User;

import java.text.NumberFormat;
import java.util.Locale;

public class UserAdapter extends ListAdapter<User, UserAdapter.UserViewHolder> {

    public UserAdapter() {
        super(DIFF_CALLBACK);
    }

    private static final DiffUtil.ItemCallback<User> DIFF_CALLBACK = new DiffUtil.ItemCallback<User>() {
        @Override
        public boolean areItemsTheSame(@NonNull User oldItem, @NonNull User newItem) {
            return oldItem.getUserId() != null && oldItem.getUserId().equals(newItem.getUserId());
        }

        @Override
        public boolean areContentsTheSame(@NonNull User oldItem, @NonNull User newItem) {
            return oldItem.getTotalCount() == newItem.getTotalCount() &&
                    oldItem.getTodayCount() == newItem.getTodayCount() &&
                    oldItem.isGuest() == newItem.isGuest() &&
                    safeStringEquals(oldItem.getFullName(), newItem.getFullName()) &&
                    safeStringEquals(oldItem.getCity(), newItem.getCity());
        }

        private boolean safeStringEquals(String s1, String s2) {
            if (s1 == null && s2 == null) return true;
            if (s1 == null || s2 == null) return false;
            return s1.equals(s2);
        }
    };

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_user_card, parent, false);
        return new UserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        User user = getItem(position);
        holder.bind(user, position + 1);
    }

    static class UserViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvRank;
        private final TextView tvFullName;
        private final TextView tvUserBadge;
        private final TextView tvCityCountry;
        private final TextView tvUserTotalCount;
        private final TextView tvUserTodayCount;

        public UserViewHolder(@NonNull View itemView) {
            super(itemView);
            tvRank = itemView.findViewById(R.id.tvRank);
            tvFullName = itemView.findViewById(R.id.tvFullName);
            tvUserBadge = itemView.findViewById(R.id.tvUserBadge);
            tvCityCountry = itemView.findViewById(R.id.tvCityCountry);
            tvUserTotalCount = itemView.findViewById(R.id.tvUserTotalCount);
            tvUserTodayCount = itemView.findViewById(R.id.tvUserTodayCount);
        }

        public void bind(User user, int rank) {
            NumberFormat formatter = NumberFormat.getInstance(Locale.US);

            tvRank.setText(String.format(Locale.US, "#%d", rank));
            tvFullName.setText(user.getFullName() != null ? user.getFullName() : "Unknown User");

            String locationStr = "";
            if (user.getCity() != null && !user.getCity().isEmpty()) {
                locationStr += user.getCity();
            }
            if (user.getCountry() != null && !user.getCountry().isEmpty()) {
                if (!locationStr.isEmpty()) locationStr += ", ";
                locationStr += user.getCountry();
            }
            if (locationStr.isEmpty()) locationStr = "Location N/A";
            tvCityCountry.setText(locationStr);

            tvUserTotalCount.setText(formatter.format(user.getTotalCount()));
            tvUserTodayCount.setText(formatter.format(user.getTodayCount()));

            if (user.isGuest()) {
                tvUserBadge.setText(R.string.guest_badge);
                tvUserBadge.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.chip_text_guest));
                tvUserBadge.setBackgroundColor(ContextCompat.getColor(itemView.getContext(), R.color.chip_bg_guest));
            } else {
                tvUserBadge.setText(R.string.registered_badge);
                tvUserBadge.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.chip_text_registered));
                tvUserBadge.setBackgroundColor(ContextCompat.getColor(itemView.getContext(), R.color.chip_bg_registered));
            }
        }
    }
}
