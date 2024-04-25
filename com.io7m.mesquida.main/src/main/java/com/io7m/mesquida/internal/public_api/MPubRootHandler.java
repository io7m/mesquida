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
import org.apache.commons.text.StringEscapeUtils;
import org.jooq.impl.DSL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.io7m.mesquida.internal.database.Tables.STREAMS;
import static com.io7m.mesquida.internal.database.Tables.STREAM_ADDRESSES;
import static com.io7m.mesquida.internal.database.Tables.USERS;
import static org.jooq.SQLDialect.DERBY;

/**
 * A server root handler.
 */

public final class MPubRootHandler extends MPubAuthenticatedHandler
{
  private static final Logger LOG =
    LoggerFactory.getLogger(MPubRootHandler.class);

  /**
   * Construct a handler.
   *
   * @param inPages    The pages
   * @param inDatabase The database
   */

  public MPubRootHandler(
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
        final var context =
          DSL.using(connection, DERBY);

        final var streams =
          context.select()
            .from(STREAMS)
            .join(USERS)
            .on(STREAMS.STREAM_OWNER.eq(USERS.USER_ID))
            .where(USERS.USER_NAME.eq(this.userName()))
            .fetch();

        final var content = new StringBuilder(256);
        content.append("<h3>Streams</h3>");
        content.append('\n');

        for (final var record : streams) {
          final var stream = record.into(STREAMS);
          content.append("<div class=\"stream\">");

          final var addresses =
            context.select()
              .from(STREAM_ADDRESSES)
              .where(STREAM_ADDRESSES.STREAM_REFERENCE.eq(stream.getStreamId()))
              .fetch();

          content.append(
            this.pages()
              .streamForm(
                StringEscapeUtils.escapeXml11(stream.getStreamTitle()),
                StringEscapeUtils.escapeXml11(stream.getStreamName()))
          );

          if (addresses.isNotEmpty()) {
            content.append("<div class=\"streamLinks\">");
            content.append("Links");
            content.append("<ul class=\"streamLinkList\">");
            for (final var address : addresses) {
              final var addressT = address.into(STREAM_ADDRESSES);
              content.append("<li>");
              content.append("<a href=\"");
              content.append(addressT.getStreamUrl());
              content.append("\">");
              content.append(addressT.getStreamUrl());
              content.append("</a>");
              content.append("</li>");
            }
            content.append("</ul>");
            content.append("</div>");
          }

          content.append("</div>");
          content.append('\n');
        }

        this.pages()
          .sendPage(
            servletResponse,
            200,
            this.pages().mainPage("Mesquida", content.toString())
          );

      } finally {
        connection.rollback();
      }
    }
  }
}
