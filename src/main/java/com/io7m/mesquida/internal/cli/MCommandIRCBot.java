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

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.io7m.claypot.core.CLPAbstractCommand;
import com.io7m.claypot.core.CLPCommandContextType;
import com.io7m.mesquida.internal.irc.MIRCService;
import com.io7m.mesquida.internal.irc.MIRCServiceConfiguration;

import java.net.URI;

/**
 * The "irc bot" command.
 */

@Parameters(commandDescription = "Run the IRC bot.")
public final class MCommandIRCBot extends CLPAbstractCommand
{
  @Parameter(
    names = "--brokerURL",
    description = "The message broker URI",
    required = true)
  private URI brokerURL;

  @Parameter(
    names = "--brokerUser",
    description = "The message broker user",
    required = true)
  private String brokerUser;

  @Parameter(
    names = "--brokerPassword",
    description = "The message broker password",
    required = true)
  private String brokerPassword;

  @Parameter(
    names = "--brokerTopic",
    description = "The message broker topic",
    required = true)
  private String brokerTopic;

  @Parameter(
    names = "--ircServer",
    description = "The IRC server hostname",
    required = true)
  private String ircServer;

  @Parameter(
    names = "--ircPort",
    description = "The IRC server port",
    required = false)
  private int ircPort = 6667;

  @Parameter(
    names = "--ircUser",
    description = "The IRC server user",
    required = true)
  private String ircUser;

  @Parameter(
    names = "--ircPassword",
    description = "The IRC server password",
    required = false)
  private String ircPassword = "";

  @Parameter(
    names = "--ircChannel",
    description = "The IRC server channel",
    required = true)
  private String ircChannel;

  /**
   * Construct a command.
   *
   * @param inContext The context
   */

  public MCommandIRCBot(
    final CLPCommandContextType inContext)
  {
    super(inContext);
  }

  @Override
  protected Status executeActual()
  {
    final var configuration =
      new MIRCServiceConfiguration(
        this.brokerURL,
        this.brokerUser,
        this.brokerPassword,
        this.brokerTopic,
        this.ircServer,
        this.ircPort,
        this.ircChannel.replace("\\", ""),
        this.ircUser,
        this.ircUser
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
  public String name()
  {
    return "irc-bot";
  }
}
