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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.io7m.mesquida.internal.MJson;
import com.io7m.mesquida.internal.MServerConfiguration;
import com.io7m.mesquida.internal.database.MDatabase;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.eclipse.jetty.server.Request;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.sql.SQLException;
import java.util.HexFormat;
import java.util.Objects;
import java.util.Set;

import static com.io7m.mesquida.internal.database.tables.Users.USERS;

/**
 * A server user create/update handler.
 */

public final class MPrivUserPutHandler extends MPrivAuthenticatedHandler
{
  private static final Logger LOG =
    LoggerFactory.getLogger(MPrivUserPutHandler.class);

  private final MDatabase database;
  private final ObjectMapper mapper;

  /**
   * Construct a handler.
   *
   * @param inConfiguration The configuration
   * @param inDatabase      The database
   */

  public MPrivUserPutHandler(
    final MServerConfiguration inConfiguration,
    final MDatabase inDatabase)
  {
    super(inConfiguration);

    this.database =
      Objects.requireNonNull(inDatabase, "inDatabase");

    this.mapper =
      MJson.createMapper(
        Set.of(
          String.class.getCanonicalName(),
          MUserPut.class.getCanonicalName())
      );
  }

  @Override
  protected void handleAuthenticated(
    final String target,
    final Request baseRequest,
    final HttpServletRequest request,
    final HttpServletResponse response)
    throws IOException
  {
    final MUserPut command;
    try (var stream = baseRequest.getInputStream()) {
      command = this.mapper.readValue(stream, MUserPut.class);
    }
    command.validate();

    try (var connection = this.database.openConnection()) {
      final var context =
        DSL.using(connection, SQLDialect.DERBY);

      var existing =
        context.fetchOne(USERS, USERS.USER_NAME.eq(command.user));

      if (existing == null) {
        existing = context.newRecord(USERS);
        existing.setUserName(command.user);
      }

      final var random = SecureRandom.getInstanceStrong();
      final var salt = new byte[32];
      random.nextBytes(salt);

      final var formatter =
        HexFormat.of();
      final var passwordSalt =
        formatter.formatHex(salt);
      final var keyFactory =
        SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
      final var keySpec =
        new PBEKeySpec(command.password.toCharArray(), salt, 10000, 256);
      final var hash =
        keyFactory.generateSecret(keySpec).getEncoded();
      final var passwordHash =
        formatter.formatHex(hash);

      existing.setUserPassHash(passwordHash);
      existing.setUserPassAlgo("PBKDF2WithHmacSHA256:10000");
      existing.setUserPassSalt(passwordSalt);
      existing.store();

      connection.commit();
    } catch (final NoSuchAlgorithmException e) {
      LOG.error("no such algorithm: ", e);
      MPrivAuthenticatedHandler.sendError(response, 500, e.getMessage());
    } catch (final InvalidKeySpecException e) {
      LOG.error("invalid key spec: ", e);
      MPrivAuthenticatedHandler.sendError(response, 500, e.getMessage());
    } catch (final SQLException e) {
      LOG.error("database error: ", e);
      MPrivAuthenticatedHandler.sendError(response, 500, e.getMessage());
    }

    response.setContentType("text/plain");
    response.setStatus(200);
    try (var outputStream = response.getOutputStream()) {
      outputStream.println("OK");
      outputStream.flush();
    }
  }
}
