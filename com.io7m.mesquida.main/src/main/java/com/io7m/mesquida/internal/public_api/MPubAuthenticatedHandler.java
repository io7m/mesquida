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
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.slf4j.Logger;
import org.slf4j.MDC;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.io.IOException;
import java.net.URI;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.sql.SQLException;
import java.util.HexFormat;
import java.util.Objects;

import static com.io7m.mesquida.internal.database.tables.Users.USERS;

/**
 * A handler that supports authentication.
 */

public abstract class MPubAuthenticatedHandler extends HttpServlet
{
  private final MPubPages pages;
  private final MDatabase database;
  private URI clientURI;
  private HttpServletResponse response;
  private String userName;

  /**
   * Construct a handler.
   *
   * @param inPages    The pages
   * @param inDatabase The database
   */

  public MPubAuthenticatedHandler(
    final MPubPages inPages,
    final MDatabase inDatabase)
  {
    this.pages =
      Objects.requireNonNull(inPages, "pages");
    this.database =
      Objects.requireNonNull(inDatabase, "database");
  }

  private static String clientOf(
    final HttpServletRequest servletRequest,
    final String user)
  {
    return new StringBuilder(64)
      .append('[')
      .append(servletRequest.getRemoteAddr())
      .append(':')
      .append(servletRequest.getRemotePort())
      .append(' ')
      .append(user)
      .append(']')
      .toString();
  }

  private static String clientOf(
    final HttpServletRequest servletRequest)
  {
    return new StringBuilder(64)
      .append('[')
      .append(servletRequest.getRemoteAddr())
      .append(':')
      .append(servletRequest.getRemotePort())
      .append(']')
      .toString();
  }

  private static URI makeClientURI(
    final HttpServletRequest servletRequest)
  {
    return URI.create(
      new StringBuilder(64)
        .append("client:")
        .append(servletRequest.getRemoteAddr())
        .append(":")
        .append(servletRequest.getRemotePort())
        .toString()
    );
  }

  private static URI makeClientURI(
    final HttpServletRequest servletRequest,
    final String userName)
  {
    return URI.create(
      new StringBuilder(64)
        .append("client:")
        .append(servletRequest.getRemoteAddr())
        .append(":")
        .append(servletRequest.getRemotePort())
        .append(":")
        .append(userName)
        .toString()
    );
  }

  /**
   * @return The pages
   */

  public final MPubPages pages()
  {
    return this.pages;
  }

  /**
   * @return The database
   */

  public final MDatabase database()
  {
    return this.database;
  }

  /**
   * @return The authenticated user
   */

  public final String userName()
  {
    return this.userName;
  }

  protected abstract Logger logger();

  protected abstract void serviceAuthenticated(
    HttpServletRequest request,
    HttpServletResponse servletResponse,
    HttpSession session)
    throws Exception;

  @Override
  public final void service(
    final HttpServletRequest request,
    final HttpServletResponse servletResponse)
    throws IOException
  {
    MDC.put("client", clientOf(request));
    this.clientURI = makeClientURI(request);
    this.response = servletResponse;

    try {
      final var session = request.getSession(false);
      if (session != null) {
        this.userName = (String) session.getAttribute("userName");
        MDC.put("client", clientOf(request, this.userName));
        this.clientURI = makeClientURI(request, this.userName);
        this.serviceAuthenticated(request, servletResponse, session);
        return;
      }

      final var tryUserName =
        request.getParameter("user");
      final var tryPassword =
        request.getParameter("password");

      if (tryUserName != null && tryPassword != null) {
        if (this.tryLogin(tryUserName, tryPassword)) {
          this.logger().info("login succeeded for '{}'", tryUserName);
          final var newSession = request.getSession();
          newSession.setAttribute("userName", tryUserName);
          this.response.sendRedirect(request.getRequestURI());
          return;
        }

        this.logger().info("login failed for '{}'", tryUserName);
        this.pages.sendPage(
          this.response,
          401,
          this.pages.loginPage("Mesquida")
        );
        return;
      }

      this.pages.sendPage(
        this.response,
        200,
        this.pages.loginPage("Mesquida")
      );
    } catch (final Exception e) {
      this.logger().trace("exception: ", e);
      throw new IOException(e);
    } finally {
      MDC.remove("client");
    }
  }

  private boolean tryLogin(
    final String tryUserName,
    final String tryPassword)
    throws SQLException, NoSuchAlgorithmException, InvalidKeySpecException
  {
    try (var connection = this.database.openConnection()) {
      try {
        final var context =
          DSL.using(connection, SQLDialect.DERBY);

        final var user =
          context.fetchOne(USERS, USERS.USER_NAME.eq(tryUserName));

        if (user == null) {
          return false;
        }

        final var formatter =
          HexFormat.of();
        final var salt =
          formatter.parseHex(user.getUserPassSalt());

        final var segments =
          user.getUserPassAlgo().split(":");

        final var keyFactory =
          SecretKeyFactory.getInstance(segments[0]);
        final var keySpec =
          new PBEKeySpec(
            tryPassword.toCharArray(),
            salt,
            Integer.parseInt(segments[1]),
            256);
        final var hash =
          keyFactory.generateSecret(keySpec).getEncoded();
        final var passwordHash =
          formatter.formatHex(hash);

        return Objects.equals(passwordHash, user.getUserPassHash());
      } finally {
        connection.rollback();
      }
    }
  }
}
