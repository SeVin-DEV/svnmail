package com.svndev.svnmail;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    private DatabaseHelper db;
    private ListView lvAccounts;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        db = new DatabaseHelper(this);
        lvAccounts = findViewById(R.id.lvAccounts);
        findViewById(R.id.btnAdd).setOnClickListener(v ->
                startActivity(new Intent(this, LoginActivity.class)));

        startService(new Intent(this, MailPollService.class));
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadAccounts();
    }

    private void loadAccounts() {
        List<Account> accounts = db.getAccounts();
        ArrayAdapter<Account> adapter = new ArrayAdapter<Account>(this, 0, accounts) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                if (convertView == null) {
                    convertView = LayoutInflater.from(MainActivity.this)
                            .inflate(R.layout.item_account, parent, false);
                }
                Account a = getItem(position);
                TextView avatar = convertView.findViewById(R.id.tvAvatar);
                TextView email = convertView.findViewById(R.id.tvEmail);
                TextView status = convertView.findViewById(R.id.tvStatus);
                avatar.setText(a.email.substring(0, 1).toUpperCase());
                email.setText(a.email);
                status.setText("IMAP 993 SSL · SMTP 465 SSL");
                convertView.findViewById(R.id.btnRemove).setOnClickListener(v ->
                        confirmRemove(a));
                convertView.setOnClickListener(v -> openInbox(a));
                return convertView;
            }
        };
        lvAccounts.setAdapter(adapter);
        if (accounts.isEmpty()) {
            startActivity(new Intent(this, LoginActivity.class));
        }
    }

    private void openInbox(Account a) {
        Intent i = new Intent(this, InboxActivity.class);
        i.putExtra("account_id", a.id);
        i.putExtra("account_email", a.email);
        i.putExtra("account_password", a.password);
        startActivity(i);
    }

    private void confirmRemove(Account a) {
        new AlertDialog.Builder(this)
                .setTitle("Remove account?")
                .setMessage(a.email + " will be removed from this device.")
                .setPositiveButton("Remove", (d, w) -> {
                    db.removeAccount(a.id);
                    Toast.makeText(this, "Account removed", Toast.LENGTH_SHORT).show();
                    loadAccounts();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
