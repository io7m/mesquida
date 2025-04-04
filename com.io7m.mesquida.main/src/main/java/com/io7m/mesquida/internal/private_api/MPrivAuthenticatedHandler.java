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

import com.io7m.mesquida.internal.MServerConfiguration;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;

import java.io.IOException;
import java.util.Objects;

/**
 * A handler that supports token-based authentication.
 */

public abstract class MPrivAuthenticatedHandler extends AbstractHandler
{
  private final MServerConfiguration configuration;

  /**
   * Construct a handler.
   *
   * @param inConfiguration The configuration
   */

  public MPrivAuthenticatedHandler(
    final MServerConfiguration inConfiguration)
  {
    this.configuration =
      Objects.requireNonNull(inConfiguration, "inConfiguration");
  }

  private static void sendErrorAuthFailed(
    final HttpServletResponse response)
    throws IOException
  {
    response.setContentType("text/plain");
    response.setStatus(401);
    try (var outputStream = response.getOutputStream()) {
      outputStream.print(401);
      outputStream.print(' ');
      outputStream.println("Operation not permitted");
      outputStream.flush();
    }
  }

  protected static void sendError(
    final HttpServletResponse response,
    final int code,
    final String message)
    throws IOException
  {
    response.setContentType("text/plain");
    response.setStatus(code);
    try (var outputStream = response.getOutputStream()) {
      outputStream.print(code);
      outputStream.print(' ');
      outputStream.println(message);
      outputStream.flush();
    }
  }

  protected abstract void handleAuthenticated(
    String target,
    Request baseRequest,
    HttpServletRequest request,
    HttpServletResponse response)
    throws IOException;

  @Override
  public final void handle(
    final String target,
    final Request baseRequest,
    final HttpServletRequest request,
    final HttpServletResponse response)
    throws IOException
  {
    final var header = baseRequest.getHeader("Mesquida-Token");
    if (header == null) {
      sendErrorAuthFailed(response);
      return;
    }
    if (!header.equals(this.configuration.serverPrivateToken())) {
      sendErrorAuthFailed(response);
      return;
    }

    this.handleAuthenticated(target, baseRequest, request, response);
  }
}
