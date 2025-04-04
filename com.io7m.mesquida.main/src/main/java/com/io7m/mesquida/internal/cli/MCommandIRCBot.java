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

import com.io7m.mesquida.internal.irc.MIRCService;
import com.io7m.mesquida.internal.irc.MIRCServiceConfiguration;
import com.io7m.quarrel.core.QCommandContextType;
import com.io7m.quarrel.core.QCommandMetadata;
import com.io7m.quarrel.core.QCommandStatus;
import com.io7m.quarrel.core.QCommandType;
import com.io7m.quarrel.core.QParameterNamed01;
import com.io7m.quarrel.core.QParameterNamed1;
import com.io7m.quarrel.core.QParameterNamedType;
import com.io7m.quarrel.core.QStringType;
import com.io7m.quarrel.core.QStringType.QConstant;

import java.net.URI;
import java.util.List;
import java.util.Optional;

/**
 * The "irc bot" command.
 */

public final class MCommandIRCBot implements QCommandType
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

  private static final QParameterNamed1<String> IRC_SERVER =
    new QParameterNamed1<>(
      "--ircServer",
      List.of(),
      new QStringType.QConstant("The IRC server hostname"),
      Optional.empty(),
      String.class
    );

  private static final QParameterNamed1<Integer> IRC_PORT =
    new QParameterNamed1<>(
      "--ircPort",
      List.of(),
      new QStringType.QConstant("The IRC server port"),
      Optional.of(6667),
      Integer.class
    );

  private static final QParameterNamed1<String> IRC_USER =
    new QParameterNamed1<>(
      "--ircUser",
      List.of(),
      new QStringType.QConstant("The IRC server user"),
      Optional.empty(),
      String.class
    );

  private static final QParameterNamed01<String> IRC_PASSWORD =
    new QParameterNamed01<>(
      "--ircPassword",
      List.of(),
      new QStringType.QConstant("The IRC server password"),
      Optional.empty(),
      String.class
    );

  private static final QParameterNamed1<String> IRC_CHANNEL =
    new QParameterNamed1<>(
      "--ircChannel",
      List.of(),
      new QStringType.QConstant("The IRC server channel"),
      Optional.empty(),
      String.class
    );

  private static final QParameterNamed1<Boolean> IRC_TRUST =
    new QParameterNamed1<>(
      "--ircTrust",
      List.of(),
      new QStringType.QConstant("Trust all IRC server certificates (unsafe!)"),
      Optional.of(Boolean.FALSE),
      Boolean.class
    );

  private final QCommandMetadata metadata;

  /**
   * Construct a command.
   */

  public MCommandIRCBot()
  {
    this.metadata =
      new QCommandMetadata(
        "irc-bot",
        new QConstant("Run the IRC bot."),
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
      IRC_CHANNEL,
      IRC_PASSWORD,
      IRC_PORT,
      IRC_SERVER,
      IRC_TRUST,
      IRC_USER
    );
  }

  @Override
  public QCommandStatus onExecute(
    final QCommandContextType context)
    throws Exception
  {
    final var configuration =
      new MIRCServiceConfiguration(
        context.parameterValue(BROKER_URL),
        context.parameterValue(BROKER_USER),
        context.parameterValue(BROKER_PASSWORD),
        context.parameterValue(BROKER_TOPIC),
        context.parameterValue(IRC_SERVER),
        context.parameterValue(IRC_PORT),
        context.parameterValue(IRC_CHANNEL).replace("\\", ""),
        context.parameterValue(IRC_USER),
        context.parameterValue(IRC_USER),
        context.parameterValue(IRC_TRUST)
      );

    try (var ignored = MIRCService.create(configuration)) {
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
