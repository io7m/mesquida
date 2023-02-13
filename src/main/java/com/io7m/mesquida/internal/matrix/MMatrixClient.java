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

package com.io7m.mesquida.internal.matrix;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Objects;

import static com.io7m.mesquida.internal.matrix.MMatrixJSON.MError;
import static com.io7m.mesquida.internal.matrix.MMatrixJSON.MLoginRequest;
import static com.io7m.mesquida.internal.matrix.MMatrixJSON.MLoginResponse;
import static com.io7m.mesquida.internal.matrix.MMatrixJSON.MMatrixJSONResponseType;
import static com.io7m.mesquida.internal.matrix.MMatrixJSON.MRoomMessage;
import static com.io7m.mesquida.internal.matrix.MMatrixJSON.MRoomResolveAliasResponse;
import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * An extremely minimal Matrix client.
 */

public final class MMatrixClient
{
  private static final Logger LOG =
    LoggerFactory.getLogger(MMatrixClient.class);

  private final HttpClient client;
  private final ObjectMapper objectMapper;
  private final URI serverBaseURI;
  private volatile BigInteger transactionId;

  private MMatrixClient(
    final HttpClient inClient,
    final ObjectMapper inObjectMapper,
    final URI inServerBaseURI)
  {
    this.client =
      Objects.requireNonNull(inClient, "client");
    this.objectMapper =
      Objects.requireNonNull(inObjectMapper, "inObjectMapper");
    this.serverBaseURI =
      Objects.requireNonNull(inServerBaseURI, "serverBaseURI");
    this.transactionId =
      BigInteger.ONE;
  }

  private static String agent()
  {
    final var version =
      MMatrixClient.class.getPackage().getImplementationVersion();
    if (version == null) {
      return "com.io7m.mesquida/0.0.0";
    }
    return String.format("com.io7m.mesquida/%s", version);
  }

  /**
   * Create a new client.
   *
   * @param inClient        The underlying HTTP client
   * @param inServerBaseURI The server base URI
   *
   * @return A new client
   */

  public static MMatrixClient create(
    final HttpClient inClient,
    final URI inServerBaseURI)
  {
    return new MMatrixClient(
      inClient,
      MMatrixObjectMappers.createMapper(),
      inServerBaseURI
    );
  }

  /**
   * Login request to the server.
   *
   * @param password The password
   * @param userName The user name
   *
   * @return A response
   *
   * @throws IOException          On I/O errors
   * @throws InterruptedException If the operation is interrupted
   */

  public MMatrixJSONResponseType login(
    final String userName,
    final String password)
    throws IOException, InterruptedException
  {
    Objects.requireNonNull(userName, "userName");
    Objects.requireNonNull(password, "password");

    final var request = new MLoginRequest();
    request.userName = userName;
    request.password = password;

    final var targetURI =
      this.serverBaseURI.resolve("/_matrix/client/r0/login");
    final var serialized =
      this.objectMapper.writeValueAsBytes(request);
    final var httpRequest =
      HttpRequest.newBuilder(targetURI)
        .POST(HttpRequest.BodyPublishers.ofByteArray(serialized))
        .header("User-Agent", agent())
        .build();
    final var response =
      this.client.send(httpRequest, HttpResponse.BodyHandlers.ofInputStream());

    final var statusCode =
      response.statusCode();
    final var contentType =
      response.headers().firstValue("content-type")
        .orElse("application/octet-stream");

    LOG.debug("{} status {}", targetURI, Integer.valueOf(statusCode));
    try (var stream = response.body()) {
      return this.parseResponse(
        statusCode,
        contentType,
        stream,
        MLoginResponse.class
      );
    }
  }

  /**
   * Resolve a room alias on the server.
   *
   * @param accessToken The access token
   * @param roomAlias   The room alias
   *
   * @return A response
   *
   * @throws IOException          On I/O errors
   * @throws InterruptedException If the operation is interrupted
   */

  public MMatrixJSONResponseType roomResolveAlias(
    final String accessToken,
    final String roomAlias)
    throws IOException, InterruptedException
  {
    Objects.requireNonNull(accessToken, "accessToken");
    Objects.requireNonNull(roomAlias, "roomAlias");

    final var encodedId =
      URLEncoder.encode(roomAlias, UTF_8);

    final var targetURI =
      this.serverBaseURI.resolve(String.format(
        "/_matrix/client/r0/directory/room/%s",
        encodedId)
      );
    final var httpRequest =
      HttpRequest.newBuilder(targetURI)
        .header("User-Agent", agent())
        .header("Authorization", String.format("Bearer %s", accessToken))
        .build();
    final var response =
      this.client.send(httpRequest, HttpResponse.BodyHandlers.ofInputStream());

    final var statusCode =
      response.statusCode();
    final var contentType =
      response.headers().firstValue("content-type")
        .orElse("application/octet-stream");

    LOG.debug("{} status {}", targetURI, Integer.valueOf(statusCode));
    try (var stream = response.body()) {
      return this.parseResponse(
        statusCode,
        contentType,
        stream,
        MRoomResolveAliasResponse.class
      );
    }
  }

