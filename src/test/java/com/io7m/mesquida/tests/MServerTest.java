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

import com.io7m.mesquida.internal.MConfiguration;
import com.io7m.mesquida.internal.database.MDatabase;
import com.io7m.mesquida.internal.database.MDatabaseConfiguration;
import com.io7m.mesquida.internal.MServerConfiguration;
import com.io7m.mesquida.internal.MServerMain;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.CookieManager;
import java.net.InetAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.util.Locale;

import static com.io7m.mesquida.internal.database.Tables.STREAMS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

public final class MServerTest
{
  private static final Logger LOG =
    LoggerFactory.getLogger(MServerTest.class);

  private Path base;
  private MDatabase database;
  private MServerMain server;
  private HttpClient client;
  private CookieManager cookies;

  @BeforeEach
  public void setup()
    throws Exception
  {
    this.cookies =
      new CookieManager();

    this.client =
      HttpClient.newBuilder()
        .followRedirects(HttpClient.Redirect.ALWAYS)
        .cookieHandler(this.cookies)
        .build();

    this.base = MTestDirectories.createTempDirectory();

    final var configuration =
      new MConfiguration(
        new MServerConfiguration(
          Locale.getDefault(),
          InetAddress.getByName("localhost"),
          9999,
          InetAddress.getByName("localhost"),
          9998,
          "abcd",
          this.base.resolve("sessions")
        ),
        new MDatabaseConfiguration(
          this.base.resolve("database"),
          true,
          true
        )
      );

    this.database = MDatabase.open(configuration.database());
    this.server = MServerMain.create(configuration.http(), this.database);
    this.server.start();
  }

  @AfterEach
  public void tearDown()
    throws Exception
  {
    this.server.close();
    this.database.close();
    MTestDirectories.deleteDirectory(this.base);
  }

  @Test
  public void testUserListNoToken()
    throws Exception
  {
    final var response =
      this.client.send(
        HttpRequest.newBuilder(URI.create("http://localhost:9999/user-list/"))
          .build(),
        HttpResponse.BodyHandlers.ofString()
      );

    assertEquals(401, response.statusCode());
    LOG.debug("{}", response.body());
  }

  @Test
  public void testUserListEmpty()
    throws Exception
  {
    final var response =
      this.client.send(
        HttpRequest.newBuilder(URI.create("http://localhost:9999/user-list/"))
          .header("mesquida-token", "abcd")
          .build(),
        HttpResponse.BodyHandlers.ofString()
      );

    assertEquals(200, response.statusCode());
    LOG.debug("{}", response.body());
  }

  @Test
  public void testUserAddList()
    throws Exception
  {
    final var putResponse =
      this.client.send(
        HttpRequest.newBuilder(URI.create("http://localhost:9999/user-put/"))
          .header("mesquida-token", "abcd")
          .POST(HttpRequest.BodyPublishers.ofString(
            """
                {
                  "user": "someone",
                  "password": "12345678"
                }
              """))
          .build(),
        HttpResponse.BodyHandlers.ofString()
      );

    assertEquals(200, putResponse.statusCode());
    LOG.debug("{}", putResponse.body());

    {
      final var getResponse =
        this.client.send(
          HttpRequest.newBuilder(URI.create("http://localhost:9999/user-list/"))
            .header("mesquida-token", "abcd")
            .build(),
          HttpResponse.BodyHandlers.ofString()
        );

      assertEquals(200, getResponse.statusCode());
      LOG.debug("{}", getResponse.body());
    }
  }

  @Test
  public void testUserAddInvalid0()
    throws Exception
  {
    final var putResponse =
      this.client.send(
        HttpRequest.newBuilder(URI.create("http://localhost:9999/user-put/"))
          .header("mesquida-token", "abcd")
          .POST(HttpRequest.BodyPublishers.ofString(
            """
                {
                  "user": "some one",
                  "password": "12345678"
                }
              """))
          .build(),
        HttpResponse.BodyHandlers.ofString()
      );

    assertEquals(500, putResponse.statusCode());
    LOG.debug("{}", putResponse.body());
  }

