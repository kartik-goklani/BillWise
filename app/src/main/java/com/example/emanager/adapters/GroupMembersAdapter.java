package com.example.emanager.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.emanager.R;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class GroupMembersAdapter extends RecyclerView.Adapter<GroupMembersAdapter.MemberViewHolder> {

    private List<String> membersList;
    private Set<String> selectedMembers = new HashSet<>();

    public GroupMembersAdapter(List<String> membersList) {
        this.membersList = membersList;
    }

    public Set<String> getSelectedMembers() {
        return selectedMembers;
    }

    @NonNull
    @Override
    public MemberViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_member_checkbox, parent, false);
        return new MemberViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MemberViewHolder holder, int position) {
        String memberName = membersList.get(position);
        holder.memberNameTextView.setText(memberName);

        holder.memberCheckBox.setOnCheckedChangeListener(null); // prevent old listeners
        holder.memberCheckBox.setChecked(selectedMembers.contains(memberName));

        holder.memberCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                selectedMembers.add(memberName);
            } else {
                selectedMembers.remove(memberName);
            }
        });
    }

    @Override
    public int getItemCount() {
        return membersList.size();
    }

    public static class MemberViewHolder extends RecyclerView.ViewHolder {
        TextView memberNameTextView;
        CheckBox memberCheckBox;

        public MemberViewHolder(@NonNull View itemView) {
            super(itemView);
            memberNameTextView = itemView.findViewById(R.id.memberNameTextView);
            memberCheckBox = itemView.findViewById(R.id.memberCheckBox);
        }
    }
}

