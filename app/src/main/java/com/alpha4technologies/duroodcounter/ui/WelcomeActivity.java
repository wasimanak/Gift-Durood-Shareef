package com.alpha4technologies.duroodcounter.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.alpha4technologies.duroodcounter.R;
import com.alpha4technologies.duroodcounter.data.PreferencesManager;
import com.alpha4technologies.duroodcounter.databinding.ActivityWelcomeBinding;
import com.alpha4technologies.duroodcounter.databinding.DialogUserProfileBinding;
import com.alpha4technologies.duroodcounter.model.User;
import com.alpha4technologies.duroodcounter.viewmodel.AuthViewModel;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class WelcomeActivity extends AppCompatActivity {
    private ActivityWelcomeBinding binding;
    private AuthViewModel authViewModel;
    private PreferencesManager prefsManager;

    @Override
    protected void attachBaseContext(android.content.Context newBase) {
        super.attachBaseContext(com.alpha4technologies.duroodcounter.DuroodApplication.getFixedFontScaleContext(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityWelcomeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        prefsManager = new PreferencesManager(this);
        User currentUser = prefsManager.getUser();

        // If user is already registered (not a guest), bypass welcome screen automatically!
        if (prefsManager.isLoggedIn() && currentUser != null && !currentUser.isGuest()) {
            navigateToMain();
            return;
        }

        authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);
        observeAuthResult();

        binding.btnStart.setOnClickListener(v -> showUserProfileDialog());
        binding.btnSkipGuest.setOnClickListener(v -> {
            if (currentUser != null && currentUser.isGuest()) {
                navigateToMain();
            } else {
                authViewModel.startGuestSession("Guest City");
            }
        });
    }

    private void observeAuthResult() {
        authViewModel.getAuthResultLiveData().observe(this, result -> {
            if (result == null) return;
            switch (result.status) {
                case LOADING:
                    binding.progressBar.setVisibility(View.VISIBLE);
                    binding.btnStart.setEnabled(false);
                    binding.btnSkipGuest.setEnabled(false);
                    break;
                case SUCCESS:
                    binding.progressBar.setVisibility(View.GONE);
                    binding.btnStart.setEnabled(true);
                    binding.btnSkipGuest.setEnabled(true);
                    Toast.makeText(WelcomeActivity.this, "Welcome, " + result.user.getFullName() + "!", Toast.LENGTH_SHORT).show();
                    navigateToMain();
                    break;
                case ERROR:
                    binding.progressBar.setVisibility(View.GONE);
                    binding.btnStart.setEnabled(true);
                    binding.btnSkipGuest.setEnabled(true);
                    new MaterialAlertDialogBuilder(WelcomeActivity.this)
                            .setTitle("Profile Error")
                            .setMessage(result.errorMessage)
                            .setPositiveButton("OK", null)
                            .show();
                    break;
            }
        });
    }

    private void showUserProfileDialog() {
        DialogUserProfileBinding dialogBinding = DialogUserProfileBinding.inflate(LayoutInflater.from(this));

        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setView(dialogBinding.getRoot())
                .setPositiveButton(R.string.btn_continue, null)
                .setNegativeButton(R.string.btn_cancel, (d, which) -> d.dismiss())
                .create();

        dialog.show();

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String fullName = dialogBinding.etFullName.getText() != null ? dialogBinding.etFullName.getText().toString() : "";
            String city = dialogBinding.etCity.getText() != null ? dialogBinding.etCity.getText().toString() : "";
            String country = dialogBinding.etCountry.getText() != null ? dialogBinding.etCountry.getText().toString() : "";

            if (fullName.trim().isEmpty() || city.trim().isEmpty()) {
                Toast.makeText(WelcomeActivity.this, "Full Name and City are required.", Toast.LENGTH_SHORT).show();
                return;
            }

            dialog.dismiss();

            User currentUser = prefsManager.getUser();
            if (currentUser != null && currentUser.isGuest()) {
                authViewModel.upgradeGuestUser(fullName, city, country);
            } else {
                authViewModel.startOrLoginUser(fullName, city, country);
            }
        });
    }

    private void navigateToMain() {
        Intent intent = new Intent(WelcomeActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
    }
}
