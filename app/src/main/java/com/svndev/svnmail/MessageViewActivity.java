package com.svndev.svnmail;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class MessageViewActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_message);

        Account account = new Account(getIntent().getLongExtra("account_id", -1),
                getIntent().getStringExtra("account_email"),
                getIntent().getStringExtra("account_password"));
        String folder = getIntent().getStringExtra("folder");
        long uid = getIntent().getLongExtra("uid", 0);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        TextView body = findViewById(R.id.tvBody);
        body.setText("Loading…");

        new Thread(() -> {
            try {
                String text = MailClient.readMessage(account, folder, uid);
                runOnUiThread(() -> body.setText(text));
            } catch (Exception e) {
                runOnUiThread(() -> body.setText("Could not load message: " + e.getMessage()));
            }
        }).start();
    }
}
