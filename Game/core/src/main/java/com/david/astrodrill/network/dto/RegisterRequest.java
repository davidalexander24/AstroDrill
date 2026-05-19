package com.david.astrodrill.network.dto;

public class RegisterRequest {
    public String username;
    public String password;

    public RegisterRequest() {}

    public RegisterRequest(String username, String password) {
        this.username = username;
        this.password = password;
    }
}
