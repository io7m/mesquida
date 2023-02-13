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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.io7m.jmulticlose.core.CloseableCollection;
import com.io7m.jmulticlose.core.CloseableCollectionType;
import com.io7m.jmulticlose.core.ClosingResourceFailedException;
import com.io7m.mesquida.internal.MJson;
import com.io7m.mesquida.internal.mq.MMessageFormatted;
import org.apache.activemq.artemis.api.core.TransportConfiguration;
import org.apache.activemq.artemis.api.jms.ActiveMQJMSClient;
import org.apache.activemq.artemis.core.remoting.impl.netty.NettyConnectorFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.TextMessage;
import javax.jms.TopicSession;
import javax.jms.TopicSubscriber;
import java.io.IOException;
import java.net.http.HttpClient;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.apache.activemq.artemis.api.jms.JMSFactoryType.CF;

/**
 * The Matrix service.
 */

public final class MMatrixService implements AutoCloseable, Runnable
{
  private static final Logger LOG =
    LoggerFactory.getLogger(MMatrixService.class);

  private final ExecutorService executor;
  private final AtomicBoolean done;
  private final LinkedBlockingQueue<CommandType> commands;
  private final MMatrixServiceConfiguration configuration;
  private final ObjectMapper queueMessageMapper;
  private CloseableCollectionType<ClosingResourceFailedException> resources;
  private TopicSubscriber subscriber;
  private TopicSession session;
  private MMatrixClient matrixClient;
  private String matrixAccessToken;
  private String matrixRoom;

  private MMatrixService(
    final MMatrixServiceConfiguration inConfiguration,
    final ExecutorService inExecutor)
  {
    this.executor =
      Objects.requireNonNull(inExecutor, "executor");
    this.configuration =
      Objects.requireNonNull(inConfiguration, "configuration");

    this.done =
      new AtomicBoolean(false);
    this.commands =
      new LinkedBlockingQueue<>();
    this.queueMessageMapper =
      MJson.createMapper(
        Set.of(
          int.class.getCanonicalName(),
          String.class.getCanonicalName(),
          MMessageFormatted.class.getCanonicalName()
        )
      );

    this.resources =
      CloseableCollection.create();
  }

  /**
   * Create a new Matrix service.
   *
   * @param configuration The configuration
   *
   * @return A new Matrix service
   */

  public static MMatrixService create(
    final MMatrixServiceConfiguration configuration)
  {
    final var executor =
      Executors.newSingleThreadExecutor(r -> {
        final var thread = new Thread(r);
        thread.setName("com.io7m.mesquida.matrix[" + thread.getId() + "]");
        thread.setDaemon(true);
        return thread;
      });

    final var service = new MMatrixService(configuration, executor);
    executor.execute(service);
    return service;
  }

  @Override
  public void close()
  {
    if (this.done.compareAndSet(false, true)) {
      this.executor.shutdown();
    }
  }

  @Override
  public void run()
  {
    while (!this.done.get()) {
      try {
        this.configureMessageQueue();
      } catch (final Exception e) {
        LOG.error("unable to configure message queue: ", e);
        this.pause();
        continue;
      }

      try {
        this.configureMatrix();
      } catch (final Exception e) {
        LOG.error("unable to configure matrix: ", e);
        this.pause();
        continue;
      }

      while (!this.done.get()) {
        try {
          final var command =
            this.commands.poll(1L, TimeUnit.SECONDS);

          if (command == null) {
            continue;
          }

          if (command instanceof CommandJMSReceived message) {
            this.handleMessage(message.message);
            continue;
          }

        } catch (final InterruptedException e) {
          Thread.currentThread().interrupt();
        } catch (final JMSException e) {
          LOG.error("jms: ", e);
        } catch (final IOException e) {
          LOG.error("i/o: ", e);
        }
      }
    }

    try {
      this.resources.close();
    } catch (final ClosingResourceFailedException e) {
      LOG.error("error closing service: ", e);
    }
  }

