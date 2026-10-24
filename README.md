# svnmail

Android multi-account webmail client (`com.svndev.svnmail`).

- IMAP SSL: smtp.nevtan.com:993 (same host as SMTP; imap.nevtan.com does not exist)
- SMTP SSL: smtp.nevtan.com:465
- Multi-account SQLite + foreground poll / notifications
- AGP 8.1.4 · compileSdk 34 · minSdk 24

## Build
```bash
chmod +x gradlew
./gradlew assembleDebug
```
