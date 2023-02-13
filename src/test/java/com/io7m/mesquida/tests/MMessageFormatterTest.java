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


package com.io7m.mesquida.tests;

import com.io7m.mesquida.internal.mq.MMessageFormatter;
import com.io7m.mesquida.internal.mq.MMessageStreamStarted;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;

import static java.util.Map.entry;
import static java.util.Map.ofEntries;

public final class MMessageFormatterTest
{
  private static final Logger LOG =
    LoggerFactory.getLogger(MMessageFormatterTest.class);

  @Test
  public void testStartedText()
  {
    final var text =
      MMessageFormatter.toText(new MMessageStreamStarted(
        "live",
        "An example stream",
        ofEntries(
          entry("rtmp", URI.create("rtmp://www.example.com/live")),
          entry("rtsp", URI.create("rtsp://www.example.com/live"))
        )
      ));

    System.out.println(text);
  }

  @Test
  public void testStartedHTML()
  {
    final var text =
      MMessageFormatter.toHTML(new MMessageStreamStarted(
        "live",
        "An example stream",
        ofEntries(
          entry("rtmp", URI.create("rtmp://www.example.com/live")),
          entry("rtsp", URI.create("rtsp://www.example.com/live"))
        )
      ));

    System.out.println(text);
  }
}
