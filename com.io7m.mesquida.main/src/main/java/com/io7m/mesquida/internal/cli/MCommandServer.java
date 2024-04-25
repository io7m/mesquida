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
import com.io7m.mesquida.internal.MConfiguration;
import com.io7m.mesquida.internal.MServerMain;
import com.io7m.mesquida.internal.database.MDatabase;

import java.nio.file.Path;

/**
 * The "server" command.
 */

@Parameters(commandDescription = "Run the server.")
public final class MCommandServer extends CLPAbstractCommand
{
  /**
   * The server configuration file.
   */

  @Parameter(
    names = "--configuration",
    description = "The configuration file",
    required = true)
  private Path configurationFile;

  /**
   * Construct a command.
   *
   * @param inContext The context
   */

  public MCommandServer(
    final CLPCommandContextType inContext)
  {
    super(inContext);
  }

  @Override
  protected Status executeActual()
    throws Exception
  {
    final var configuration =
      MConfiguration.open(this.configurationFile);
    try (var database = MDatabase.open(configuration.database())) {
      try (var server = MServerMain.create(configuration.http(), database)) {
        server.start();
        while (true) {
          Thread.sleep(1_000L);
        }
      }
    }
  }

  @Override
  public String name()
  {
    return "server";
  }
}
