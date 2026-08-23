package com.alpha4technologies.duroodcounter.ui;

import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.ScaleAnimation;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.alpha4technologies.duroodcounter.R;
import com.alpha4technologies.duroodcounter.databinding.DialogUserProfileBinding;
import com.alpha4technologies.duroodcounter.databinding.FragmentCounterBinding;
import com.alpha4technologies.duroodcounter.viewmodel.AuthViewModel;
import com.alpha4technologies.duroodcounter.viewmodel.CounterViewModel;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.text.NumberFormat;
import java.util.Locale;

public class CounterFragment extends Fragment {
    private FragmentCounterBinding binding;
    private CounterViewModel counterViewModel;
    private AuthViewModel authViewModel;
    private Vibrator vibrator;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentCounterBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    private com.alpha4technologies.duroodcounter.model.DonationInfo currentDonationInfo;
    private boolean donationDialogShownThisSession = false;

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        counterViewModel = new ViewModelProvider(requireActivity()).get(CounterViewModel.class);
        authViewModel = new ViewModelProvider(requireActivity()).get(AuthViewModel.class);
        vibrator = (Vibrator) requireActivity().getSystemService(Context.VIBRATOR_SERVICE);

        observeViewModel();

        binding.rootCounterLayout.setOnClickListener(v -> handleTap());
        binding.scrollViewCounter.setOnClickListener(v -> handleTap());
        binding.flTapButtonContainer.setOnClickListener(v -> handleTap());
        binding.btnRegisterGuest.setOnClickListener(v -> showRegisterProfileDialog());
        binding.btnSupportUs.setOnClickListener(v -> {
            if (currentDonationInfo != null) {
                DonationDialog.show(requireContext(), currentDonationInfo);
            }
        });
    }

    private void observeViewModel() {
        NumberFormat formatter = NumberFormat.getInstance(Locale.US);

        counterViewModel.getCurrentUser().observe(getViewLifecycleOwner(), user -> {
            if (user != null) {
                if (user.isGuest()) {
                    binding.tvUserGreeting.setText("Welcome, Guest!");
                    binding.btnRegisterGuest.setVisibility(View.VISIBLE);
                } else {
                    binding.tvUserGreeting.setText("Assalamu Alaikum, " + user.getFullName() + "!");
                    binding.btnRegisterGuest.setVisibility(View.GONE);
                }
            }
        });

        counterViewModel.getTasbihName().observe(getViewLifecycleOwner(), name -> {
            if (name != null && !name.isEmpty()) {
                binding.tvTasbihName.setText(name);
            }
        });

        counterViewModel.getDonationInfo().observe(getViewLifecycleOwner(), info -> {
            currentDonationInfo = info;
            if (info != null && info.isShowDialog()) {
                binding.btnSupportUs.setVisibility(View.VISIBLE);
                if (!donationDialogShownThisSession) {
                    donationDialogShownThisSession = true;
                    DonationDialog.show(requireContext(), info);
                }
            } else {
                binding.btnSupportUs.setVisibility(View.GONE);
            }
        });

        counterViewModel.getTodayCount().observe(getViewLifecycleOwner(), count -> {
            long val = count != null ? count : 0L;
            binding.tvTodayCount.setText(formatter.format(val));
            binding.tvTapDisplay.setText(formatter.format(val));
        });

        counterViewModel.getTotalCount().observe(getViewLifecycleOwner(), count -> {
            long val = count != null ? count : 0L;
            binding.tvTotalCount.setText(formatter.format(val));
        });

        long GOAL_1M = 1000000L;
        counterViewModel.getGlobalOverallTotal().observe(getViewLifecycleOwner(), total -> {
            long currentOverall = total != null ? total : 0L;
            double percentage = Math.min(100.0, ((double) currentOverall / GOAL_1M) * 100.0);
            long remaining = Math.max(0L, GOAL_1M - currentOverall);

            binding.progressGoal1M.setProgress((int) Math.min(GOAL_1M, currentOverall));
            binding.tvGoalPercentage.setText(String.format(Locale.US, "%.1f%%", percentage));
            binding.tvGoalProgressText.setText(formatter.format(currentOverall) + " / " + formatter.format(GOAL_1M));

            if (currentOverall >= GOAL_1M) {
                binding.tvGoalRemainingText.setText("Goal Achieved! 🎉");
            } else {
                binding.tvGoalRemainingText.setText(formatter.format(remaining) + " Remaining");
            }
        });

        authViewModel.getAuthResultLiveData().observe(getViewLifecycleOwner(), result -> {
            if (result != null && result.status == AuthViewModel.AuthResult.Status.SUCCESS) {
                counterViewModel.refreshCounts();
            }
        });
    }

    private void showRegisterProfileDialog() {
        DialogUserProfileBinding dialogBinding = DialogUserProfileBinding.inflate(LayoutInflater.from(requireContext()));

        AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
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
                Toast.makeText(requireContext(), "Full Name and City are required.", Toast.LENGTH_SHORT).show();
                return;
            }

            dialog.dismiss();
            authViewModel.upgradeGuestUser(fullName, city, country);
        });
    }

    private void handleTap() {
        triggerHaptic();

        ScaleAnimation scaleAnimation = new ScaleAnimation(
                0.93f, 1.0f, 0.93f, 1.0f,
                ScaleAnimation.RELATIVE_TO_SELF, 0.5f,
                ScaleAnimation.RELATIVE_TO_SELF, 0.5f
        );
        scaleAnimation.setDuration(120);
        binding.flTapButtonContainer.startAnimation(scaleAnimation);

        counterViewModel.incrementCount();
    }

    private void triggerHaptic() {
        if (vibrator != null && vibrator.hasVibrator()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(30, VibrationEffect.DEFAULT_AMPLITUDE));
            } else {
                vibrator.vibrate(30);
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (counterViewModel != null) {
            counterViewModel.flushPendingSync();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
