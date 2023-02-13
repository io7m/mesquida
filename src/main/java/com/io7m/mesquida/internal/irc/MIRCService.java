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
import org.pircbotx.Configuration;
import org.pircbotx.PircBotX;
import org.pircbotx.UtilSSLSocketFactory;
import org.pircbotx.exception.IrcException;
import org.pircbotx.hooks.ListenerAdapter;
import org.pircbotx.hooks.events.ConnectAttemptFailedEvent;
import org.pircbotx.hooks.events.ConnectEvent;
import org.pircbotx.hooks.events.DisconnectEvent;
import org.pircbotx.hooks.events.JoinEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.TextMessage;
import javax.jms.TopicSession;
import javax.jms.TopicSubscriber;
import java.io.IOException;
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
 * The IRC service.
 */

public final class MIRCService extends ListenerAdapter
  implements AutoCloseable, Runnable
{
  private static final Logger LOG =
    LoggerFactory.getLogger(MIRCService.class);

  private final ExecutorService executor;
  private final AtomicBoolean done;
  private final LinkedBlockingQueue<CommandType> commands;
  private final MIRCServiceConfiguration configuration;
  private final ExecutorService ircExecutor;
  private final ObjectMapper queueMessageMapper;
  private CloseableCollectionType<ClosingResourceFailedException> resources;
  private TopicSubscriber subscriber;
  private TopicSession session;
  private PircBotX ircBot;

  private MIRCService(
    final MIRCServiceConfiguration inConfiguration,
    final ExecutorService inExecutor,
    final ExecutorService inIrcExecutor)
  {
    this.executor =
      Objects.requireNonNull(inExecutor, "executor");
    this.configuration =
      Objects.requireNonNull(inConfiguration, "configuration");
    this.ircExecutor =
      Objects.requireNonNull(inIrcExecutor, "inIrcExecutor");

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
   * Create a new IRC service.
   *
   * @param configuration The configuration
   *
   * @return A new IRC service
   */

  public static MIRCService create(
    final MIRCServiceConfiguration configuration)
  {
    final var executor =
      Executors.newSingleThreadExecutor(r -> {
        final var thread = new Thread(r);
        thread.setName("com.io7m.mesquida.irc[" + thread.getId() + "]");
        thread.setDaemon(true);
        return thread;
      });

    final var ircExecutor =
      Executors.newSingleThreadExecutor(r -> {
        final var thread = new Thread(r);
        thread.setName("com.io7m.mesquida.irc_bot[" + thread.getId() + "]");
        thread.setDaemon(true);
        return thread;
      });

    final var service = new MIRCService(configuration, executor, ircExecutor);
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
        this.configureIRC();
      } catch (final Exception e) {
        LOG.error("unable to configure irc: ", e);
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

          if (command instanceof CommandIRCDisconnected) {
            break;
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

  private void configureIRC()
    throws IOException, IrcException
  {
    final var tlsFactory = new UtilSSLSocketFactory();

    if (this.configuration.ircUnsafeTrust()) {
      tlsFactory.trustAllCertificates();
    }

    final var builder = new Configuration.Builder();
    builder.addServer(
      this.configuration.ircHost(),
      this.configuration.ircPort());
    builder.addAutoJoinChannel(this.configuration.ircChannel());
    builder.setName(this.configuration.ircUserName());
    builder.setRealName(this.configuration.ircUserName());
    builder.setVersion("mesquida");
    builder.setAutoReconnect(true);
    builder.setSocketFactory(tlsFactory);
    builder.setLogin(this.configuration.ircUserName());
    builder.setSocketTimeout(5_000);
    builder.setAutoReconnectAttempts(Integer.MAX_VALUE);
    builder.setAutoReconnectDelay(1_000);
    builder.addListener(this);

    final var irc_configuration = builder.buildConfiguration();
    this.ircBot = this.resources.add(new PircBotX(irc_configuration));

    this.ircExecutor.execute(() -> {
      try {
        this.ircBot.startBot();
      } catch (final Exception e) {
        LOG.error("irc failed: ", e);
      }
    });
  }

  @Override
  public void onConnect(
    final ConnectEvent event)
    throws Exception
  {
    super.onConnect(event);
    LOG.debug("connected");
  }

  @Override
  public void onJoin(
    final JoinEvent event)
    throws Exception
  {
    super.onJoin(event);
    LOG.debug("joined: {}", event.getChannel().getName());
  }

  @Override
  public void onConnectAttemptFailed(
    final ConnectAttemptFailedEvent event)
    throws Exception
  {
    super.onConnectAttemptFailed(event);

    event.getConnectExceptions().forEach(
      (address, exception) ->
        LOG.error(
          "connection failed: {} - {}",
          address,
          exception.getClass().getCanonicalName(),
          exception.getMessage())
    );
  }

  @Override
  public void onDisconnect(
    final DisconnectEvent event)
    throws Exception
  {
    super.onDisconnect(event);
    this.commands.add(new CommandIRCDisconnected());

    final Exception ex = event.getDisconnectException();
    if (ex != null) {
      LOG.info(
        "disconnected: {} - {}",
        ex.getClass().getCanonicalName(),
        ex.getMessage());
    } else {
      LOG.info("disconnected: (no exception information available)");
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
       * Don't acknowledge the message until the IRC server has accepted
       * our version of it.
       */

      this.ircBot.send()
        .message(this.configuration.ircChannel(), parsed.text);

      textMessage.acknowledge();
    }

    LOG.debug("ignored unrecognized message type: {}", message);
    message.acknowledge();
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

  record CommandIRCDisconnected() implements CommandType
  {

  }

  record CommandJMSReceived(Message message) implements CommandType
  {

  }
}
