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
import com.io7m.mesquida.internal.matrix.MMatrixService;
import com.io7m.mesquida.internal.matrix.MMatrixServiceConfiguration;

import java.net.URI;

/**
 * The "matrix bot" command.
 */

@Parameters(commandDescription = "Run the Matrix bot.")
public final class MCommandMatrixBot extends CLPAbstractCommand
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
    names = "--matrixServer",
    description = "The matrix server base URI",
    required = true)
  private URI matrixServerBase;

  @Parameter(
    names = "--matrixUser",
    description = "The matrix server user",
    required = true)
  private String matrixUser;

  @Parameter(
    names = "--matrixPassword",
    description = "The matrix server password",
    required = true)
  private String matrixPassword;

  @Parameter(
    names = "--matrixChannel",
    description = "The matrix server channel",
    required = true)
  private String matrixChannel;

  /**
   * Construct a command.
   *
   * @param inContext The context
   */

  public MCommandMatrixBot(
    final CLPCommandContextType inContext)
  {
    super(inContext);
  }

  @Override
  protected Status executeActual()
  {
    final var configuration =
      new MMatrixServiceConfiguration(
        this.brokerURL,
        this.brokerUser,
        this.brokerPassword,
        this.brokerTopic,
        this.matrixServerBase,
        this.matrixUser,
        this.matrixPassword,
        this.matrixChannel.replace("\\", "")
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
  public String name()
  {
    return "matrix-bot";
  }
}
