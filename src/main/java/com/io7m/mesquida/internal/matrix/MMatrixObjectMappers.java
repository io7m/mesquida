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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.io7m.dixmont.core.DmJsonRestrictedDeserializers;

import java.util.Set;

import static com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES;
import static com.io7m.mesquida.internal.matrix.MMatrixJSON.MError;
import static com.io7m.mesquida.internal.matrix.MMatrixJSON.MLoginRequest;
import static com.io7m.mesquida.internal.matrix.MMatrixJSON.MLoginResponse;
import static com.io7m.mesquida.internal.matrix.MMatrixJSON.MRoomMessage;
import static com.io7m.mesquida.internal.matrix.MMatrixJSON.MRoomResolveAliasResponse;

/**
 * Functions to create JSON mappers for Matrix messages.
 */

public final class MMatrixObjectMappers
{
  private MMatrixObjectMappers()
  {

  }

  /**
   * @return A new mapper for Matrix messages
   */

  public static ObjectMapper createMapper()
  {
    final var classes = Set.of(
      MError.class.getCanonicalName(),
      MLoginRequest.class.getCanonicalName(),
      MLoginResponse.class.getCanonicalName(),
      MRoomResolveAliasResponse.class.getCanonicalName(),
      MRoomMessage.class.getCanonicalName(),
      "java.lang.String",
      "java.net.URI",
      "java.util.List<java.lang.String>"
    );

    final JsonMapper mapper =
      JsonMapper.builder()
        .disable(FAIL_ON_UNKNOWN_PROPERTIES)
        .build();

    final var deserializers =
      DmJsonRestrictedDeserializers.builder()
        .allowClassNames(classes)
        .build();

    final var simpleModule = new SimpleModule();
    simpleModule.setDeserializers(deserializers);
    mapper.registerModule(simpleModule);
    return mapper;
  }
}
