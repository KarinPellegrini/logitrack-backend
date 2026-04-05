package com.logitrack.logitrack_api.dto;

import lombok.Data;

@Data
public class ResetPasswordRequestDTO {
    private String token;
    private String nuevaClave;
}