  @Test
  public void testStreamPutInvalid0()
    throws Exception
  {
    {
      final var putResponse =
        this.client.send(
          HttpRequest.newBuilder(URI.create("http://localhost:9999/user-put/"))
            .header("mesquida-token", "abcd")
            .POST(HttpRequest.BodyPublishers.ofString(
              """
                  {
                    "user": "someone",
                    "password": "12345678"
                  }
                """))
            .build(),
          HttpResponse.BodyHandlers.ofString()
        );

      assertEquals(200, putResponse.statusCode());
      LOG.debug("{}", putResponse.body());
    }

    final var putResponse =
      this.client.send(
        HttpRequest.newBuilder(URI.create("http://localhost:9999/stream-put/"))
          .header("mesquida-token", "abcd")
          .POST(HttpRequest.BodyPublishers.ofString(
            """
                {
                  "streamName": "a",
                  "streamTitle": "The current stream title.",
                  "streamOwner": 1,
                }
              """))
          .build(),
        HttpResponse.BodyHandlers.ofString()
      );

    assertEquals(500, putResponse.statusCode());
    LOG.debug("{}", putResponse.body());
  }

  @Test
  public void testStreamPutList()
    throws Exception
  {
    {
      final var putResponse =
        this.client.send(
          HttpRequest.newBuilder(URI.create("http://localhost:9999/user-put/"))
            .header("mesquida-token", "abcd")
            .POST(HttpRequest.BodyPublishers.ofString(
              """
                  {
                    "user": "someone",
                    "password": "12345678"
                  }
                """))
            .build(),
          HttpResponse.BodyHandlers.ofString()
        );

      assertEquals(200, putResponse.statusCode());
      LOG.debug("{}", putResponse.body());
    }

    {
      final var putResponse =
        this.client.send(
          HttpRequest.newBuilder(URI.create("http://localhost:9999/stream-put/"))
            .header("mesquida-token", "abcd")
            .POST(HttpRequest.BodyPublishers.ofString(
              """
                  {
                    "streamName": "live",
                    "streamTitle": "The current stream title.",
                    "streamOwner": 1
                  }
                """))
            .build(),
          HttpResponse.BodyHandlers.ofString()
        );

      assertEquals(200, putResponse.statusCode());
      LOG.debug("{}", putResponse.body());
    }

    {
      final var getResponse =
        this.client.send(
          HttpRequest.newBuilder(URI.create("http://localhost:9999/stream-list/"))
            .header("mesquida-token", "abcd")
            .build(),
          HttpResponse.BodyHandlers.ofString()
        );

      assertEquals(200, getResponse.statusCode());
      LOG.debug("{}", getResponse.body());
    }

    {
      final var deleteResponse =
        this.client.send(
          HttpRequest.newBuilder(URI.create(
              "http://localhost:9999/stream-delete/"))
            .header("mesquida-token", "abcd")
            .POST(HttpRequest.BodyPublishers.ofString(
              """
                  {
                    "streamId": 1
                  }
                """))
            .build(),
          HttpResponse.BodyHandlers.ofString()
        );

      assertEquals(200, deleteResponse.statusCode());
      LOG.debug("{}", deleteResponse.body());
    }

    {
      final var getResponse =
        this.client.send(
          HttpRequest.newBuilder(URI.create("http://localhost:9999/stream-list/"))
            .header("mesquida-token", "abcd")
            .build(),
          HttpResponse.BodyHandlers.ofString()
        );

      assertEquals(200, getResponse.statusCode());
      LOG.debug("{}", getResponse.body());
    }
  }

