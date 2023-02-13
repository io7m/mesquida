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
import org.apache.commons.io.output.CloseShieldOutputStream;
import org.eclipse.jetty.server.Request;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Objects;
import java.util.Set;

import static com.io7m.mesquida.internal.database.tables.Users.USERS;

/**
 * A server user list handler.
 */

public final class MPrivUserListHandler extends MPrivAuthenticatedHandler
{
  private static final Logger LOG =
    LoggerFactory.getLogger(MPrivUserListHandler.class);

  private final MDatabase database;
  private final ObjectMapper mapper;

  /**
   * Construct a handler.
   *
   * @param inConfiguration The configuration
   * @param inDatabase      The database
   */

  public MPrivUserListHandler(
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
    final var output = new MUserList();

    try (var connection = this.database.openConnection()) {
      final var context =
        DSL.using(connection, SQLDialect.DERBY);

      final var users = context.fetch(USERS);
      for (final var user : users) {
        final var outputUser = new MUserListed();
        outputUser.userId = user.getUserId().intValue();
        outputUser.userName = user.getUserName();
        outputUser.passwordAlgo = user.getUserPassAlgo();
        outputUser.passwordHash = user.getUserPassHash();
        outputUser.passwordSalt = user.getUserPassSalt();
        output.users.add(outputUser);
      }

      connection.rollback();
    } catch (final SQLException e) {
      LOG.error("database error: ", e);
      sendError(response, 500, e.getMessage());
    }

    response.setContentType("text/json");
    response.setStatus(200);
    try (var outputStream = response.getOutputStream()) {
      try (var out = CloseShieldOutputStream.wrap(outputStream)) {
        this.mapper.writeValue(out, output);
      }
      outputStream.println();
    }
  }
}
