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


package com.io7m.mesquida.internal;

import java.net.InetAddress;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Objects;

/**
 * The web server configuration.
 *
 * @param locale             The current locale
 * @param privateAddress     The address of the private REST API
 * @param privatePort        The port used for the private REST API
 * @param publicAddress      The address used for the public REST API
 * @param publicPort         The port used for the public REST API
 * @param serverPrivateToken The token used to access the private API
 * @param sessionDirectory   The session directory for the server
 */

public record MServerConfiguration(
  Locale locale,
  InetAddress privateAddress,
  int privatePort,
  InetAddress publicAddress,
  int publicPort,
  String serverPrivateToken,
  Path sessionDirectory)
{
  /**
   * The web server configuration.
   *
   * @param locale             The current locale
   * @param privateAddress     The address of the private REST API
   * @param privatePort        The port used for the private REST API
   * @param publicAddress      The address used for the public REST API
   * @param publicPort         The port used for the public REST API
   * @param serverPrivateToken The token used to access the private API
   * @param sessionDirectory   The session directory for the server
   */

  public MServerConfiguration
  {
    Objects.requireNonNull(locale, "locale");
    Objects.requireNonNull(privateAddress, "privateAddress");
    Objects.requireNonNull(publicAddress, "publicAddress");
    Objects.requireNonNull(serverPrivateToken, "serverPrivateToken");
    Objects.requireNonNull(sessionDirectory, "sessionDirectory");
  }
}