  private void handleMessage(
    final Message message)
    throws JMSException, IOException, InterruptedException
  {
    if (message instanceof TextMessage textMessage) {
      final MMessageFormatted parsed;
      try {
        parsed = this.queueMessageMapper.readValue(
          textMessage.getText(),
          MMessageFormatted.class
        );
      } catch (JsonProcessingException | JMSException e) {

        /*
         * Acknowledge the message if we can't parse it, so that we don't
         * get it again and fail again.
         */

        textMessage.acknowledge();
        throw e;
      }

      /*
       * Don't acknowledge the message until the Matrix server has accepted
       * our version of it.
       */

      this.matrixClient.roomSendMessage(
        this.matrixAccessToken,
        this.matrixRoom,
        new MMatrixMessage(
          parsed.text,
          parsed.html
        )
      );

      textMessage.acknowledge();
    }

    LOG.debug("ignored unrecognized message type: {}", message);
    message.acknowledge();
  }

  private void configureMatrix()
    throws IOException, InterruptedException
  {
    this.matrixClient =
      MMatrixClient.create(
        HttpClient.newHttpClient(),
        this.configuration.matrixServerBase()
      );

    this.matrixAccessToken =
      this.fetchAccessToken();
    this.matrixRoom =
      this.fetchRoom(this.matrixAccessToken);

    this.joinRoom(this.matrixAccessToken, this.matrixRoom);
  }

  private String fetchAccessToken()
    throws IOException, InterruptedException
  {
    final var response =
      this.matrixClient.login(
        this.configuration.matrixUser(),
        this.configuration.matrixPassword()
      );

    if (response instanceof MMatrixJSON.MError error) {
      throw new IOException(String.format(
        "Matrix server said: %s %s",
        error.errorCode,
        error.errorMessage)
      );
    }

    final var token =
      ((MMatrixJSON.MLoginResponse) response).accessToken;

    LOG.debug("retrieved access token {}", token);
    LOG.info("logged in to {}", this.configuration.matrixServerBase());
    return token;
  }

  private String fetchRoom(
    final String token)
    throws IOException, InterruptedException
  {
    final var roomId =
      this.matrixClient.roomResolveAlias(
        token,
        this.configuration.matrixChannel());

    if (roomId instanceof MMatrixJSON.MError error) {
      throw new IOException(String.format(
        "Matrix server said: %s %s",
        error.errorCode,
        error.errorMessage)
      );
    }

    return ((MMatrixJSON.MRoomResolveAliasResponse) roomId).roomId;
  }

  private void joinRoom(
    final String token,
    final String room)
    throws IOException, InterruptedException
  {
    this.matrixClient.roomJoin(token, room);
  }

  private void pause()
  {
    try {
      Thread.sleep(1_000L);
    } catch (final InterruptedException ex) {
      Thread.currentThread().interrupt();
    }
  }

  private void configureMessageQueue()
    throws Exception
  {
    if (this.resources != null) {
      this.resources.close();
    }

    this.resources = CloseableCollection.create();

    LOG.info("connect {}", this.configuration.brokerURL());

    final var transportConfiguration =
      new TransportConfiguration(NettyConnectorFactory.class.getName());
    final var connections =
      this.resources.add(
        ActiveMQJMSClient.createConnectionFactoryWithoutHA(
          CF,
          transportConfiguration
        ));

    connections.setBrokerURL(this.configuration.brokerURL().toString());
    connections.setClientID(UUID.randomUUID().toString());
    connections.setUser(this.configuration.brokerUser());
    connections.setPassword(this.configuration.brokerPass());
    connections.setReconnectAttempts(-1);

    final var topicConnection =
      this.resources.add(connections.createTopicConnection());

    this.session =
      this.resources.add(
        topicConnection.createTopicSession(false, 0)
      );

    final var topic =
      this.session.createTopic(this.configuration.brokerTopic());
    this.subscriber =
      this.resources.add(this.session.createSubscriber(topic));

    this.subscriber.setMessageListener(message -> {
      this.commands.add(new CommandJMSReceived(message));
    });

    topicConnection.start();
  }

  sealed interface CommandType
  {

  }

  record CommandJMSReceived(Message message) implements CommandType
  {

  }
}