  /**
   * Join a room on the server.
   *
   * @param accessToken The access token
   * @param roomId      The room ID
   *
   * @throws IOException          On I/O errors
   * @throws InterruptedException If the operation is interrupted
   */

  public void roomJoin(
    final String accessToken,
    final String roomId)
    throws IOException, InterruptedException
  {
    Objects.requireNonNull(accessToken, "accessToken");
    Objects.requireNonNull(roomId, "roomId");

    this.transactionId =
      this.transactionId.add(BigInteger.ONE);

    final var targetURI =
      this.serverBaseURI.resolve(
        String.format(
          "/_matrix/client/r0/rooms/%s/join",
          URLEncoder.encode(roomId, UTF_8)
        ));

    final var httpRequest =
      HttpRequest.newBuilder(targetURI)
        .header("User-Agent", agent())
        .header("Authorization", String.format("Bearer %s", accessToken))
        .POST(HttpRequest.BodyPublishers.noBody())
        .build();
    final var response =
      this.client.send(httpRequest, HttpResponse.BodyHandlers.ofInputStream());

    final var statusCode = response.statusCode();
    LOG.debug("{} status {}", targetURI, Integer.valueOf(statusCode));
    if (statusCode >= 400) {
      throw new IOException(
        String.format("Server responded: %d", Integer.valueOf(statusCode)));
    }
  }

  /**
   * Send a plain text message to the server.
   *
   * @param accessToken The access token
   * @param roomId      The room ID
   * @param message     The message text
   *
   * @throws IOException          On I/O errors
   * @throws InterruptedException If the operation is interrupted
   */

  public void roomSendMessage(
    final String accessToken,
    final String roomId,
    final MMatrixMessage message)
    throws IOException, InterruptedException
  {
    Objects.requireNonNull(accessToken, "accessToken");
    Objects.requireNonNull(roomId, "roomId");
    Objects.requireNonNull(message, "message");

    final var messageReq = new MRoomMessage();
    messageReq.body = message.text();
    messageReq.formattedBody = message.html();
    messageReq.msgtype = "m.text";

    this.transactionId =
      this.transactionId.add(BigInteger.ONE);

    final var targetURI =
      this.serverBaseURI.resolve(
        String.format(
          "/_matrix/client/r0/rooms/%s/send/m.room.message/%s",
          URLEncoder.encode(roomId, UTF_8),
          this.transactionId
        ));

    final var messageData =
      this.objectMapper.writeValueAsBytes(messageReq);

    final var httpRequest =
      HttpRequest.newBuilder(targetURI)
        .header("User-Agent", agent())
        .header("Authorization", String.format("Bearer %s", accessToken))
        .PUT(HttpRequest.BodyPublishers.ofByteArray(messageData))
        .build();
    final var response =
      this.client.send(httpRequest, HttpResponse.BodyHandlers.ofInputStream());

    final var statusCode = response.statusCode();
    LOG.debug("{} status {}", targetURI, Integer.valueOf(statusCode));
    if (statusCode >= 400) {
      throw new IOException(
        String.format("Server responded: %d", Integer.valueOf(statusCode)));
    }
  }

  private MMatrixJSONResponseType parseResponse(
    final int statusCode,
    final String contentType,
    final InputStream stream,
    final Class<? extends MMatrixJSONResponseType> responseClass)
    throws IOException
  {
    if (!Objects.equals(contentType, "application/json")) {
      throw new IOException(String.format(
        "Server responded with an unexpected content type '%s'",
        contentType)
      );
    }

    final var data = stream.readAllBytes();
    // CHECKSTYLE:OFF
    final var text = new String(data, UTF_8);
    // CHECKSTYLE:ON
    LOG.trace("received: {}", text);

    if (statusCode >= 400) {
      final var error =
        this.objectMapper.readValue(text, MError.class);

      LOG.trace("error: {}", error);
      return error;
    }

    final var response =
      this.objectMapper.readValue(text, responseClass);

    LOG.trace("response: {}", response);
    return response;
  }
}
