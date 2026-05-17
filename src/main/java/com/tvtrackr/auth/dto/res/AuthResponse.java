package com.tvtrackr.auth.dto.res;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class AuthResponse {
  private String accessToken;
  private String uuid;
  private String email;
  private String username;
}
