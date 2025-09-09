package com.nelumbo.park.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserLoginResponse {
    private String id;
    private String username;
    private String email;
    private String role;
    private String tokenType;
    private String accessToken;
}