  @Test
  public void testStreamPutAddressList()
    throws Exception
  {
    {
      final var putResponse =
        this.client.send(
          HttpRequest.newBuilder(URI.create("http://localhost:9999/user-put/"))
            .header("mesquida-token", "abcd")
            .POST(HttpRequest.BodyPublishers.ofString(
              """
                  {
                    "user": "someone",
                    "password": "12345678"
                  }
                """))
            .build(),
          HttpResponse.BodyHandlers.ofString()
        );

      assertEquals(200, putResponse.statusCode());
      LOG.debug("{}", putResponse.body());
    }

    {
      final var putResponse =
        this.client.send(
          HttpRequest.newBuilder(URI.create("http://localhost:9999/stream-put/"))
            .header("mesquida-token", "abcd")
            .POST(HttpRequest.BodyPublishers.ofString(
              """
                  {
                    "streamName": "live",
                    "streamTitle": "The current stream title.",
                    "streamOwner": 1
                  }
                """))
            .build(),
          HttpResponse.BodyHandlers.ofString()
        );

      assertEquals(200, putResponse.statusCode());
      LOG.debug("{}", putResponse.body());
    }

    {
      final var putResponse =
        this.client.send(
          HttpRequest.newBuilder(URI.create(
              "http://localhost:9999/stream-address-put/"))
            .header("mesquida-token", "abcd")
            .POST(HttpRequest.BodyPublishers.ofString(
              """
                {
                  "streamId": 1,
                  "protocol": "rtmp",
                  "url": "rtmp://wwww.example.com/stream"
                }
                """))
            .build(),
          HttpResponse.BodyHandlers.ofString()
        );

      assertEquals(200, putResponse.statusCode());
      LOG.debug("{}", putResponse.body());
    }

    {
      final var getResponse =
        this.client.send(
          HttpRequest.newBuilder(URI.create("http://localhost:9999/stream-list/"))
            .header("mesquida-token", "abcd")
            .build(),
          HttpResponse.BodyHandlers.ofString()
        );

      assertEquals(200, getResponse.statusCode());
      LOG.debug("{}", getResponse.body());
    }

    {
      final var deleteResponse =
        this.client.send(
          HttpRequest.newBuilder(URI.create(
              "http://localhost:9999/stream-delete/"))
            .header("mesquida-token", "abcd")
            .POST(HttpRequest.BodyPublishers.ofString(
              """
                  {
                    "streamId": 1
                  }
                """))
            .build(),
          HttpResponse.BodyHandlers.ofString()
        );

      assertEquals(200, deleteResponse.statusCode());
      LOG.debug("{}", deleteResponse.body());
    }

    {
      final var getResponse =
        this.client.send(
          HttpRequest.newBuilder(URI.create("http://localhost:9999/stream-list/"))
            .header("mesquida-token", "abcd")
            .build(),
          HttpResponse.BodyHandlers.ofString()
        );

      assertEquals(200, getResponse.statusCode());
      LOG.debug("{}", getResponse.body());
    }
  }

  @Test
  public void testStreamStartStop()
    throws Exception
  {
    {
      final var putResponse =
        this.client.send(
          HttpRequest.newBuilder(URI.create("http://localhost:9999/user-put/"))
            .header("mesquida-token", "abcd")
            .POST(HttpRequest.BodyPublishers.ofString(
              """
                  {
                    "user": "someone",
                    "password": "12345678"
                  }
                """))
            .build(),
          HttpResponse.BodyHandlers.ofString()
        );

      assertEquals(200, putResponse.statusCode());
      LOG.debug("{}", putResponse.body());
    }

    {
      final var putResponse =
        this.client.send(
          HttpRequest.newBuilder(URI.create("http://localhost:9999/stream-put/"))
            .header("mesquida-token", "abcd")
            .POST(HttpRequest.BodyPublishers.ofString(
              """
                  {
                    "streamName": "live",
                    "streamTitle": "The current stream title.",
                    "streamOwner": 1
                  }
                """))
            .build(),
          HttpResponse.BodyHandlers.ofString()
        );

      assertEquals(200, putResponse.statusCode());
      LOG.debug("{}", putResponse.body());
    }

    {
      final var putResponse =
        this.client.send(
          HttpRequest.newBuilder(URI.create(
              "http://localhost:9999/stream-start/"))
            .header("mesquida-token", "abcd")
            .POST(HttpRequest.BodyPublishers.ofString(
              """
                {
                  "streamName": "live"
                }
                """))
            .build(),
          HttpResponse.BodyHandlers.ofString()
        );

      assertEquals(200, putResponse.statusCode());
      LOG.debug("{}", putResponse.body());
    }

    {
      final var putResponse =
        this.client.send(
          HttpRequest.newBuilder(URI.create("http://localhost:9999/stream-stop/"))
            .header("mesquida-token", "abcd")
            .POST(HttpRequest.BodyPublishers.ofString(
              """
                {
                  "streamName": "live"
                }
                """))
            .build(),
          HttpResponse.BodyHandlers.ofString()
        );

      assertEquals(200, putResponse.statusCode());
      LOG.debug("{}", putResponse.body());
    }
  }

