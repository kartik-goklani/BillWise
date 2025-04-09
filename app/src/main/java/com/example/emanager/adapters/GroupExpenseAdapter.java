package com.example.emanager.adapters;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;
import com.example.emanager.R;
import com.example.emanager.models.PersonExpense;
import com.example.emanager.views.fragments.GroupDetailsFragment;
import com.google.firebase.firestore.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GroupExpenseAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final int TYPE_GROUP = 1;
    private static final int TYPE_EXPENSE = 2;

    private final Context context;
    private final ArrayList<PersonExpense> items;
    private final String groupId;
    private final String groupName;
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    public GroupExpenseAdapter(Context context, ArrayList<PersonExpense> items,
                               String groupId, String groupName) {
        this.context = context;
        this.items = items;
        this.groupId = groupId;
        this.groupName = groupName;
    }

    private String getCurrentUser() {
        SharedPreferences prefs = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE);
        return prefs.getString("user_name", "");
    }

    @Override
    public int getItemViewType(int position) {
        return (items.get(position).getExpenseName() == null ||
                items.get(position).getExpenseName().isEmpty()) ? TYPE_GROUP : TYPE_EXPENSE;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(context);
        if (viewType == TYPE_GROUP) {
            View view = inflater.inflate(R.layout.item_group, parent, false);
            return new GroupViewHolder(view);
        } else {
            View view = inflater.inflate(R.layout.item_person_expense, parent, false);
            return new ExpenseViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        PersonExpense item = items.get(position);

        if (holder.getItemViewType() == TYPE_GROUP) {
            GroupViewHolder groupHolder = (GroupViewHolder) holder;
            groupHolder.groupNameTextView.setText(item.getPersonName());

            // Click listener for entire item
            groupHolder.itemView.setOnClickListener(v -> {
                Log.d("GroupClick", "Opening group details for: " + item.getGroupId());
                openGroupDetails(item.getGroupId());
            });

            // Click listener for leave button
            groupHolder.leaveGroupImageView.setOnClickListener(v -> {
                Log.d("GroupLeave", "Leave button clicked for position: " + position);
                v.setEnabled(false);
                handleGroupLeave(position);
                v.postDelayed(() -> v.setEnabled(true), 500);
            });

            // Prevent event propagation
            groupHolder.leaveGroupImageView.setOnTouchListener((v, event) -> {
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    v.performClick();
                    return true;
                }
                return false;
            });

        } else {
            ExpenseViewHolder expenseHolder = (ExpenseViewHolder) holder;
            expenseHolder.nameTextView.setText(item.getExpenseName());
            expenseHolder.amountTextView.setText(String.format("₹%.2f", item.getAmount()));

            expenseHolder.removePersonBtn.setOnClickListener(v -> {
                Log.d("ExpenseDelete", "Deleting expense at position: " + position);
                handleExpenseDeletion(position);
            });
        }
    }

    private void openGroupDetails(String groupId) {
        if (context instanceof AppCompatActivity) {
            AppCompatActivity activity = (AppCompatActivity) context;
            GroupDetailsFragment fragment = GroupDetailsFragment.newInstance(groupId, groupName);
            activity.getSupportFragmentManager().beginTransaction()
                    .replace(R.id.content, fragment)
                    .addToBackStack(null)
                    .commit();
        }
    }

    private void handleGroupLeave(int position) {
        try {
            PersonExpense item = items.get(position);
            String groupIdToLeave = item.getGroupId();

            Log.d("GroupLeave", "Attempting to leave group: " + groupIdToLeave);
            Log.d("GroupLeave", "Current user: " + getCurrentUser());

            if (groupIdToLeave == null || groupIdToLeave.isEmpty()) {
                Log.e("GroupLeave", "Invalid group ID");
                Toast.makeText(context, "Invalid group ID", Toast.LENGTH_SHORT).show();
                return;
            }

            DocumentReference groupRef = db.collection("groups").document(groupIdToLeave);
            groupRef.get().addOnSuccessListener(documentSnapshot -> {
                if (!documentSnapshot.exists()) {
                    Log.e("GroupLeave", "Document doesn't exist");
                    Toast.makeText(context, "Group not found", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Check balances
                Map<String, Object> rawBalances = documentSnapshot.contains("balances") ?
                        (Map<String, Object>) documentSnapshot.get("balances") : new HashMap<>();
                Map<String, Double> balances = new HashMap<>();

                for (Map.Entry<String, Object> entry : rawBalances.entrySet()) {
                    if (entry.getValue() instanceof Number) {
                        balances.put(entry.getKey(), ((Number) entry.getValue()).doubleValue());
                    }
                }
                Double userBalance = balances.getOrDefault(getCurrentUser(), 0.0);

                if (userBalance != 0.0) {
                    Log.d("GroupLeave", "User balance not zero: " + userBalance);
                    Toast.makeText(context, "Clear ₹" + userBalance + " first", Toast.LENGTH_SHORT).show();
                    return;
                }

                new AlertDialog.Builder(context)
                        .setTitle("Leave Group?")
                        .setMessage("This action cannot be undone")
                        .setPositiveButton("Leave", (dialog, which) -> {
                            groupRef.update(
                                    "members", FieldValue.arrayRemove(getCurrentUser()),
                                    "balances." + getCurrentUser(), FieldValue.delete()
                            ).addOnCompleteListener(task -> {
                                if (task.isSuccessful()) {
                                    Log.d("GroupLeave", "Firestore update successful");
                                    int removePosition = findItemPositionByGroupId(groupIdToLeave);
                                    if (removePosition != -1) {
                                        Log.d("GroupLeave", "Removing item at position: " + removePosition);
                                        removeItem(removePosition);
                                    } else {
                                        Log.e("GroupLeave", "Item not found, forcing refresh");
                                        items.removeIf(i -> i.getGroupId().equals(groupIdToLeave));
                                        notifyDataSetChanged();
                                    }
                                    Toast.makeText(context, "Left group successfully", Toast.LENGTH_SHORT).show();

                                    // Add navigation code to go back to previous fragment
                                    if (context instanceof AppCompatActivity) {
                                        AppCompatActivity activity = (AppCompatActivity) context;
                                        // Check if we're in a group details fragment
                                        Fragment currentFragment = activity.getSupportFragmentManager().findFragmentById(R.id.content);
                                        if (currentFragment instanceof GroupDetailsFragment) {
                                            // Pop back stack to return to previous fragment
                                            activity.getSupportFragmentManager().popBackStack();
                                        }
                                    }
                                } else {
                                    Log.e("GroupLeave", "Firestore error: " + task.getException());
                                    Toast.makeText(context, "Failed to leave group", Toast.LENGTH_SHORT).show();
                                }
                            });
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            });

        } catch (Exception e) {
            Log.e("GroupLeave", "Critical error: ", e);
            Toast.makeText(context, "An error occurred", Toast.LENGTH_SHORT).show();
        }
    }


    private int findItemPositionByGroupId(String groupId) {
        for (int i = 0; i < items.size(); i++) {
            PersonExpense item = items.get(i);
            if (item.getGroupId() != null && item.getGroupId().equals(groupId)) {
                return i;
            }
        }
        return -1;
    }

    private void removeItem(int position) {
        if (position >= 0 && position < items.size()) {
            items.remove(position);
            notifyItemRemoved(position);
            notifyItemRangeChanged(position, items.size());
        } else {
            Log.e("Adapter", "Invalid position: " + position);
        }
    }

    private void handleExpenseDeletion(int position) {
        PersonExpense expense = items.get(position);

        // Log for debugging
        Log.d("ExpenseDebug", "Deleting expense: " + expense.getExpenseName() +
                ", ID: " + expense.getExpenseId());

        // FIXED: Use expenseId instead of expenseName
        String expenseId = expense.getExpenseId();
        if (expenseId == null || expenseId.isEmpty()) {
            Toast.makeText(context, "Invalid expense ID", Toast.LENGTH_SHORT).show();
            return;
        }

        DocumentReference expenseRef = db.collection("groups").document(groupId)
                .collection("expenses").document(expenseId);

        expenseRef.get().addOnSuccessListener(documentSnapshot -> {
            if (!documentSnapshot.exists()) {
                Toast.makeText(context, "Expense no longer exists", Toast.LENGTH_SHORT).show();
                removeItem(position);
                return;
            }

            Object amountObj = documentSnapshot.get("amount");
            if (!(amountObj instanceof Number)) {
                Toast.makeText(context, "Invalid amount data", Toast.LENGTH_SHORT).show();
                return;
            }
            double amount = ((Number) amountObj).doubleValue();

            String payer = documentSnapshot.getString("paidBy");
            List<String> splitAmong = (List<String>) documentSnapshot.get("splitAmong");

            if (payer == null || splitAmong == null || splitAmong.isEmpty()) {
                Toast.makeText(context, "Invalid expense data", Toast.LENGTH_SHORT).show();
                return;
            }

            // Update balances in reverse
            db.runTransaction(transaction -> {
                DocumentReference groupRef = db.collection("groups").document(groupId);
                DocumentSnapshot groupSnapshot = transaction.get(groupRef);

                Map<String, Object> balancesObj = (Map<String, Object>) groupSnapshot.get("balances");
                Map<String, Double> balances = new HashMap<>();

                if (balancesObj != null) {
                    for (Map.Entry<String, Object> entry : balancesObj.entrySet()) {
                        if (entry.getValue() instanceof Number) {
                            balances.put(entry.getKey(), ((Number) entry.getValue()).doubleValue());
                        }
                    }
                }

                double splitAmount = amount / splitAmong.size();

                for (String member : splitAmong) {
                    if (!member.equals(payer)) {
                        // Reverse the original transaction
                        balances.put(member, balances.getOrDefault(member, 0.0) - splitAmount);
                        balances.put(payer, balances.getOrDefault(payer, 0.0) + splitAmount);
                    }
                }

                transaction.update(groupRef, "balances", balances);
                return null;
            }).addOnSuccessListener(v -> {
                // Delete expense after balance update
                expenseRef.delete().addOnSuccessListener(v2 -> {
                    Toast.makeText(context, "Transaction deleted", Toast.LENGTH_SHORT).show();
                    removeItem(position);
                });
            });
        });
    }


    static class GroupViewHolder extends RecyclerView.ViewHolder {
        TextView groupNameTextView;
        ImageView leaveGroupImageView;

        public GroupViewHolder(@NonNull View itemView) {
            super(itemView);
            groupNameTextView = itemView.findViewById(R.id.groupNameTextView);
            leaveGroupImageView = itemView.findViewById(R.id.leaveGroupImageView);
        }
    }

    static class ExpenseViewHolder extends RecyclerView.ViewHolder {
        TextView nameTextView, amountTextView;
        ImageView removePersonBtn;

        public ExpenseViewHolder(@NonNull View itemView) {
            super(itemView);
            nameTextView = itemView.findViewById(R.id.personName);
            amountTextView = itemView.findViewById(R.id.personAmount);
            removePersonBtn = itemView.findViewById(R.id.removePersonBtn);
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public void updateData(ArrayList<PersonExpense> newItems) {
        items.clear();
        items.addAll(newItems);
        notifyDataSetChanged();
    }
}
