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

package com.io7m.mesquida.internal.mq;

import java.net.URI;
import java.util.Map;
import java.util.Objects;

/**
 * Streaming started.
 *
 * @param streamName  The stream name
 * @param addresses   The stream addresses
 * @param streamTitle The stream title
 */

public record MMessageStreamStarted(
  String streamName,
  String streamTitle,
  Map<String, URI> addresses
) implements MMessageType
{
  /**
   * Streaming started.
   *
   * @param streamName  The stream name
   * @param addresses   The stream addresses
   * @param streamTitle The stream title
   */

  public MMessageStreamStarted
  {
    Objects.requireNonNull(streamName, "streamName");
    Objects.requireNonNull(streamTitle, "streamTitle");
    Objects.requireNonNull(addresses, "addresses");
  }
}
