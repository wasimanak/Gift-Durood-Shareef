package com.alpha4technologies.duroodcounter.model;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.IgnoreExtraProperties;

@IgnoreExtraProperties
public class DonationInfo {
    private boolean showDialog;
    private String bankName;
    private String accountTitle;
    private String accountNumber;
    private String ibanNumber;
    private String messageUrdu;

    public DonationInfo() {
        // Required empty constructor for Firebase
    }

    public DonationInfo(boolean showDialog, String bankName, String accountTitle, String accountNumber, String ibanNumber, String messageUrdu) {
        this.showDialog = showDialog;
        this.bankName = bankName;
        this.accountTitle = accountTitle;
        this.accountNumber = accountNumber;
        this.ibanNumber = ibanNumber;
        this.messageUrdu = messageUrdu;
    }

    public static DonationInfo fromSnapshot(DataSnapshot snapshot) {
        if (snapshot == null || !snapshot.exists()) return null;

        DonationInfo info = new DonationInfo();

        Object showObj = snapshot.child("show_dialog").exists()
                ? snapshot.child("show_dialog").getValue()
                : snapshot.child("showDialog").getValue();
        info.setShowDialog(parseBoolean(showObj));

        info.setBankName(parseString(snapshot.child("bank_name"), snapshot.child("bankName")));
        info.setAccountTitle(parseString(snapshot.child("account_title"), snapshot.child("accountTitle")));
        info.setAccountNumber(parseString(snapshot.child("account_number"), snapshot.child("accountNumber")));
        info.setIbanNumber(parseString(snapshot.child("iban_number"), snapshot.child("ibanNumber")));
        info.setMessageUrdu(parseString(snapshot.child("message_urdu"), snapshot.child("messageUrdu")));

        return info;
    }

    private static boolean parseBoolean(Object val) {
        if (val == null) return false;
        if (val instanceof Boolean) return (Boolean) val;
        if (val instanceof String) {
            String s = ((String) val).trim().toLowerCase();
            return "true".equals(s) || "1".equals(s) || "yes".equals(s);
        }
        if (val instanceof Number) {
            return ((Number) val).intValue() != 0;
        }
        return false;
    }

    private static String parseString(DataSnapshot snap1, DataSnapshot snap2) {
        if (snap1 != null && snap1.exists() && snap1.getValue() != null) {
            return String.valueOf(snap1.getValue());
        }
        if (snap2 != null && snap2.exists() && snap2.getValue() != null) {
            return String.valueOf(snap2.getValue());
        }
        return "";
    }

    public boolean isShowDialog() {
        return showDialog;
    }

    public void setShowDialog(boolean showDialog) {
        this.showDialog = showDialog;
    }

    public void setShowDialog(Object val) {
        this.showDialog = parseBoolean(val);
    }

    public String getBankName() {
        return bankName;
    }

    public void setBankName(String bankName) {
        this.bankName = bankName;
    }

    public String getAccountTitle() {
        return accountTitle;
    }

    public void setAccountTitle(String accountTitle) {
        this.accountTitle = accountTitle;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }

    public String getIbanNumber() {
        return ibanNumber;
    }

    public void setIbanNumber(String ibanNumber) {
        this.ibanNumber = ibanNumber;
    }

    public String getMessageUrdu() {
        return messageUrdu;
    }

    public void setMessageUrdu(String messageUrdu) {
        this.messageUrdu = messageUrdu;
    }
}
