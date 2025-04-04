/*
 * Copyright © 2022 Mark Raynsford <code@io7m.com> https://www.io7m.com
 *
 * Permission to use, copy, modify, and/or distribute this software for any
 * purpose with or without fee is hereby granted, provided that the above
 * copyright notice and this permission notice appear in all copies.
 *
 * THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES
 * WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY
 * SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES
 * WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN
 * ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR
 * IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.
 */


package com.io7m.mesquida.internal.private_api;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.util.regex.Pattern;

// CHECKSTYLE:OFF

@JsonDeserialize
@JsonSerialize
final class MUserPut
{
  private static final Pattern VALID_USER_NAME =
    Pattern.compile("[a-z0-9_]{2,32}");

  @JsonProperty(required = true, value = "user")
  public String user;
  @JsonProperty(required = true, value = "password")
  public String password;

  MUserPut()
  {

  }

  void validate()
  {
    final var matcher = VALID_USER_NAME.matcher(this.user);
    if (!matcher.matches()) {
      throw new IllegalArgumentException(
        "Invalid user name. Must match: " + VALID_USER_NAME);
    }
  }
}
