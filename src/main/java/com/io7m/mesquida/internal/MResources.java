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

package com.io7m.mesquida.internal;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Functions over internal resources.
 */

public final class MResources
{
  private MResources()
  {

  }

  /**
   * Copy the named resource to the given output stream.
   *
   * @param outputStream The output stream
   * @param name         The resource name
   *
   * @throws IOException On I/O errors
   */

  public static void copyOut(
    final OutputStream outputStream,
    final String name)
    throws IOException
  {
    final var fileName =
      String.format("/com/io7m/mesquida/internal/%s", name);
    final var url =
      MResources.class.getResource(fileName);
    if (url == null) {
      throw new FileNotFoundException(fileName);
    }
    try (var stream = url.openStream()) {
      stream.transferTo(outputStream);
      outputStream.flush();
    }
  }
}
