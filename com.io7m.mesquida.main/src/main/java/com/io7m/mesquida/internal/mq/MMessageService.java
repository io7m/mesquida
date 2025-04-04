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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.io7m.jmulticlose.core.CloseableCollection;
import com.io7m.jmulticlose.core.CloseableCollectionType;
import com.io7m.jmulticlose.core.ClosingResourceFailedException;
import com.io7m.mesquida.internal.database.MDatabase;
import org.apache.activemq.artemis.api.core.TransportConfiguration;
import org.apache.activemq.artemis.api.jms.ActiveMQJMSClient;
import org.apache.activemq.artemis.core.remoting.impl.netty.NettyConnectorFactory;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jms.DeliveryMode;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.TopicPublisher;
import javax.jms.TopicSession;
import java.sql.SQLException;
import java.time.Duration;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.io7m.mesquida.internal.database.Tables.MESSAGE_QUEUE;
import static com.io7m.mesquida.internal.mq.MMessageService.CmdConfigurationChanged.CMD_CONFIGURATION_CHANGED;
import static org.apache.activemq.artemis.api.jms.JMSFactoryType.CF;

/**
 * A message queue service.
 */

public final class MMessageService implements AutoCloseable, Runnable
{
  private static final Logger LOG =
    LoggerFactory.getLogger(MMessageService.class);

  private final ExecutorService executor;
  private final AtomicBoolean done;
  private final MDatabase database;
  private final LinkedBlockingQueue<CommandType> commands;
  private final ObjectMapper mapper;
  private CloseableCollectionType<ClosingResourceFailedException> resources;
  private TopicPublisher publisher;
  private TopicSession session;

  private MMessageService(
    final MDatabase inDatabase,
    final ExecutorService inExecutor)
  {
    this.database =
      Objects.requireNonNull(inDatabase, "database");
    this.executor =
      Objects.requireNonNull(inExecutor, "executor");
    this.done =
      new AtomicBoolean(false);
    this.commands =
      new LinkedBlockingQueue<>();

    this.mapper =
      new ObjectMapper();
    this.resources =
      CloseableCollection.create();
  }

  /**
   * Create a message service.
   *
   * @param database The database
   *
   * @return A message service
   */

  public static MMessageService create(
    final MDatabase database)
  {
    final var executor =
      Executors.newSingleThreadExecutor(r -> {
        final var thread = new Thread(r);
        thread.setName("com.io7m.mesquida.messages[" + thread.getId() + "]");
        thread.setDaemon(true);
        return thread;
      });

    final var service = new MMessageService(database, executor);
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

      while (!this.done.get()) {
        try {
          final var command =
            this.commands.poll(1L, TimeUnit.SECONDS);

          if (command == null) {
            continue;
          }

          if (command instanceof CmdConfigurationChanged) {
            LOG.info("configuration changed");
            break;
          }

          if (command instanceof CmdSendMessage sendMessage) {
            if (this.publisher != null) {
              final var textMessage =
                this.session.createTextMessage(
                  this.mapper.writeValueAsString(
                    MMessageFormatter.toFormatted(sendMessage.message())
                  )
                );

              /*
               * Set a two-hour expiration date.
               */

              this.publisher.publish(
                textMessage,
                DeliveryMode.NON_PERSISTENT,
                Message.DEFAULT_PRIORITY,
                Duration.ofHours(2L).toMillis()
              );
            }
          }

        } catch (final InterruptedException e) {
          Thread.currentThread().interrupt();
        } catch (final JMSException e) {
          LOG.error("jms: ", e);
        } catch (final JsonProcessingException e) {
          LOG.error("json: ", e);
        }
      }
    }

    try {
      this.resources.close();
    } catch (final ClosingResourceFailedException e) {
      LOG.error("error closing service: ", e);
    }
  }

  private void pause()
  {
    try {
      Thread.sleep(1_000L);
    } catch (final InterruptedException ex) {
      Thread.currentThread().interrupt();
    }
  }

  /**
   * Tell the service the configuration has changed.
   */

  public void setConfigurationChanged()
  {
    this.commands.add(CMD_CONFIGURATION_CHANGED);
  }

  /**
   * Send a message to the queue.
   *
   * @param message The message
   */

  public void sendMessage(
    final MMessageType message)
  {
    this.commands.add(new CmdSendMessage(message));
  }

  private void configureMessageQueue()
    throws Exception
  {
    if (this.resources != null) {
      this.resources.close();
    }

    final var brokerConfigurationOpt = this.loadBrokerConfiguration();
    if (brokerConfigurationOpt.isEmpty()) {
      return;
    }

    final var brokerConfiguration =
      brokerConfigurationOpt.get();

    this.resources = CloseableCollection.create();

    LOG.info("connect {}", brokerConfiguration.brokerURL);

    final var transportConfiguration =
      new TransportConfiguration(NettyConnectorFactory.class.getName());
    final var connections =
      this.resources.add(
        ActiveMQJMSClient.createConnectionFactoryWithoutHA(
          CF,
          transportConfiguration
        ));

    connections.setBrokerURL(brokerConfiguration.brokerURL);
    connections.setClientID(UUID.randomUUID().toString());
    connections.setUser(brokerConfiguration.brokerUser);
    connections.setPassword(brokerConfiguration.brokerPass);
    connections.setReconnectAttempts(-1);

    final var topicConnection =
      this.resources.add(connections.createTopicConnection());

    this.session =
      this.resources.add(
        topicConnection.createTopicSession(false, 0)
      );

    final var topic =
      this.session.createTopic(brokerConfiguration.brokerTopic);
    this.publisher =
      this.resources.add(this.session.createPublisher(topic));

    topicConnection.start();
  }

  private Optional<BrokerConfiguration> loadBrokerConfiguration()
    throws SQLException
  {
    final BrokerConfiguration brokerConfiguration;
    try (var connection = this.database.openConnection()) {
      final var context =
        DSL.using(connection, SQLDialect.DERBY);
      final var mqConfiguration =
        context.fetchOne(MESSAGE_QUEUE);
      connection.rollback();

      if (!mqConfiguration.getMqEnabled()) {
        return Optional.empty();
      }

      brokerConfiguration = new BrokerConfiguration(
        mqConfiguration.getMqUrl(),
        mqConfiguration.getMqUser(),
        mqConfiguration.getMqPassword(),
        mqConfiguration.getMqTopic()
      );
    }
    return Optional.of(brokerConfiguration);
  }

  enum CmdConfigurationChanged
    implements CommandType
  {
    CMD_CONFIGURATION_CHANGED
  }

  sealed interface CommandType
  {

  }

  record CmdSendMessage(
    MMessageType message)
    implements CommandType
  {

  }

  private record BrokerConfiguration(
    String brokerURL,
    String brokerUser,
    String brokerPass,
    String brokerTopic
  )
  {

  }
}
