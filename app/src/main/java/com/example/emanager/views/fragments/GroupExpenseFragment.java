package com.example.emanager.views.fragments;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.emanager.R;
import com.example.emanager.adapters.GroupExpenseAdapter;
import com.example.emanager.databinding.FragmentGroupExpenseBinding;
import com.example.emanager.models.PersonExpense;
import com.example.emanager.utils.RecyclerItemClickListener;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;






public class GroupExpenseFragment extends Fragment {
    private FirebaseFirestore db;
    private String currentGroupId = null, currentGroupName = null;
    private ArrayList<PersonExpense> personExpenses;
    private GroupExpenseAdapter adapter;
    private FragmentGroupExpenseBinding binding;
    private ArrayList<PersonExpense> groupExpenses = new ArrayList<>(); // Store group expenses separately

    public GroupExpenseFragment() {}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentGroupExpenseBinding.inflate(inflater, container, false);
        db = FirebaseFirestore.getInstance();
        personExpenses = new ArrayList<>();

        loadSavedGroupData();
        setupRecyclerView();
        loadUserGroups();
        setupListeners();

        return binding.getRoot();
    }

    private void loadSavedGroupData() {
        SharedPreferences preferences = requireActivity().getSharedPreferences("group_prefs", Context.MODE_PRIVATE);
        currentGroupId = preferences.getString("group_id", null);
        currentGroupName = preferences.getString("group_name", null);
        if (currentGroupId != null) updateUIForGroup();
    }

    private void loadUserGroups() {
        String userName = getUserName();
        if (userName.equals("Unknown User")) return;

        db.collection("groups").whereArrayContains("members", userName)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    personExpenses.clear();

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        String groupName = document.getString("groupName");
                        String groupId = document.getId();

                        // Format group name as "Group Name (GroupID)"
                        String displayName = groupName + " (" + groupId + ")";

                        // Store formatted name
                        personExpenses.add(new PersonExpense(displayName, 0.0, groupId, groupName));
                    }

                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> Log.e("FirestoreError", "Error loading groups", e));
    }


    private void setupRecyclerView() {
        adapter = new GroupExpenseAdapter(getContext(), personExpenses, getUserName(), currentGroupName);
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.recyclerView.setAdapter(adapter);

        binding.recyclerView.addOnItemTouchListener(new RecyclerItemClickListener(getContext(), binding.recyclerView,
                new RecyclerItemClickListener.OnItemClickListener() {
                    @Override
                    public void onItemClick(View view, int position) {
                        PersonExpense selectedGroup = personExpenses.get(position);

                        GroupDetailsFragment fragment = new GroupDetailsFragment();
                        Bundle bundle = new Bundle();
                        bundle.putString("groupId", selectedGroup.getGroupId());
                        bundle.putString("groupName", selectedGroup.getGroupName()); // optional, if you want to show the name
                        fragment.setArguments(bundle);

                        getActivity().getSupportFragmentManager().beginTransaction()
                                .replace(R.id.content, fragment)
                                .addToBackStack(null)
                                .commit();
                    }

                    @Override
                    public void onLongItemClick(View view, int position) {}
                }
        ));
    }

    private void setupListeners() {
        binding.fabGroupAction.setOnClickListener(v -> showGroupOptionsDialog());
    }

    private void showGroupOptionsDialog() {
        new AlertDialog.Builder(getContext())
                .setTitle("Group Options")
                .setItems(new String[]{"Create Group", "Join Group"}, (dialog, which) -> {
                    if (which == 0) createNewGroup();
                    else joinExistingGroup();
                }).show();
    }

    private String getUserName() {
        SharedPreferences preferences = requireActivity().getSharedPreferences("user_prefs", Context.MODE_PRIVATE);
        return preferences.getString("user_name", "Unknown User");
    }

    private void saveGroupData(String groupId, String groupName) {
        SharedPreferences.Editor editor = requireActivity().getSharedPreferences("group_prefs", Context.MODE_PRIVATE).edit();
        editor.putString("group_id", groupId);
        editor.putString("group_name", groupName);
        editor.apply();
    }

    private void createNewGroup() {
        EditText input = new EditText(getContext());
        new AlertDialog.Builder(getContext()).setTitle("Enter Group Name").setView(input)
                .setPositiveButton("Create", (dialog, which) -> {
                    String groupName = input.getText().toString().trim();
                    if (TextUtils.isEmpty(groupName)) return;

                    String groupId = generateRandomGroupId();
                    String admin = getUserName();
                    // Create group document
                    Map<String, Object> groupData = new HashMap<>();
                    groupData.put("groupName", groupName);
                    groupData.put("groupId", groupId);
                    groupData.put("admin", admin);
                    groupData.put("members", new ArrayList<String>() {{ add(admin); }});

                    db.collection("groups").document(groupId)
                            .set(groupData)
                            .addOnSuccessListener(aVoid -> {
                                // Create an initial expense subcollection with an empty document

                                // Save locally
                                currentGroupId = groupId;
                                currentGroupName = groupName;
                                saveGroupData(groupId, groupName);
                                updateUIForGroup();
                            });
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .show();
    }



    private void joinExistingGroup() {
        EditText input = new EditText(getContext());
        new AlertDialog.Builder(getContext()).setTitle("Enter Group ID").setView(input)
                .setPositiveButton("Join", (dialog, which) -> {
                    String groupId = input.getText().toString().trim();
                    if (TextUtils.isEmpty(groupId)) {
                        Toast.makeText(getContext(), "Please enter a group ID", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    db.collection("groups").document(groupId).get()
                            .addOnSuccessListener(documentSnapshot -> {
                                if (!documentSnapshot.exists()) {
                                    Toast.makeText(getContext(), "Invalid group ID. Group not found.", Toast.LENGTH_SHORT).show();
                                    return;
                                }

                                currentGroupId = groupId;
                                currentGroupName = documentSnapshot.getString("groupName");
                                saveGroupData(groupId, currentGroupName);
                                updateUIForGroup();
                                addMemberToGroup(groupId, getUserName());
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(getContext(), "Error joining group: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .show();
    }


    private void addMemberToGroup(String groupId, String userName) {
        if (userName.equals("Unknown User")) return;

        db.collection("groups").document(groupId)
                .update("members", FieldValue.arrayUnion(userName))
                .addOnSuccessListener(aVoid -> Toast.makeText(getContext(), "Joined group successfully!", Toast.LENGTH_SHORT).show());
    }

    private void updateUIForGroup() {
        loadGroupExpenses();
    }

    private String generateRandomGroupId() {
        String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        Random random = new Random();
        StringBuilder groupId = new StringBuilder();
        for (int i = 0; i < 7; i++) groupId.append(characters.charAt(random.nextInt(characters.length())));
        return groupId.toString();
    }

    private void saveGroupExpense() {
        if (currentGroupId == null || personExpenses.isEmpty()) return;

        db.collection("groups").document(currentGroupId).collection("expenses")
                .add(new HashMap<String, Object>() {{
                    put("expenses", personExpenses);
                }})
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(getContext(), "Expenses saved!", Toast.LENGTH_SHORT).show();
                    personExpenses.clear();
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> Toast.makeText(getContext(), "Error saving expenses", Toast.LENGTH_SHORT).show());
    }



    private void loadGroupExpenses() {
        db.collection("groups").document(currentGroupId).collection("expenses")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    groupExpenses.clear(); // Clear the expense list, NOT the group list
                    for (QueryDocumentSnapshot documentSnapshot : queryDocumentSnapshots) {
                        groupExpenses.add(documentSnapshot.toObject(PersonExpense.class));
                    }
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> Log.e("FirestoreError", "Error loading expenses", e));
    }

}
