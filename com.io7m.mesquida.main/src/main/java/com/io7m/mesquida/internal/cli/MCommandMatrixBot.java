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

package com.io7m.mesquida.internal.cli;

import com.io7m.mesquida.internal.matrix.MMatrixService;
import com.io7m.mesquida.internal.matrix.MMatrixServiceConfiguration;
import com.io7m.quarrel.core.QCommandContextType;
import com.io7m.quarrel.core.QCommandMetadata;
import com.io7m.quarrel.core.QCommandStatus;
import com.io7m.quarrel.core.QCommandType;
import com.io7m.quarrel.core.QParameterNamed1;
import com.io7m.quarrel.core.QParameterNamedType;
import com.io7m.quarrel.core.QStringType;

import java.net.URI;
import java.util.List;
import java.util.Optional;

/**
 * The "matrix bot" command.
 */

public final class MCommandMatrixBot implements QCommandType
{
  private static final QParameterNamed1<URI> BROKER_URL =
    new QParameterNamed1<>(
      "--brokerURL",
      List.of(),
      new QStringType.QConstant("The message broker URI"),
      Optional.empty(),
      URI.class
    );

  private static final QParameterNamed1<String> BROKER_USER =
    new QParameterNamed1<>(
      "--brokerUser",
      List.of(),
      new QStringType.QConstant("The message broker user"),
      Optional.empty(),
      String.class
    );

  private static final QParameterNamed1<String> BROKER_PASSWORD =
    new QParameterNamed1<>(
      "--brokerPassword",
      List.of(),
      new QStringType.QConstant("The message broker password"),
      Optional.empty(),
      String.class
    );

  private static final QParameterNamed1<String> BROKER_TOPIC =
    new QParameterNamed1<>(
      "--brokerTopic",
      List.of(),
      new QStringType.QConstant("The message broker topic"),
      Optional.empty(),
      String.class
    );

  private static final QParameterNamed1<URI> MATRIX_SERVER =
    new QParameterNamed1<>(
      "--matrixServer",
      List.of(),
      new QStringType.QConstant("The matrix server base URI"),
      Optional.empty(),
      URI.class
    );

  private static final QParameterNamed1<String> MATRIX_USER =
    new QParameterNamed1<>(
      "--matrixUser",
      List.of(),
      new QStringType.QConstant("The matrix server user"),
      Optional.empty(),
      String.class
    );

  private static final QParameterNamed1<String> MATRIX_PASSWORD =
    new QParameterNamed1<>(
      "--matrixPassword",
      List.of(),
      new QStringType.QConstant("The matrix server password"),
      Optional.empty(),
      String.class
    );

  private static final QParameterNamed1<String> MATRIX_CHANNEL =
    new QParameterNamed1<>(
      "--matrixChannel",
      List.of(),
      new QStringType.QConstant("The matrix server channel"),
      Optional.empty(),
      String.class
    );

  private final QCommandMetadata metadata;

  /**
   * Construct a command.
   */

  public MCommandMatrixBot()
  {
    this.metadata =
      new QCommandMetadata(
        "matrix-bot",
        new QStringType.QConstant("Run the Matrix bot."),
        Optional.empty()
      );
  }

  @Override
  public List<QParameterNamedType<?>> onListNamedParameters()
  {
    return List.of(
      BROKER_PASSWORD,
      BROKER_TOPIC,
      BROKER_URL,
      BROKER_USER,
      MATRIX_CHANNEL,
      MATRIX_PASSWORD,
      MATRIX_SERVER,
      MATRIX_USER
    );
  }

  @Override
  public QCommandStatus onExecute(
    final QCommandContextType context)
  {
    final var configuration =
      new MMatrixServiceConfiguration(
        context.parameterValue(BROKER_URL),
        context.parameterValue(BROKER_USER),
        context.parameterValue(BROKER_PASSWORD),
        context.parameterValue(BROKER_TOPIC),
        context.parameterValue(MATRIX_SERVER),
        context.parameterValue(MATRIX_USER),
        context.parameterValue(MATRIX_PASSWORD),
        context.parameterValue(MATRIX_CHANNEL).replace("\\", "")
      );

    try (var ignored = MMatrixService.create(configuration)) {
      while (true) {
        try {
          Thread.sleep(1_000L);
        } catch (final InterruptedException e) {
          Thread.currentThread().interrupt();
        }
      }
    }
  }

  @Override
  public QCommandMetadata metadata()
  {
    return this.metadata;
  }
}
