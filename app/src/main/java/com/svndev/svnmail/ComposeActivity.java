package com.svndev.svnmail;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class ComposeActivity extends AppCompatActivity {

    private Account account;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_compose);

        account = new Account(getIntent().getLongExtra("account_id", -1),
                getIntent().getStringExtra("account_email"),
                getIntent().getStringExtra("account_password"));

        TextView tvFrom = findViewById(R.id.tvFrom);
        tvFrom.setText("From: " + account.email);

        EditText etTo = findViewById(R.id.etTo);
        EditText etCc = findViewById(R.id.etCc);
        EditText etSubject = findViewById(R.id.etSubject);
        EditText etBody = findViewById(R.id.etBody);
        Button btnSend = findViewById(R.id.btnSend);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        btnSend.setOnClickListener(v -> {
            String to = etTo.getText().toString().trim();
            if (to.isEmpty()) {
                Toast.makeText(this, "Enter a recipient", Toast.LENGTH_SHORT).show();
                return;
            }
            btnSend.setEnabled(false);
            btnSend.setText("Sending…");
            new Thread(() -> {
                try {
                    MailClient.send(account, to, etCc.getText().toString(),
                            etSubject.getText().toString(), etBody.getText().toString());
                    runOnUiThread(() -> {
                        Toast.makeText(this, "Message sent", Toast.LENGTH_SHORT).show();
                        finish();
                    });
                } catch (Exception e) {
                    runOnUiThread(() -> {
                        btnSend.setEnabled(true);
                        btnSend.setText("Send");
                        Toast.makeText(this, "Send failed: " + e.getMessage(),
                                Toast.LENGTH_LONG).show();
                    });
                }
            }).start();
        });
    }
}
