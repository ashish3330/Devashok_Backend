package com.realestate.emi.dto.response;

import lombok.*;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {
    private Long id;
    private String username;
    private String fullName;
    private String mobile;
    private String role;
    private Long staffId;
    private String staffName;
    private LocalDateTime createdAt;
}
