package com.example.emanager.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.emanager.R;
import com.example.emanager.models.GroupMember;

import java.util.ArrayList;
import java.util.List;

public class MemberSelectionAdapter extends RecyclerView.Adapter<MemberSelectionAdapter.ViewHolder> {

    private List<GroupMember> membersList;

    public MemberSelectionAdapter(List<GroupMember> membersList) {
        this.membersList = membersList;
    }

    public List<GroupMember> getSelectedMembers() {
        List<GroupMember> selectedMembers = new ArrayList<>();
        for (GroupMember member : membersList) {
            if (member.isSelected()) {
                selectedMembers.add(member);
            }
        }
        return selectedMembers;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_member_checkbox, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        GroupMember member = membersList.get(position);
        holder.memberCheckBox.setText(member.getName());
        holder.memberCheckBox.setChecked(member.isSelected());

        holder.memberCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            member.setSelected(isChecked);
        });
    }

    @Override
    public int getItemCount() {
        return membersList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        CheckBox memberCheckBox;

        public ViewHolder(View itemView) {
            super(itemView);
            memberCheckBox = itemView.findViewById(R.id.memberCheckBox);
        }
    }
}

