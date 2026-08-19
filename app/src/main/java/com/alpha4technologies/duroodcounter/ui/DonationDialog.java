package com.alpha4technologies.duroodcounter.ui;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.view.LayoutInflater;

import androidx.appcompat.app.AlertDialog;

import com.alpha4technologies.duroodcounter.databinding.DialogDonationBinding;
import com.alpha4technologies.duroodcounter.model.DonationInfo;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class DonationDialog {

    public static void show(Context context, DonationInfo info) {
        if (context == null || info == null) return;

        DialogDonationBinding binding = DialogDonationBinding.inflate(LayoutInflater.from(context));

        if (info.getMessageUrdu() != null && !info.getMessageUrdu().isEmpty()) {
            binding.tvUrduMessage.setText(info.getMessageUrdu());
        }

        if (info.getBankName() != null && !info.getBankName().isEmpty()) {
            binding.tvBankName.setText(info.getBankName());
        }

        if (info.getAccountTitle() != null && !info.getAccountTitle().isEmpty()) {
            binding.tvAccountTitle.setText("Title: " + info.getAccountTitle());
        }

        String accNum = info.getAccountNumber() != null ? info.getAccountNumber() : "";
        binding.tvAccountNumber.setText(accNum);

        String iban = info.getIbanNumber() != null ? info.getIbanNumber() : "";
        binding.tvIbanNumber.setText(iban);

        binding.ibCopyAccount.setOnClickListener(v -> copyToClipboard(context, "Account Number", accNum));
        binding.ibCopyIban.setOnClickListener(v -> copyToClipboard(context, "IBAN", iban));

        AlertDialog dialog = new MaterialAlertDialogBuilder(context)
                .setView(binding.getRoot())
                .setPositiveButton("Close", (d, which) -> d.dismiss())
                .create();

        dialog.show();
    }

    private static void copyToClipboard(Context context, String label, String text) {
        if (text == null || text.isEmpty()) return;
        ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard != null) {
            ClipData clip = ClipData.newPlainText(label, text);
            clipboard.setPrimaryClip(clip);
            android.widget.Toast.makeText(context, label + " copied to clipboard!", android.widget.Toast.LENGTH_SHORT).show();
        }
    }
}
