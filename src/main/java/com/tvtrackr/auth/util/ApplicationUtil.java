package com.tvtrackr.auth.util;

import static java.lang.Boolean.FALSE;

import com.tvtrackr.auth.entity.RefreshToken;

public class ApplicationUtil {
  public static boolean isValidToken(RefreshToken refreshToken) {
    return !refreshToken.isRevoked() && !refreshToken.isExpired();
  }
}
