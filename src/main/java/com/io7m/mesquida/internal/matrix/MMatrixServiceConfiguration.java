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

package com.io7m.mesquida.internal.matrix;

import java.net.URI;
import java.util.Objects;

/**
 * The Matrix service configuration.
 *
 * @param brokerPass       The message broker password
 * @param brokerTopic      The message broker topic
 * @param brokerURL        The message broker URL
 * @param brokerUser       The message broker user
 * @param matrixChannel    The Matrix channel name
 * @param matrixServerBase The Matrix server
 * @param matrixPassword   The Matrix password
 * @param matrixUser       The Matrix username
 */

public record MMatrixServiceConfiguration(
  URI brokerURL,
  String brokerUser,
  String brokerPass,
  String brokerTopic,
  URI matrixServerBase,
  String matrixUser,
  String matrixPassword,
  String matrixChannel)
{
  /**
   * The Matrix service configuration.
   *
   * @param brokerPass       The message broker password
   * @param brokerTopic      The message broker topic
   * @param brokerURL        The message broker URL
   * @param brokerUser       The message broker user
   * @param matrixChannel    The Matrix channel name
   * @param matrixServerBase The Matrix server
   * @param matrixPassword   The Matrix password
   * @param matrixUser       The Matrix username
   */

  public MMatrixServiceConfiguration
  {
    Objects.requireNonNull(brokerPass, "brokerPass");
    Objects.requireNonNull(brokerTopic, "brokerTopic");
    Objects.requireNonNull(brokerURL, "brokerURL");
    Objects.requireNonNull(brokerUser, "brokerUser");
    Objects.requireNonNull(matrixChannel, "matrixChannel");
    Objects.requireNonNull(matrixUser, "matrixUser");
    Objects.requireNonNull(matrixPassword, "matrixPassword");
    Objects.requireNonNull(matrixServerBase, "matrixServerBase");
  }
}
