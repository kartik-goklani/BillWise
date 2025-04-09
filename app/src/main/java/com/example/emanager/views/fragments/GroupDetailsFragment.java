package com.example.emanager.views.fragments;

import static android.service.controls.ControlsProviderService.TAG;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import android.Manifest;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.emanager.R;
import com.example.emanager.adapters.GroupExpenseAdapter;
import com.example.emanager.databinding.FragmentGroupDetailsBinding;
import com.example.emanager.models.PersonExpense;
import com.example.emanager.utils.OpenAIClient;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GroupDetailsFragment extends Fragment {

    private TextView groupNameTextView, groupIdTextView;
    private EditText expenseNameEditText, expenseAmountEditText;
    private Button addExpenseButton;
    private RecyclerView recyclerView;
    private GroupExpenseAdapter expenseAdapter;
    private ArrayList<PersonExpense> expenseList;
    private LinearLayout membersBalanceContainer;

    private String groupId;
    private String groupName;

    private FirebaseFirestore firestore;

    private ActivityResultLauncher<Intent> cameraLauncher;
    private Uri imageUri;
    private Uri photoUri;

    FragmentGroupDetailsBinding binding;
    private double extractedTotalExpense = 0.0;

    public GroupDetailsFragment() {
        // Required empty public constructor
    }

    // NewInstance method for safe fragment creation
    public static GroupDetailsFragment newInstance(String groupId, String groupName) {
        GroupDetailsFragment fragment = new GroupDetailsFragment();
        Bundle args = new Bundle();
        args.putString("groupId", groupId);
        args.putString("groupName", groupName);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        cameraLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == getActivity().RESULT_OK) {
                        try {
                            Bitmap bitmap = MediaStore.Images.Media.getBitmap(requireActivity().getContentResolver(), imageUri);
                            runOCR(bitmap);
                        } catch (IOException e) {
                            e.printStackTrace();
                            Toast.makeText(getContext(), "Failed to load image", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
        );
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        binding = FragmentGroupDetailsBinding.inflate(inflater, container, false);
        View view = binding.getRoot();

        // Initialize views using findViewById on the binding's root view
        groupNameTextView = view.findViewById(R.id.groupNameTextView);
        groupIdTextView = view.findViewById(R.id.groupIdTextView);
        expenseNameEditText = view.findViewById(R.id.expenseNameEditText);
        expenseAmountEditText = view.findViewById(R.id.expenseAmountEditText);
        addExpenseButton = view.findViewById(R.id.addExpenseButton);
        recyclerView = view.findViewById(R.id.recyclerView);
        membersBalanceContainer = view.findViewById(R.id.membersBalanceContainer);

        firestore = FirebaseFirestore.getInstance();

        // Rest of your code remains the same
        if (getArguments() != null) {
            groupId = getArguments().getString("groupId");
            groupName = getArguments().getString("groupName");

            groupNameTextView.setText(groupName);
            groupIdTextView.setText("Group ID: " + groupId);
        }

        expenseList = new ArrayList<>();
        expenseAdapter = new GroupExpenseAdapter(getContext(), expenseList, groupId, groupName);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(expenseAdapter);

        loadExpenses();
        loadMemberBalances(); // Load member balances

        addExpenseButton.setOnClickListener(v -> {
            showMemberSelectionDialog();
        });

        binding.camButton.setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
                    != PackageManager.PERMISSION_GRANTED) {
                // Use requestPermissionLauncher instead of ActivityCompat.requestPermissions
                requestPermissionLauncher.launch(Manifest.permission.CAMERA);
                return;
            }
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            File photo = new File(requireActivity().getExternalFilesDir(null), "pic.jpg");
            imageUri = FileProvider.getUriForFile(requireContext(), "com.example.emanager.fileprovider", photo);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
            cameraLauncher.launch(intent);
        });

        return view;
    }

    // New method to load and display member balances
    private void loadMemberBalances() {
        firestore.collection("groups").document(groupId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Map<String, Object> rawBalances = (Map<String, Object>) documentSnapshot.get("balances");

                        // Clear previous balances
                        membersBalanceContainer.removeAllViews();

                        if (rawBalances != null && !rawBalances.isEmpty()) {
                            for (Map.Entry<String, Object> entry : rawBalances.entrySet()) {
                                String memberName = entry.getKey();
                                double balance = 0.0;

                                if (entry.getValue() instanceof Number) {
                                    balance = ((Number) entry.getValue()).doubleValue();
                                }

                                // Create a TextView for each member's balance
                                TextView balanceView = new TextView(getContext());
                                String balanceText = memberName + ": ₹" + String.format("%.2f", balance);
                                balanceView.setText(balanceText);
                                balanceView.setTextSize(16);
                                balanceView.setPadding(8, 8, 8, 8);

                                // Set text color based on balance (red for negative, green for positive)
                                if (balance < 0) {
                                    balanceView.setTextColor(Color.parseColor("#388E3C")); // Red
                                } else if (balance > 0) {
                                    balanceView.setTextColor(Color.parseColor("#D32F2F")); // Green
                                }

                                membersBalanceContainer.addView(balanceView);
                            }
                        } else {
                            // No balances found
                            TextView noBalancesView = new TextView(getContext());
                            noBalancesView.setText("No balances to display");
                            noBalancesView.setTextSize(16);
                            noBalancesView.setPadding(8, 8, 8, 8);
                            membersBalanceContainer.addView(noBalancesView);
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("MemberBalances", "Error loading balances", e);
                    Toast.makeText(getContext(), "Failed to load member balances", Toast.LENGTH_SHORT).show();
                });
    }

    private void runOCR(Bitmap bitmap) {
        InputImage image = InputImage.fromBitmap(bitmap, 0);
        TextRecognizer recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);

        recognizer.process(image)
                .addOnSuccessListener(visionText -> {
                    String ocrResult = visionText.getText();

                    // Optional: Show OCR result
                    Log.d("OCR Result", ocrResult);

                    // Send to OpenAI
                    OpenAIClient.sendToOpenAI(ocrResult, new OpenAIClient.ResponseCallback() {
                        @Override
                        public void onResponse(String result) {
                            requireActivity().runOnUiThread(() -> {
                                Log.d(TAG, "OpenAI Result: " + result);
                                new AlertDialog.Builder(requireContext())
                                        .setTitle("Extracted Expense")
                                        .setMessage(result)
                                        .setPositiveButton("Add", (dialog, which) -> {
                                            // Extract amount from response using regex
                                            extractedTotalExpense = extractAmountFromResponse(result);
                                            String stringAmount = String.valueOf(extractedTotalExpense);
                                            Log.d(TAG, "Extracted total expense: ₹" + extractedTotalExpense);

                                            expenseAmountEditText.setText(stringAmount);

                                            // Example: You can now use this value to pre-fill a transaction form or store in DB
                                            Toast.makeText(requireContext(), "Added ₹" + extractedTotalExpense, Toast.LENGTH_SHORT).show();

                                        })
                                        .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                                        .show();

                            });
                        }

                        @Override
                        public void onError(String error) {
                            requireActivity().runOnUiThread(() -> {
                                Log.e(TAG, "OpenAI Error: " + error);
                                Toast.makeText(requireContext(), "OpenAI Error: " + error, Toast.LENGTH_LONG).show();
                            });
                        }
                    });
                })
                .addOnFailureListener(e -> {
                    Log.e("OCR Failed", e.getMessage());
                    Toast.makeText(requireContext(), "OCR failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    openCamera();  // defined below
                } else {
                    Toast.makeText(getContext(), "Camera permission denied", Toast.LENGTH_SHORT).show();
                }
            });

    private void openCamera() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (intent.resolveActivity(requireActivity().getPackageManager()) != null) {
            File photoFile = null;
            try {
                File storageDir = requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES);
                photoFile = File.createTempFile(
                        "JPEG_" + System.currentTimeMillis(),  // filename
                        ".jpg",                               // extension
                        storageDir                             // directory
                );
            } catch (IOException ex) {
                Toast.makeText(getContext(), "Error creating image file", Toast.LENGTH_SHORT).show();
                return;
            }

            if (photoFile != null) {
                photoUri = FileProvider.getUriForFile(
                        requireContext(),
                        "com.example.emanager.fileprovider", // same as manifest
                        photoFile
                );
                intent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
                startActivity(intent);
            }
        }
    }

    private double extractAmountFromResponse(String jsonResponse) {
        try {
            // Clean the string if it's not strictly JSON
            jsonResponse = jsonResponse.trim();
            if (jsonResponse.startsWith("```json")) {
                jsonResponse = jsonResponse.replace("```json", "").trim();
            }
            if (jsonResponse.endsWith("```")) {
                jsonResponse = jsonResponse.substring(0, jsonResponse.length() - 3).trim();
            }

            // Convert to JSONObject
            org.json.JSONObject jsonObject = new org.json.JSONObject(jsonResponse);

            // Get last key
            java.util.Iterator<String> keys = jsonObject.keys();
            String lastKey = null;
            while (keys.hasNext()) {
                lastKey = keys.next();
            }

            if (lastKey != null) {
                return jsonObject.getDouble(lastKey);
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to extract total expense from JSON: " + e.getMessage());
        }
        return 0.0;
    }

    private void loadExpenses() {
        CollectionReference expensesRef = firestore.collection("groups").document(groupId).collection("expenses");
        expensesRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                expenseList.clear();
                QuerySnapshot snapshot = task.getResult();
                if (snapshot != null) {
                    for (QueryDocumentSnapshot document : snapshot) {
                        // FIXED: Manually create PersonExpense with document ID
                        PersonExpense expense = new PersonExpense(
                                document.getString("paidBy"),           // personName
                                document.getDouble("amount") != null ? document.getDouble("amount") : 0.0, // amount
                                groupId,                               // groupId
                                groupName,                             // groupName
                                document.getString("expenseName"),      // expenseName
                                document.getId()                        // expenseId - ESSENTIAL FIX
                        );

                        Log.d("ExpenseDebug", "Loaded expense: " + expense.getExpenseName() +
                                " with ID: " + expense.getExpenseId());

                        expenseList.add(expense);
                    }
                    expenseAdapter.notifyDataSetChanged();
                }
            } else {
                Toast.makeText(getContext(), "Failed to load expenses", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showMemberSelectionDialog() {
        String expenseName = expenseNameEditText.getText().toString().trim();
        String amountStr = expenseAmountEditText.getText().toString().trim();

        if (TextUtils.isEmpty(expenseName) || TextUtils.isEmpty(amountStr)) {
            Toast.makeText(getContext(), "Please enter both expense name and amount", Toast.LENGTH_SHORT).show();
            return;
        }

        double amount = Double.parseDouble(amountStr);

        // Get the current user name from SharedPreferences
        SharedPreferences sharedPreferences = requireContext().getSharedPreferences("user_prefs", Context.MODE_PRIVATE);
        String currentUser = sharedPreferences.getString("user_name", "Unknown");

        // Use array to allow modification in lambda
        final String[] paidBy = {currentUser};

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Expense Details");

        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_select_members, null);
        LinearLayout membersCheckboxContainer = dialogView.findViewById(R.id.membersCheckboxContainer);

        // Create section headers and containers
        TextView payerHeader = new TextView(getContext());
        payerHeader.setText("Who paid for this expense?");
        payerHeader.setTextSize(16);
        payerHeader.setPadding(0, 8, 0, 8);
        payerHeader.setTypeface(null, Typeface.BOLD);
        membersCheckboxContainer.addView(payerHeader);

        RadioGroup payerRadioGroup = new RadioGroup(getContext());
        membersCheckboxContainer.addView(payerRadioGroup);

        View divider = new View(getContext());
        divider.setBackgroundColor(Color.LTGRAY);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 2);
        params.setMargins(0, 16, 0, 16);
        divider.setLayoutParams(params);
        membersCheckboxContainer.addView(divider);

        TextView splitHeader = new TextView(getContext());
        splitHeader.setText("Split expense among:");
        splitHeader.setTextSize(16);
        splitHeader.setPadding(0, 8, 0, 8);
        splitHeader.setTypeface(null, Typeface.BOLD);
        membersCheckboxContainer.addView(splitHeader);

        // Container for checkboxes
        LinearLayout splitCheckboxContainer = new LinearLayout(getContext());
        splitCheckboxContainer.setOrientation(LinearLayout.VERTICAL);
        membersCheckboxContainer.addView(splitCheckboxContainer);

        firestore.collection("groups").document(groupId).get().addOnSuccessListener(documentSnapshot -> {
            List<String> members = (List<String>) documentSnapshot.get("members");
            if (members != null) {
                // Add radio buttons for payer selection
                for (int i = 0; i < members.size(); i++) {
                    String member = members.get(i);

                    RadioButton radioButton = new RadioButton(getContext());
                    radioButton.setText(member);
                    radioButton.setId(View.generateViewId());

                    // Set current user as default selected
                    if (member.equals(currentUser)) {
                        radioButton.setChecked(true);
                    }

                    payerRadioGroup.addView(radioButton);
                }

                // Add checkboxes for split selection
                for (String member : members) {
                    CheckBox checkBox = new CheckBox(getContext());
                    checkBox.setText(member);
                    // Default to checked for all members
                    checkBox.setChecked(true);
                    splitCheckboxContainer.addView(checkBox);
                }
            }
        });

        // Set listener for radio group
        payerRadioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            RadioButton selectedRadio = dialogView.findViewById(checkedId);
            if (selectedRadio != null) {
                paidBy[0] = selectedRadio.getText().toString();
            }
        });

        builder.setView(dialogView);

        builder.setPositiveButton("Save", (dialog, which) -> {
            List<String> selectedMembers = new ArrayList<>();

            // Get selected members from the split checkbox container
            for (int i = 0; i < splitCheckboxContainer.getChildCount(); i++) {
                View child = splitCheckboxContainer.getChildAt(i);
                if (child instanceof CheckBox) {
                    CheckBox checkBox = (CheckBox) child;
                    if (checkBox.isChecked()) {
                        selectedMembers.add(checkBox.getText().toString());
                    }
                }
            }

            if (selectedMembers.isEmpty()) {
                Toast.makeText(getContext(), "Please select at least one member", Toast.LENGTH_SHORT).show();
                return;
            }

            // Save expense with selected members and payer
            saveExpense(expenseName, amount, paidBy[0], selectedMembers);
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());

        builder.show();
    }



    private void saveExpense(String expenseName, double amount, String paidBy, List<String> splitAmong) {
        // Generate document ID first so we can use it for the expense
        DocumentReference newExpenseRef = firestore.collection("groups").document(groupId)
                .collection("expenses").document();
        String expenseId = newExpenseRef.getId();

        Map<String, Object> expenseData = new HashMap<>();
        expenseData.put("expenseName", expenseName);
        expenseData.put("amount", amount);
        expenseData.put("paidBy", paidBy);
        expenseData.put("splitAmong", splitAmong);
        expenseData.put("timestamp", com.google.firebase.firestore.FieldValue.serverTimestamp());

        newExpenseRef.set(expenseData)
                .addOnSuccessListener(aVoid -> {
                    // Calculate split amount here
                    double splitAmount = amount / splitAmong.size();
                    updateBalances(splitAmong, paidBy, splitAmount);

                    Toast.makeText(getContext(), "Expense added successfully", Toast.LENGTH_SHORT).show();
                    expenseNameEditText.setText("");
                    expenseAmountEditText.setText("");
                    loadExpenses(); // Refresh the list
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Failed to add expense", Toast.LENGTH_SHORT).show();
                });
    }

    private void updateBalances(List<String> selectedMembers, String payerName, double splitAmount) {
        DocumentReference groupRef = firestore.collection("groups").document(groupId);

        firestore.runTransaction(transaction -> {
            DocumentSnapshot snapshot = transaction.get(groupRef);
            Map<String, Object> balancesObj = (Map<String, Object>) snapshot.get("balances");
            Map<String, Double> balances = new HashMap<>();

            if (balancesObj != null) {
                for (Map.Entry<String, Object> entry : balancesObj.entrySet()) {
                    // Convert all number types to Double
                    if (entry.getValue() instanceof Number) {
                        balances.put(entry.getKey(), ((Number) entry.getValue()).doubleValue());
                    }
                }
            }

            for (String member : selectedMembers) {
                if (!member.equals(payerName)) {
                    // Get current balance with default 0.0
                    double currentBalance = balances.getOrDefault(member, 0.0);
                    balances.put(member, currentBalance + splitAmount);

                    // Handle payer's balance
                    double payerBalance = balances.getOrDefault(payerName, 0.0);
                    balances.put(payerName, payerBalance - splitAmount);
                }
            }

            transaction.update(groupRef, "balances", balances);
            return null;
        }).addOnSuccessListener(aVoid -> {
            Log.d("UpdateBalances", "Balances updated successfully");
        }).addOnFailureListener(e -> {
            Log.e("UpdateBalances", "Error updating balances", e);
        });
    }

}
