package com.tvtrackr.auth.dto.req;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequest {

  @NotBlank(message = "Email or username is required")
  @Size(min = 3, max = 254, message = "Data too long")
  private String emailOrUsername;

  @NotBlank(message = "Password is required")
  private String password;
}
