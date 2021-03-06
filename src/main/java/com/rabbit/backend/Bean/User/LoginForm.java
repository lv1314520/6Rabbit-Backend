package com.rabbit.backend.Bean.User;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class LoginForm {

    @NotBlank
    private String username;

    @NotBlank
    private String password;

    @NotBlank(message = "Pass captcha first!")
    private String token;
}
