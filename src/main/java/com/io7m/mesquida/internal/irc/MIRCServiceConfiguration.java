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

package com.io7m.mesquida.internal.irc;

import java.net.URI;
import java.util.Objects;

/**
 * The IRC service configuration.
 *
 * @param brokerPass  The message broker password
 * @param brokerTopic The message broker topic
 * @param brokerURL   The message broker URL
 * @param brokerUser  The message broker user
 * @param ircChannel  The IRC channel name
 * @param ircHost     The IRC host
 * @param ircNickName The IRC nickname
 * @param ircPort     The IRC port
 * @param ircUserName The IRC user name
 */

public record MIRCServiceConfiguration(
  URI brokerURL,
  String brokerUser,
  String brokerPass,
  String brokerTopic,
  String ircHost,
  int ircPort,
  String ircChannel,
  String ircUserName,
  String ircNickName)
{
  /**
   * The IRC service configuration.
   *
   * @param brokerPass  The message broker password
   * @param brokerTopic The message broker topic
   * @param brokerURL   The message broker URL
   * @param brokerUser  The message broker user
   * @param ircChannel  The IRC channel name
   * @param ircHost     The IRC host
   * @param ircNickName The IRC nickname
   * @param ircPort     The IRC port
   * @param ircUserName The IRC user name
   */

  public MIRCServiceConfiguration
  {
    Objects.requireNonNull(brokerPass, "brokerPass");
    Objects.requireNonNull(brokerTopic, "brokerTopic");
    Objects.requireNonNull(brokerURL, "brokerURL");
    Objects.requireNonNull(brokerUser, "brokerUser");
    Objects.requireNonNull(ircChannel, "ircChannel");
    Objects.requireNonNull(ircHost, "ircHost");
    Objects.requireNonNull(ircNickName, "ircNickName");
    Objects.requireNonNull(ircUserName, "ircUserName");
  }
}
