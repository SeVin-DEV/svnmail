package com.svndev.svnmail;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class LoginActivity extends AppCompatActivity {

    private EditText etEmail, etPassword;
    private ProgressBar progress;
    private TextView tvError;
    private DatabaseHelper db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        db = new DatabaseHelper(this);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        progress = findViewById(R.id.progress);
        tvError = findViewById(R.id.tvError);
        Button btn = findViewById(R.id.btnSignIn);

        btn.setOnClickListener(v -> attemptLogin());
    }

    private void attemptLogin() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString();
        if (email.isEmpty() || !email.contains("@")) {
            showError("Enter a valid email address.");
            return;
        }
        if (password.isEmpty()) {
            showError("Enter your password.");
            return;
        }
        setBusy(true);

        new Thread(() -> {
            try {
                MailClient.verify(new Account(-1, email, password));
                long id = db.addAccount(email, password);
                boolean alreadyExists = (id == -1);
                runOnUiThread(() -> {
                    Toast.makeText(this, "Account connected", Toast.LENGTH_SHORT).show();
                    new Thread(() -> {
                        try {
                            long realId = id;
                            if (alreadyExists) {
                                for (Account a : db.getAccounts()) {
                                    if (a.email.equalsIgnoreCase(email)) realId = a.id;
                                }
                            }
                            db.setLastUid(realId, MailClient.maxUid(
                                    new Account(realId, email, password)));
                        } catch (Exception ignored) {
                        }
                    }).start();
                    startService(new android.content.Intent(this, MailPollService.class));
                    finish();
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    setBusy(false);
                    showError("Sign-in failed: " + e.getMessage());
                });
            }
        }).start();
    }

    private void setBusy(boolean busy) {
        progress.setVisibility(busy ? View.VISIBLE : View.GONE);
        tvError.setVisibility(View.GONE);
    }

    private void showError(String msg) {
        tvError.setText(msg);
        tvError.setVisibility(View.VISIBLE);
    }
}
