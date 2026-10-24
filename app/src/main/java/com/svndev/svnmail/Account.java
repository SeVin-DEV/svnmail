package com.svndev.svnmail;

public class Account {
    public long id;
    public String email;
    public String password;

    public Account(long id, String email, String password) {
        this.id = id;
        this.email = email;
        this.password = password;
    }
}
