/*
 * Copyright © 2021 Mark Raynsford <code@io7m.com> https://www.io7m.com
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

import com.io7m.jxtrand.vanilla.JXTAbstractStrings;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Locale;

/**
 * Functions to generate server pages.
 */

public final class MPubPages extends JXTAbstractStrings
{
  /**
   * A page generator.
   *
   * @param locale The server locale
   *
   * @throws IOException On errors
   */

  public MPubPages(
    final Locale locale)
    throws IOException
  {
    super(
      locale,
      MPubPages.class,
      "/com/io7m/mesquida/internal",
      "Pages"
    );
  }

  /**
   * Generate a login page.
   *
   * @param title The title
   *
   * @return A page
   */

  public String loginPage(
    final String title)
  {
    return this.format(
      "loginPage",
      title
    );
  }

  /**
   * Generate a main page.
   *
   * @param title   The title
   * @param content The content
   *
   * @return A page
   */

  public String mainPage(
    final String title,
    final String content)
  {
    return this.format(
      "mainPage",
      title,
      content
    );
  }

  /**
   * Generate a stream form.
   *
   * @param title The stream title
   * @param name  The stream name
   *
   * @return A page
   */

  public String streamForm(
    final String title,
    final String name)
  {
    return this.format(
      "streamForm",
      name,
      title
    );
  }

  /**
   * Send a page to a servlet response.
   *
   * @param response   The servlet response
   * @param statusCode The status code
   * @param text       The page text
   *
   * @throws IOException On I/O errors
   */

  public void sendPage(
    final HttpServletResponse response,
    final int statusCode,
    final String text)
    throws IOException
  {
    response.setContentType("application/xhtml+xml");
    response.setStatus(statusCode);

    try (var outputStream = response.getOutputStream()) {
      outputStream.println(text);
      outputStream.flush();
    }
  }
}
