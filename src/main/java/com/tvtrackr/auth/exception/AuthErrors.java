package com.tvtrackr.auth.exception;

import com.tvtrackr.common.error.BusinessErrors;

public class AuthErrors extends BusinessErrors {
  public static final AuthErrors EMAIL_ALREADY_EXISTS =
      new AuthErrors("1000", "Email already in use", 409);
  public static final AuthErrors USERNAME_ALREADY_EXISTS =
      new AuthErrors("1001", "Username already in use", 409);
  public static final AuthErrors EMAIL_OR_USERNAME_ALREADY_EXISTS =
      new AuthErrors("1002", "Username or email already in use", 409);
  public static final AuthErrors USER_NOT_FOUND = new AuthErrors("1003", "User not found", 400);
  public static final AuthErrors INVALID_CREDENTIALS =
      new AuthErrors("1004", "Invalid credentials", 401);
  public static final AuthErrors INVALID_TOKEN = new AuthErrors("1005", "Invalid token", 400);
  public static final AuthErrors PASSWORD_RESET_NOT_SUPPORTED =
      new AuthErrors("1006", "Password reset not supported for this account", 400);
  public static final AuthErrors EMAIL_SEND_FAILED =
      new AuthErrors("1007", "Failed to send email", 500);
  public static final AuthErrors TOO_MANY_REQUESTS =
      new AuthErrors("1008", "Too many requests. Please wait before sending more requests", 429);

  protected AuthErrors(String code, String desc, int httpStatus) {
    super(code, desc, httpStatus);
    this.prefix = "AU";
    this.code = prefix + code;
  }
}
