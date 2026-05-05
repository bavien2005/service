package org.anta.dto.response;

import org.anta.enums.Role;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class UserResponse {

    private Long id ;

    private String name;

    private String password;

    private String email;

    private Role role;

    private String phoneNumber;

    private LocalDateTime createdAt;


    private LocalDate workStartDate;   // thêm
}
