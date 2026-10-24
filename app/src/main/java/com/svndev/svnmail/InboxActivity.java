package com.svndev.svnmail;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

public class InboxActivity extends AppCompatActivity {

    private Account account;
    private String folder = "INBOX";
    private ListView lvMails;
    private TextView tvEmpty;
    private SwipeRefreshLayout refresh;
    private MailClient.MailSummary[] summaries = new MailClient.MailSummary[0];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inbox);

        long id = getIntent().getLongExtra("account_id", -1);
        account = new Account(id,
                getIntent().getStringExtra("account_email"),
                getIntent().getStringExtra("account_password"));

        lvMails = findViewById(R.id.lvMails);
        tvEmpty = findViewById(R.id.tvEmpty);
        refresh = findViewById(R.id.refresh);
        TextView tvAccount = findViewById(R.id.tvAccount);
        tvAccount.setText(account.email);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnCompose).setOnClickListener(v -> {
            Intent i = new Intent(this, ComposeActivity.class);
            i.putExtra("account_id", account.id);
            i.putExtra("account_email", account.email);
            i.putExtra("account_password", account.password);
            startActivity(i);
        });

        Button tabInbox = findViewById(R.id.tabInbox);
        Button tabSent = findViewById(R.id.tabSent);
        Button tabDrafts = findViewById(R.id.tabDrafts);
        Button tabTrash = findViewById(R.id.tabTrash);
        tabInbox.setOnClickListener(v -> switchFolder("INBOX", tabInbox));
        tabSent.setOnClickListener(v -> switchFolder("Sent", tabSent));
        tabDrafts.setOnClickListener(v -> switchFolder("Drafts", tabDrafts));
        tabTrash.setOnClickListener(v -> switchFolder("Trash", tabTrash));

        refresh.setOnRefreshListener(this::load);
        refresh.setColorSchemeColors(Color.parseColor("#FF2E88"));

        lvMails.setOnItemClickListener((parent, view, position, rowId) -> {
            MailClient.MailSummary s = summaries[position];
            Intent i = new Intent(this, MessageViewActivity.class);
            i.putExtra("account_id", account.id);
            i.putExtra("account_email", account.email);
            i.putExtra("account_password", account.password);
            i.putExtra("folder", folder);
            i.putExtra("uid", s.uid);
            startActivity(i);
        });

        load();
    }

    private void switchFolder(String f, Button selected) {
        folder = f;
        for (Button b : new Button[]{
                findViewById(R.id.tabInbox), findViewById(R.id.tabSent),
                findViewById(R.id.tabDrafts), findViewById(R.id.tabTrash)}) {
            b.setBackgroundResource(R.drawable.tab_unselected);
            b.setTextColor(Color.parseColor("#8B93B8"));
        }
        selected.setBackgroundResource(R.drawable.tab_selected);
        selected.setTextColor(Color.parseColor("#F2F4FF"));
        load();
    }

    private void load() {
        refresh.setRefreshing(true);
        new Thread(() -> {
            try {
                summaries = MailClient.listMessages(account, folder, 50);
                runOnUiThread(() -> {
                    refresh.setRefreshing(false);
                    tvEmpty.setVisibility(summaries.length == 0 ? View.VISIBLE : View.GONE);
                    render();
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    refresh.setRefreshing(false);
                    Toast.makeText(this, "Failed to load: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }

    private void render() {
        ArrayAdapter<MailClient.MailSummary> adapter =
                new ArrayAdapter<MailClient.MailSummary>(this, 0, summaries) {
                    @Override
                    public View getView(int position, View convertView, ViewGroup parent) {
                        if (convertView == null) {
                            convertView = LayoutInflater.from(InboxActivity.this)
                                    .inflate(R.layout.item_mail, parent, false);
                        }
                        MailClient.MailSummary s = getItem(position);
                        TextView av = convertView.findViewById(R.id.tvFromAvatar);
                        TextView from = convertView.findViewById(R.id.tvFrom);
                        TextView subj = convertView.findViewById(R.id.tvSubject);
                        TextView prev = convertView.findViewById(R.id.tvPreview);
                        TextView date = convertView.findViewById(R.id.tvDate);
                        View dot = convertView.findViewById(R.id.dotUnread);

                        String name = s.from.contains("<")
                                ? s.from.substring(0, s.from.indexOf("<")).trim()
                                : s.from;
                        av.setText(name.isEmpty() ? "?" : name.substring(0, 1).toUpperCase());
                        from.setText(name);
                        subj.setText(s.subject);
                        prev.setText(s.preview);
                        date.setText(s.date);
                        dot.setVisibility(s.unread ? View.VISIBLE : View.INVISIBLE);
                        return convertView;
                    }
                };
        lvMails.setAdapter(adapter);
    }
}
