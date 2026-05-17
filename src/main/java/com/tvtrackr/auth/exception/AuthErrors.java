package com.tvtrackr.auth.exception;

import com.tvtrackr.common.error.BusinessErrors;

public class AuthErrors extends BusinessErrors {
  public static final AuthErrors EMAIL_ALREADY_EXISTS =
      new AuthErrors("1000", "Email already in use", 409);
  public static final AuthErrors USERNAME_ALREADY_EXISTS =
      new AuthErrors("1001", "Username already in use", 409);
  public static final AuthErrors USER_NOT_FOUND =
      new AuthErrors("1002", "User not found", 400);
  public static final AuthErrors INVALID_CREDENTIALS =
      new AuthErrors("1003", "Invalid credentials", 403);
  public static final AuthErrors INVALID_REFRESH_TOKEN =
      new AuthErrors("1004", "Invalid refresh token", 400);

  protected AuthErrors(String code, String desc, int httpStatus) {
    super(code, desc, httpStatus);
    this.prefix = "AU";
    this.code = prefix + code;
  }
}