  @Test
  public void testLogin()
    throws Exception
  {
    {
      final var putResponse =
        this.client.send(
          HttpRequest.newBuilder(URI.create("http://localhost:9999/user-put/"))
            .header("mesquida-token", "abcd")
            .POST(HttpRequest.BodyPublishers.ofString(
              """
                  {
                    "user": "someone",
                    "password": "12345678"
                  }
                """))
            .build(),
          HttpResponse.BodyHandlers.ofString()
        );

      assertEquals(200, putResponse.statusCode());
      LOG.debug("{}", putResponse.body());
    }

    {
      final var putResponse =
        this.client.send(
          HttpRequest.newBuilder(URI.create("http://localhost:9998/login/"))
            .headers("Content-Type", "application/x-www-form-urlencoded")
            .POST(HttpRequest.BodyPublishers.ofString(
              "user=someone&password=12345678"))
            .build(),
          HttpResponse.BodyHandlers.ofString()
        );

      assertEquals(200, putResponse.statusCode());
      LOG.debug("{}", putResponse.body());

      final var cookieList =
        this.cookies.getCookieStore()
          .getCookies();

      assertEquals(1, cookieList.size());
    }
  }

  @Test
  public void testLoginFailure()
    throws Exception
  {
    {
      final var putResponse =
        this.client.send(
          HttpRequest.newBuilder(URI.create("http://localhost:9999/user-put/"))
            .header("mesquida-token", "abcd")
            .POST(HttpRequest.BodyPublishers.ofString(
              """
                  {
                    "user": "someone",
                    "password": "12345678"
                  }
                """))
            .build(),
          HttpResponse.BodyHandlers.ofString()
        );

      assertEquals(200, putResponse.statusCode());
      LOG.debug("{}", putResponse.body());
    }

    {
      final var putResponse =
        this.client.send(
          HttpRequest.newBuilder(URI.create("http://localhost:9998/login/"))
            .headers("Content-Type", "application/x-www-form-urlencoded")
            .POST(HttpRequest.BodyPublishers.ofString(
              "user=someone&password=1234"))
            .build(),
          HttpResponse.BodyHandlers.ofString()
        );

      assertEquals(401, putResponse.statusCode());
      LOG.debug("{}", putResponse.body());
    }
  }

  @Test
  public void testStreamEdit()
    throws Exception
  {
    {
      final var putResponse =
        this.client.send(
          HttpRequest.newBuilder(URI.create("http://localhost:9999/user-put/"))
            .header("mesquida-token", "abcd")
            .POST(HttpRequest.BodyPublishers.ofString(
              """
                  {
                    "user": "someone",
                    "password": "12345678"
                  }
                """))
            .build(),
          HttpResponse.BodyHandlers.ofString()
        );

      assertEquals(200, putResponse.statusCode());
      LOG.debug("{}", putResponse.body());
    }

    {
      final var putResponse =
        this.client.send(
          HttpRequest.newBuilder(URI.create("http://localhost:9999/stream-put/"))
            .header("mesquida-token", "abcd")
            .POST(HttpRequest.BodyPublishers.ofString(
              """
                  {
                    "streamName": "live",
                    "streamTitle": "The current stream title.",
                    "streamOwner": 1
                  }
                """))
            .build(),
          HttpResponse.BodyHandlers.ofString()
        );

      assertEquals(200, putResponse.statusCode());
      LOG.debug("{}", putResponse.body());
    }

    {
      final var putResponse =
        this.client.send(
          HttpRequest.newBuilder(URI.create("http://localhost:9998/login/"))
            .headers("Content-Type", "application/x-www-form-urlencoded")
            .POST(HttpRequest.BodyPublishers.ofString(
              "user=someone&password=12345678"))
            .build(),
          HttpResponse.BodyHandlers.ofString()
        );

      assertEquals(200, putResponse.statusCode());
      LOG.debug("{}", putResponse.body());
    }

    {
      final var putResponse =
        this.client.send(
          HttpRequest.newBuilder(URI.create("http://localhost:9998/stream-edit/"))
            .headers("Content-Type", "application/x-www-form-urlencoded")
            .POST(HttpRequest.BodyPublishers.ofString(
              "streamName=live&streamTitle=title"))
            .build(),
          HttpResponse.BodyHandlers.ofString()
        );

      assertEquals(200, putResponse.statusCode());
      LOG.debug("{}", putResponse.body());
    }

    try (var connection = this.database.openConnection()) {
      final var context =
        DSL.using(connection, SQLDialect.DERBY);
      final var stream =
        context.fetchOne(STREAMS);

      assertEquals("title", stream.getStreamTitle());
      connection.rollback();
    }
  }

