package com.example.emanager.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.emanager.R;
import com.example.emanager.models.GroupExpense;

import java.util.List;

public class GroupListAdapter extends RecyclerView.Adapter<GroupListAdapter.GroupViewHolder> {

    private Context context;
    private List<GroupExpense> groupList;
    private OnGroupItemClickListener listener;

    public GroupListAdapter(Context context, List<GroupExpense> groupList, OnGroupItemClickListener listener) {
        this.context = context;
        this.groupList = groupList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public GroupViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_group, parent, false);
        return new GroupViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull GroupViewHolder holder, int position) {
        GroupExpense groupExpense = groupList.get(position);
        holder.groupNameTextView.setText(groupExpense.getGroupName());

        holder.leaveGroupImageView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onLeaveGroupClick(groupExpense);
            }
        });
    }

    @Override
    public int getItemCount() {
        return groupList.size();
    }

    public interface OnGroupItemClickListener {
        void onLeaveGroupClick(GroupExpense groupExpense);
    }

    public static class GroupViewHolder extends RecyclerView.ViewHolder {
        TextView groupNameTextView;
        ImageView leaveGroupImageView;

        public GroupViewHolder(@NonNull View itemView) {
            super(itemView);
            groupNameTextView = itemView.findViewById(R.id.groupNameTextView);
            leaveGroupImageView = itemView.findViewById(R.id.leaveGroupImageView);
        }
    }
}
