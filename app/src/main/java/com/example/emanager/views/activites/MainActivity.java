package com.example.emanager.views.activites;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModelProvider;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.Toast;

import com.example.emanager.R;
import com.example.emanager.databinding.ActivityMainBinding;
import com.example.emanager.utils.Constants;
import com.example.emanager.viewmodels.MainViewModel;
import com.example.emanager.views.fragments.StatsFragment;
import com.example.emanager.views.fragments.TransactionsFragment;
import com.example.emanager.views.fragments.GroupExpenseFragment;
import com.google.android.material.navigation.NavigationBarView;

import java.util.Calendar;

public class MainActivity extends AppCompatActivity {

    ActivityMainBinding binding;
    Calendar calendar;
    public MainViewModel viewModel;

    private static final String PREF_NAME = "user_prefs";
    private static final String KEY_USER_NAME = "user_name";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        viewModel = new ViewModelProvider(this).get(MainViewModel.class);

        // Check if the user name is already saved
        SharedPreferences preferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        String userName = preferences.getString(KEY_USER_NAME, null);

        // If user name is not already saved, prompt for it
        if (userName == null) {
            promptForUserName();
        } else {
            // If the name is saved, display a welcome message
            Toast.makeText(this, "Welcome back, " + userName, Toast.LENGTH_SHORT).show();
        }

        setSupportActionBar(binding.toolBar);
        getSupportActionBar().setTitle("Transactions");

        Constants.setCategories();
        calendar = Calendar.getInstance();

        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.content, new TransactionsFragment());
        transaction.commit();

        binding.bottomNavigationView.setOnItemSelectedListener(new NavigationBarView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
                if (item.getItemId() == R.id.transactions) {
                    getSupportFragmentManager().popBackStack();
                } else if (item.getItemId() == R.id.stats) {
                    transaction.replace(R.id.content, new StatsFragment());
                    transaction.addToBackStack(null);
                } else if (item.getItemId() == R.id.more) {
                    transaction.replace(R.id.content, new GroupExpenseFragment());
                    transaction.addToBackStack(null);
                }
                transaction.commit();
                return true;
            }
        });
    }

    // Prompt the user for their name
    private void promptForUserName() {
        // Create an input dialog to ask the user for their name
        EditText editText = new EditText(this);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Enter your name")
                .setView(editText)
                .setPositiveButton("OK", (dialog, which) -> {
                    String userName = editText.getText().toString();
                    if (!userName.isEmpty()) {
                        saveUserName(userName); // Save the user name
                        Toast.makeText(this, "Welcome, " + userName, Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "Name cannot be empty!", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .show();
    }


    // Save the user's name to SharedPreferences
    private void saveUserName(String name) {
        SharedPreferences preferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(KEY_USER_NAME, name);
        editor.apply(); // Commit changes
    }

    public String getUserName() {
        // Get SharedPreferences instance
        SharedPreferences preferences = getSharedPreferences(Constants.PREFERENCE_NAME, MODE_PRIVATE);
        // Retrieve the username (returns null if not set)
        return preferences.getString(Constants.KEY_USER_NAME, null);
    }

    public void getTransactions() {
        viewModel.getTransactions(calendar);
    }

    public void getGroupExpenses() {
        viewModel.getGroupExpenses();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.top_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }
}
