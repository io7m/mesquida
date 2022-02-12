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

import java.io.IOException;
import java.net.URI;
import java.sql.SQLException;
import java.util.Objects;
import java.util.Set;

import static com.io7m.mesquida.internal.database.Tables.STREAM_ADDRESSES;

/**
 * A server stream address create/update handler.
 */

public final class MPrivStreamAddressPutHandler extends
  MPrivAuthenticatedHandler
{
  private static final Logger LOG =
    LoggerFactory.getLogger(MPrivStreamAddressPutHandler.class);

  private final MDatabase database;
  private final ObjectMapper mapper;

  /**
   * Construct a handler.
   *
   * @param inConfiguration The configuration
   * @param inDatabase      The database
   */

  public MPrivStreamAddressPutHandler(
    final MServerConfiguration inConfiguration,
    final MDatabase inDatabase)
  {
    super(inConfiguration);

    this.database =
      Objects.requireNonNull(inDatabase, "inDatabase");

    this.mapper =
      MJson.createMapper(
        Set.of(
          int.class.getCanonicalName(),
          String.class.getCanonicalName(),
          URI.class.getCanonicalName(),
          MStreamAddressPut.class.getCanonicalName())
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
    final MStreamAddressPut command;
    try (var stream = baseRequest.getInputStream()) {
      command = this.mapper.readValue(stream, MStreamAddressPut.class);
    }

    try (var connection = this.database.openConnection()) {
      final var context =
        DSL.using(connection, SQLDialect.DERBY);

      var existing =
        context.fetchOne(
          STREAM_ADDRESSES,
          STREAM_ADDRESSES.STREAM_REFERENCE.eq(command.streamId)
            .and(STREAM_ADDRESSES.STREAM_PROTOCOL.eq(command.protocol))
        );

      if (existing == null) {
        existing = context.newRecord(STREAM_ADDRESSES);
        existing.setStreamReference(command.streamId);
      }

      existing.setStreamProtocol(command.protocol);
      existing.setStreamUrl(command.url.toString());
      existing.store();

      connection.commit();
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
