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

package com.io7m.mesquida.internal.public_api;

import com.io7m.mesquida.internal.database.MDatabase;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.jooq.impl.DSL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import static com.io7m.mesquida.internal.database.Tables.STREAMS;
import static com.io7m.mesquida.internal.database.Tables.USERS;
import static org.jooq.SQLDialect.DERBY;

/**
 * A server root handler.
 */

public final class MPubStreamEditHandler extends MPubAuthenticatedHandler
{
  private static final Logger LOG =
    LoggerFactory.getLogger(MPubStreamEditHandler.class);

  /**
   * Construct a handler.
   *
   * @param inPages    The pages
   * @param inDatabase The database
   */

  public MPubStreamEditHandler(
    final MPubPages inPages,
    final MDatabase inDatabase)
  {
    super(inPages, inDatabase);
  }

  @Override
  protected Logger logger()
  {
    return LOG;
  }

  @Override
  protected void serviceAuthenticated(
    final HttpServletRequest request,
    final HttpServletResponse servletResponse,
    final HttpSession session)
    throws Exception
  {
    try (var connection = this.database().openConnection()) {
      try {
        final var streamName =
          request.getParameter("streamName");
        final var streamTitle =
          request.getParameter("streamTitle");

        if (streamName == null || streamTitle == null) {
          this.validationError(servletResponse);
          return;
        }

        if (streamTitle.length() >= 256) {
          this.validationError(servletResponse);
          return;
        }

        final var context =
          DSL.using(connection, DERBY);

        final var stream =
          context.select()
            .from(STREAMS)
            .join(USERS)
            .on(STREAMS.STREAM_OWNER.eq(USERS.USER_ID))
            .where(USERS.USER_NAME.eq(this.userName()))
            .and(STREAMS.STREAM_NAME.eq(streamName))
            .fetchOne();

        final var streamT = stream.into(STREAMS);
        streamT.setStreamTitle(streamTitle);
        streamT.store();
        connection.commit();

        LOG.info("updated stream '{}' title to '{}'", streamName, streamTitle);
        servletResponse.sendRedirect("/");
      } finally {
        connection.rollback();
      }
    }
  }

  private void validationError(final HttpServletResponse servletResponse)
    throws IOException
  {
    servletResponse.setContentType("text/plain");
    servletResponse.setStatus(400);

    try (var outputStream = servletResponse.getOutputStream()) {
      outputStream.print(400);
      outputStream.print(' ');
      outputStream.println("Missing or invalid parameters.");
      outputStream.flush();
    }
  }
}
