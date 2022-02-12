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

package com.io7m.mesquida;

import com.io7m.claypot.core.CLPApplicationConfiguration;
import com.io7m.claypot.core.Claypot;
import com.io7m.mesquida.internal.cli.MCommandIRCBot;
import com.io7m.mesquida.internal.cli.MCommandMatrixBot;
import com.io7m.mesquida.internal.cli.MCommandServer;
import org.slf4j.LoggerFactory;

import java.net.URI;

/**
 * The main command-line entry point.
 */

public final class Main
{
  private Main()
  {

  }

  /**
   * The main command-line entry point.
   *
   * @param args The command-line arguments
   */

  public static void main(
    final String[] args)
  {
    System.exit(mainExitless(args));
  }

  /**
   * The main command-line entry point.
   *
   * @param args The command-line arguments
   *
   * @return A program exit code
   */

  public static int mainExitless(
    final String[] args)
  {
    final var configuration =
      CLPApplicationConfiguration.builder()
        .setLogger(LoggerFactory.getLogger(Main.class))
        .setDocumentationURI(URI.create(
          "https://www.io7m.com/software/mesquida/"))
        .setProgramName("mesquida")
        .addCommands(MCommandServer::new)
        .addCommands(MCommandMatrixBot::new)
        .addCommands(MCommandIRCBot::new)
        .build();

    final var claypot = Claypot.create(configuration);
    claypot.execute(args);
    return claypot.exitCode();
  }
}
