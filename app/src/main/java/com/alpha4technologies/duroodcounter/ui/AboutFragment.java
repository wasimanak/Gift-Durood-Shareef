package com.alpha4technologies.duroodcounter.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.alpha4technologies.duroodcounter.BuildConfig;
import com.alpha4technologies.duroodcounter.databinding.FragmentAboutBinding;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class AboutFragment extends Fragment {

    private FragmentAboutBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentAboutBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.tvAppVersion.setText("Version " + BuildConfig.VERSION_NAME);

        binding.btnPrivacyPolicy.setOnClickListener(v -> showPrivacyPolicyDialog());
        binding.btnWhatsapp.setOnClickListener(v ->
                openUrl("https://wa.me/923477442050?text=Assalamu%20Alaikum!%20Reciting%20Durood%20Pak%20on%20Durood%20Counter%20App.")
        );        binding.btnInstagram.setOnClickListener(v -> openUrl("https://instagram.com/"));
        binding.btnFacebook.setOnClickListener(v -> openUrl("https://facebook.com/"));
    }

    private void showPrivacyPolicyDialog() {
        String privacyPolicyText =
                "Privacy Policy for Durood Counter\n\n" +
                "Effective Date: 2026\n\n" +
                "1. Data Collection & Usage:\n" +
                "• Durood Counter allows optional user profile creation (Full Name & City) solely to display reciter standings on the Community Leaderboard.\n" +
                "• Your daily and overall recitation counts are securely saved using Firebase Realtime Database.\n" +
                "• Guest Mode allows you to use the app anonymously without storing personal details.\n\n" +
                "2. Data Security & Encryption:\n" +
                "• All communications between the app and servers are strictly encrypted over SSL/HTTPS protocols.\n\n" +
                "3. Third-Party Sharing:\n" +
                "• We do NOT sell, trade, or share any user personal information with third parties or advertisers.\n\n" +
                "4. App Permissions:\n" +
                "• The app requires basic Network Access (INTERNET, ACCESS_NETWORK_STATE) for cloud sync and VIBRATE for haptic counter feedback. No storage, location, or contact access is requested.\n\n" +
                "5. Contact Us:\n" +
                "• For any privacy questions or data deletion requests, contact developer PKSofter.";

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Privacy Policy")
                .setMessage(privacyPolicyText)
                .setPositiveButton("Close", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void openUrl(String url) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(getContext(), "Unable to open link", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
