package com.placement.campus_placement_system.auth.dto.response;

import com.placement.campus_placement_system.auth.enums.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Response payload returned after a successful login, containing user details and a signed JWT.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthResponse {

    private Long userId;
    private String name;
    private String email;
    private Role role;
    private String token;

    @Builder.Default
    private String tokenType = "Bearer";
}
