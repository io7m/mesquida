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


package com.io7m.mesquida.internal.mq;

import com.io7m.junreachable.UnreachableCodeException;
import org.apache.commons.text.StringEscapeUtils;

import java.net.URI;
import java.util.stream.Collectors;

/**
 * Functions to format messages.
 */

public final class MMessageFormatter
{
  private MMessageFormatter()
  {

  }

  /**
   * Convert the given message to a formatted message.
   *
   * @param message The message
   *
   * @return A formatted message
   */

  public static MMessageFormatted toFormatted(
    final MMessageType message)
  {
    final var formatted = new MMessageFormatted();
    if (message instanceof MMessageStreamStarted started) {
      formatted.status = "STREAM_STARTED";
      formatted.text = toText(started);
      formatted.html = toHTML(started);
      return formatted;
    }
    if (message instanceof MMessageStreamEnded ended) {
      formatted.status = "STREAM_ENDED";
      formatted.text = toText(ended);
      formatted.html = toHTML(ended);
      return formatted;
    }
    throw new UnreachableCodeException();
  }

  /**
   * Convert the given message to HTML
   *
   * @param message The message
   *
   * @return HTML
   */

  public static String toHTML(
    final MMessageType message)
  {
    if (message instanceof MMessageStreamStarted started) {
      return toHTMLStarted(started);
    }
    if (message instanceof MMessageStreamEnded ended) {
      return toHTMLEnded(ended);
    }
    throw new UnreachableCodeException();
  }

  private static String toHTMLEnded(
    final MMessageStreamEnded ended)
  {
    final var text = new StringBuilder(128);
    text.append("Stream ended: <i>");
    text.append(StringEscapeUtils.escapeXml11(ended.streamTitle()));
    text.append("</i>");
    return text.toString();
  }

  private static String toHTMLStarted(
    final MMessageStreamStarted started)
  {
    final var text = new StringBuilder(128);
    text.append("<p><b>Stream starting: <i>");
    text.append(StringEscapeUtils.escapeXml11(started.streamTitle()));
    text.append("</i></b></p>");
    text.append(
      "<p>The stream is accessible using any of the following links:</p>");
    text.append("<ul>");
    for (final var entry : started.addresses().entrySet()) {
      text.append("<li>");
      text.append("<a href=\"");
      text.append(entry.getValue());
      text.append("\">");
      text.append(entry.getValue());
      text.append("</a>");
      text.append("</li>");
    }
    text.append("</ul>");
    for (final var entry : started.addresses().entrySet()) {
      text.append("<pre>");
      text.append("$ vlc ");
      text.append("<a href=\"");
      text.append(entry.getValue());
      text.append("\">");
      text.append(entry.getValue());
      text.append("</a>");
      text.append("</pre>");
      break;
    }
    return text.toString();
  }

  /**
   * Convert the given message to text
   *
   * @param message The message
   *
   * @return text
   */

  public static String toText(
    final MMessageType message)
  {
    if (message instanceof MMessageStreamStarted started) {
      return toTextStarted(started);
    }
    if (message instanceof MMessageStreamEnded ended) {
      return toTextEnded(ended);
    }
    throw new UnreachableCodeException();
  }

  private static String toTextEnded(
    final MMessageStreamEnded ended)
  {
    final var text = new StringBuilder(128);
    text.append("Stream ending: ");
    text.append('"');
    text.append(ended.streamTitle());
    text.append('"');
    return text.toString();
  }

  private static String toTextStarted(
    final MMessageStreamStarted started)
  {
    final var text = new StringBuilder(128);
    text.append("Stream starting: ");
    text.append('"');
    text.append(started.streamTitle());
    text.append('"');

    if (!started.addresses().isEmpty()) {
      text.append(" (");
      text.append(
        started.addresses()
          .values()
          .stream()
          .map(URI::toString)
          .collect(Collectors.joining(" | "))
      );
      text.append(")");
    }
    return text.toString();
  }
}