  @Test
  public void testStreamEditInvalid0()
    throws Exception
  {
    {
      final var putResponse =
        this.client.send(
          HttpRequest.newBuilder(URI.create("http://localhost:9999/user-put/"))
            .header("mesquida-token", "abcd")
            .POST(HttpRequest.BodyPublishers.ofString(
              """
                  {
                    "user": "someone",
                    "password": "12345678"
                  }
                """))
            .build(),
          HttpResponse.BodyHandlers.ofString()
        );

      assertEquals(200, putResponse.statusCode());
      LOG.debug("{}", putResponse.body());
    }

    {
      final var putResponse =
        this.client.send(
          HttpRequest.newBuilder(URI.create("http://localhost:9999/stream-put/"))
            .header("mesquida-token", "abcd")
            .POST(HttpRequest.BodyPublishers.ofString(
              """
                  {
                    "streamName": "live",
                    "streamTitle": "The current stream title.",
                    "streamOwner": 1
                  }
                """))
            .build(),
          HttpResponse.BodyHandlers.ofString()
        );

      assertEquals(200, putResponse.statusCode());
      LOG.debug("{}", putResponse.body());
    }

    {
      final var putResponse =
        this.client.send(
          HttpRequest.newBuilder(URI.create("http://localhost:9998/login/"))
            .headers("Content-Type", "application/x-www-form-urlencoded")
            .POST(HttpRequest.BodyPublishers.ofString(
              "user=someone&password=12345678"))
            .build(),
          HttpResponse.BodyHandlers.ofString()
        );

      assertEquals(200, putResponse.statusCode());
      LOG.debug("{}", putResponse.body());
    }

    {
      final var text = new StringBuilder(512);
      text.append("streamName=live&");
      text.append("streamTitle=");
      for (int index = 0; index < 512; ++index) {
        text.append('a');
      }

      final var putResponse =
        this.client.send(
          HttpRequest.newBuilder(URI.create("http://localhost:9998/stream-edit/"))
            .headers("Content-Type", "application/x-www-form-urlencoded")
            .POST(HttpRequest.BodyPublishers.ofString(text.toString()))
            .build(),
          HttpResponse.BodyHandlers.ofString()
        );

      assertEquals(400, putResponse.statusCode());
      LOG.debug("{}", putResponse.body());
    }
  }

  @Test
  public void testStreamEditInvalid1()
    throws Exception
  {
    {
      final var putResponse =
        this.client.send(
          HttpRequest.newBuilder(URI.create("http://localhost:9999/user-put/"))
            .header("mesquida-token", "abcd")
            .POST(HttpRequest.BodyPublishers.ofString(
              """
                  {
                    "user": "someone",
                    "password": "12345678"
                  }
                """))
            .build(),
          HttpResponse.BodyHandlers.ofString()
        );

      assertEquals(200, putResponse.statusCode());
      LOG.debug("{}", putResponse.body());
    }

    {
      final var putResponse =
        this.client.send(
          HttpRequest.newBuilder(URI.create("http://localhost:9999/stream-put/"))
            .header("mesquida-token", "abcd")
            .POST(HttpRequest.BodyPublishers.ofString(
              """
                  {
                    "streamName": "live",
                    "streamTitle": "The current stream title.",
                    "streamOwner": 1
                  }
                """))
            .build(),
          HttpResponse.BodyHandlers.ofString()
        );

      assertEquals(200, putResponse.statusCode());
      LOG.debug("{}", putResponse.body());
    }

    {
      final var putResponse =
        this.client.send(
          HttpRequest.newBuilder(URI.create("http://localhost:9998/login/"))
            .headers("Content-Type", "application/x-www-form-urlencoded")
            .POST(HttpRequest.BodyPublishers.ofString(
              "user=someone&password=12345678"))
            .build(),
          HttpResponse.BodyHandlers.ofString()
        );

      assertEquals(200, putResponse.statusCode());
      LOG.debug("{}", putResponse.body());
    }

    {
      final var putResponse =
        this.client.send(
          HttpRequest.newBuilder(URI.create("http://localhost:9998/stream-edit/"))
            .headers("Content-Type", "application/x-www-form-urlencoded")
            .POST(HttpRequest.BodyPublishers.ofString(""))
            .build(),
          HttpResponse.BodyHandlers.ofString()
        );

      assertEquals(400, putResponse.statusCode());
      LOG.debug("{}", putResponse.body());
    }
  }
}
