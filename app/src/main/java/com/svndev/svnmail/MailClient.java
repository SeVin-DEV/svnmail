package com.svndev.svnmail;

import java.util.Properties;

import javax.mail.Authenticator;
import javax.mail.Folder;
import javax.mail.UIDFolder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

public class MailClient {

    public static final String IMAP_HOST = "imap.nevtan.com";
    public static final int IMAP_PORT = 993;
    public static final String SMTP_HOST = "smtp.nevtan.com";
    public static final int SMTP_PORT = 465;

    public static Properties imapProps() {
        Properties p = new Properties();
        p.put("mail.store.protocol", "imaps");
        p.put("mail.imaps.host", IMAP_HOST);
        p.put("mail.imaps.port", String.valueOf(IMAP_PORT));
        p.put("mail.imaps.ssl.enable", "true");
        p.put("mail.imaps.ssl.checkserveridentity", "true");
        p.put("mail.imaps.connectiontimeout", "15000");
        p.put("mail.imaps.timeout", "15000");
        return p;
    }

    public static Properties smtpProps() {
        Properties p = new Properties();
        p.put("mail.smtp.host", SMTP_HOST);
        p.put("mail.smtp.port", String.valueOf(SMTP_PORT));
        p.put("mail.smtp.auth", "true");
        p.put("mail.smtp.ssl.enable", "true");
        p.put("mail.smtp.ssl.checkserveridentity", "true");
        p.put("mail.smtp.connectiontimeout", "15000");
        p.put("mail.smtp.timeout", "15000");
        return p;
    }

    public static Authenticator authenticator(final String email, final String password) {
        return new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(email, password);
            }
        };
    }

    public static void verify(Account account) throws MessagingException {
        Session session = Session.getInstance(imapProps(),
                authenticator(account.email, account.password));
        Store store = session.getStore("imaps");
        store.connect();
        Folder inbox = store.getFolder("INBOX");
        inbox.open(Folder.READ_ONLY);
        inbox.close(false);
        store.close();
    }

    public static class MailSummary {
        public String from;
        public String subject;
        public String preview;
        public String date;
        public boolean unread;
        public long uid;
    }

    public static MailSummary[] listMessages(Account account, String folderName,
                                             int limit) throws MessagingException {
        Session session = Session.getInstance(imapProps(),
                authenticator(account.email, account.password));
        Store store = session.getStore("imaps");
        store.connect();
        Folder folder = store.getFolder(folderName);
        folder.open(Folder.READ_ONLY);

        int total = folder.getMessageCount();
        int start = Math.max(1, total - limit + 1);
        Message[] msgs = folder.getMessages(start, total);

        MailSummary[] out = new MailSummary[msgs.length];
        for (int i = 0; i < msgs.length; i++) {
            Message m = msgs[msgs.length - 1 - i];
            MailSummary s = new MailSummary();
            try {
                s.from = m.getFrom() != null && m.getFrom().length > 0
                        ? m.getFrom()[0].toString() : "(unknown)";
                s.subject = m.getSubject() != null ? m.getSubject() : "(no subject)";
                s.preview = previewOf(m);
                s.date = formatDate(m);
                s.unread = !m.isSet(javax.mail.Flags.Flag.SEEN);
                try {
                    s.uid = ((UIDFolder) folder).getUID(m);
                } catch (Exception ignored) {
                    s.uid = 0;
                }
            } catch (Exception e) {
                s.from = "(error)"; s.subject = ""; s.preview = "";
            }
            out[i] = s;
        }
        folder.close(false);
        store.close();
        return out;
    }

    public static String readMessage(Account account, String folderName,
                                     long uid) throws Exception {
        Session session = Session.getInstance(imapProps(),
                authenticator(account.email, account.password));
        Store store = session.getStore("imaps");
        store.connect();
        Folder folder = store.getFolder(folderName);
        folder.open(Folder.READ_WRITE);
        Message m = ((UIDFolder) folder).getMessageByUID(uid);
        String body;
        if (m == null) {
            body = "Message not found.";
        } else {
            body = bodyOf(m);
            m.setFlag(javax.mail.Flags.Flag.SEEN, true);
        }
        folder.close(false);
        store.close();
        return body;
    }

    public static void send(Account account, String to, String cc, String subject,
                            String body) throws MessagingException {
        Session session = Session.getInstance(smtpProps(),
                authenticator(account.email, account.password));
        MimeMessage msg = new MimeMessage(session);
        msg.setFrom(new InternetAddress(account.email));
        msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
        if (cc != null && !cc.trim().isEmpty()) {
            msg.setRecipients(Message.RecipientType.CC, InternetAddress.parse(cc));
        }
        msg.setSubject(subject != null ? subject : "");
        msg.setText(body != null ? body : "");
        Transport.send(msg);
    }

    public static long maxUid(Account account) throws MessagingException {
        Session session = Session.getInstance(imapProps(),
                authenticator(account.email, account.password));
        Store store = session.getStore("imaps");
        store.connect();
        Folder inbox = store.getFolder("INBOX");
        inbox.open(Folder.READ_ONLY);
        long max = 0;
        if (inbox.getMessageCount() > 0) {
            try {
                Message last = inbox.getMessage(inbox.getMessageCount());
                max = ((UIDFolder) inbox).getUID(last);
            } catch (Exception ignored) {
            }
        }
        inbox.close(false);
        store.close();
        return max;
    }

    public static Message getNewestMessage(Account account) throws MessagingException {
        Session session = Session.getInstance(imapProps(),
                authenticator(account.email, account.password));
        Store store = session.getStore("imaps");
        store.connect();
        Folder inbox = store.getFolder("INBOX");
        inbox.open(Folder.READ_ONLY);
        Message m = null;
        if (inbox.getMessageCount() > 0) {
            m = inbox.getMessage(inbox.getMessageCount());
        }
        inbox.close(false);
        store.close();
        return m;
    }

    private static String previewOf(Message m) throws Exception {
        String text = bodyOf(m);
        text = text.replaceAll("\\s+", " ").trim();
        return text.length() > 90 ? text.substring(0, 90) + "…" : text;
    }

    private static String formatDate(Message m) throws Exception {
        java.util.Date d = m.getSentDate();
        if (d == null) return "";
        java.text.SimpleDateFormat f =
                new java.text.SimpleDateFormat("MMM d, h:mm a", java.util.Locale.getDefault());
        return f.format(d);
    }

    private static String bodyOf(Message m) throws Exception {
        Object content = m.getContent();
        if (content instanceof String) {
            return (String) content;
        }
        if (content instanceof javax.mail.Multipart) {
            return extractFromMultipart((javax.mail.Multipart) content);
        }
        return String.valueOf(content);
    }

    private static String extractFromMultipart(javax.mail.Multipart mp) throws Exception {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < mp.getCount(); i++) {
            javax.mail.BodyPart part = mp.getBodyPart(i);
            Object c = part.getContent();
            if (c instanceof String) {
                sb.append(c).append("\n\n");
            } else if (c instanceof javax.mail.Multipart) {
                sb.append(extractFromMultipart((javax.mail.Multipart) c));
            }
        }
        return sb.toString().trim();
    }
}
