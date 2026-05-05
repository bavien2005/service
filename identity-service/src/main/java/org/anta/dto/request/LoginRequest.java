package org.anta.dto.request;


import lombok.Data;

@Data
public class LoginRequest {

    private String name;

    private String email;

    private String password;

}
