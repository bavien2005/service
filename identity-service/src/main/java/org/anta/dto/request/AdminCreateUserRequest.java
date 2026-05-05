
package org.anta.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.anta.enums.Role;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AdminCreateUserRequest {

    private String name;

    private String email;

    private String password;

    private String phoneNumber;

    private Role role;
}