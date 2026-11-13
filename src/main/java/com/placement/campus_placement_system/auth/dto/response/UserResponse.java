package com.placement.campus_placement_system.auth.dto.response;

import com.placement.campus_placement_system.auth.enums.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Safe public user representation without sensitive fields such as password hashes.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserResponse {

    private Long userId;
    private String name;
    private String email;
    private Role role;
}
