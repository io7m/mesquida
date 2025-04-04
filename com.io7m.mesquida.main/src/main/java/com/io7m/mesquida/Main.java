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

import com.io7m.mesquida.internal.cli.MCommandIRCBot;
import com.io7m.mesquida.internal.cli.MCommandMatrixBot;
import com.io7m.mesquida.internal.cli.MCommandServer;
import com.io7m.quarrel.core.QApplication;
import com.io7m.quarrel.core.QApplicationMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;

/**
 * The main command-line entry point.
 */

public final class Main
{
  private static final Logger LOG =
    LoggerFactory.getLogger(Main.class);

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
    System.setProperty("org.jooq.no-logo", "true");

    final var builder =
      QApplication.builder(
        new QApplicationMetadata(
          "mesquida",
          "com.io7m.mesquida",
          "1.0.0",
          "cafebabe",
          "The mesquida application.",
          Optional.empty()
        )
      );

    builder.addCommand(new MCommandIRCBot());
    builder.addCommand(new MCommandMatrixBot());
    builder.addCommand(new MCommandServer());

    final var application =
      builder.build();
    final var r =
      application.run(LOG, List.of(args));

    return r.exitCode();
  }
}
