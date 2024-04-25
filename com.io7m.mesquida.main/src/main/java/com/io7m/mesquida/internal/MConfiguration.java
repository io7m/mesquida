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

import com.io7m.jproperties.JProperties;
import com.io7m.jproperties.JPropertyException;
import com.io7m.mesquida.internal.database.MDatabaseConfiguration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Objects;
import java.util.Properties;

/**
 * The main configuration.
 *
 * @param http     The HTTP server configuration
 * @param database The database configuration
 */

public record MConfiguration(
  MServerConfiguration http,
  MDatabaseConfiguration database)
{
  /**
   * The main configuration.
   *
   * @param http     The HTTP server configuration
   * @param database The database configuration
   */

  public MConfiguration
  {
    Objects.requireNonNull(http, "http");
    Objects.requireNonNull(database, "database");
  }

  /**
   * Open the given properties file as a configuration.
   *
   * @param file The file
   *
   * @return A configuration
   *
   * @throws IOException        On errors
   * @throws JPropertyException On errors
   */

  public static MConfiguration open(
    final Path file)
    throws IOException, JPropertyException
  {
    final var fs = file.getFileSystem();

    try (var stream = Files.newInputStream(file)) {
      final var properties = new Properties();
      properties.load(stream);

      final var privateAddr =
        JProperties.getInetAddress(properties, "server.privateAddress");
      final var privatePort =
        JProperties.getInteger(properties, "server.privatePort");
      final var publicAddr =
        JProperties.getInetAddress(properties, "server.publicAddress");
      final var publicPort =
        JProperties.getInteger(properties, "server.publicPort");
      final var privateToken =
        JProperties.getString(properties, "server.privateToken");
      final var sessionDirectory =
        JProperties.getString(properties, "server.sessionPath");
      final var database =
        JProperties.getString(properties, "database.path");
      final var databaseCreate =
        JProperties.getBooleanWithDefault(properties, "database.create", true);
      final var databaseUpgrade =
        JProperties.getBooleanWithDefault(properties, "database.upgrade", true);

      return new MConfiguration(
        new MServerConfiguration(
          Locale.getDefault(),
          privateAddr,
          privatePort,
          publicAddr,
          publicPort,
          privateToken,
          fs.getPath(sessionDirectory)
        ),
        new MDatabaseConfiguration(
          fs.getPath(database),
          databaseUpgrade,
          databaseCreate
        )
      );
    }
  }
}
