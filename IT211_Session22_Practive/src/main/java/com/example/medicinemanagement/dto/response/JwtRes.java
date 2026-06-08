package com.example.medicinemanagement.dto.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JwtRes {

    private String fullName;

    private String accessToken;

    private String refreshToken;

    private String role;
}
