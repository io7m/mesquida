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

package com.io7m.mesquida.internal;

import com.io7m.mesquida.internal.database.MDatabase;
import com.io7m.mesquida.internal.mq.MMessageService;
import com.io7m.mesquida.internal.private_api.MPrivErrorHandler;
import com.io7m.mesquida.internal.private_api.MPrivMessageQueuePutHandler;
import com.io7m.mesquida.internal.private_api.MPrivRootHandler;
import com.io7m.mesquida.internal.private_api.MPrivStreamAddressPutHandler;
import com.io7m.mesquida.internal.private_api.MPrivStreamDeleteHandler;
import com.io7m.mesquida.internal.private_api.MPrivStreamListHandler;
import com.io7m.mesquida.internal.private_api.MPrivStreamPutHandler;
import com.io7m.mesquida.internal.private_api.MPrivStreamStartHandler;
import com.io7m.mesquida.internal.private_api.MPrivStreamStopHandler;
import com.io7m.mesquida.internal.private_api.MPrivUserListHandler;
import com.io7m.mesquida.internal.private_api.MPrivUserPutHandler;
import com.io7m.mesquida.internal.public_api.MPubPages;
import com.io7m.mesquida.internal.public_api.MPubRootHandler;
import com.io7m.mesquida.internal.public_api.MPubServletHolder;
import com.io7m.mesquida.internal.public_api.MPubStaticHandler;
import com.io7m.mesquida.internal.public_api.MPubStreamEditHandler;
import org.eclipse.jetty.jmx.MBeanContainer;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.server.handler.StatisticsHandler;
import org.eclipse.jetty.server.session.DefaultSessionCache;
import org.eclipse.jetty.server.session.DefaultSessionIdManager;
import org.eclipse.jetty.server.session.FileSessionDataStore;
import org.eclipse.jetty.server.session.SessionHandler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.Objects;

/**
 * The main server.
 */

public final class MServerMain implements AutoCloseable
{
  private static final Logger LOG =
    LoggerFactory.getLogger(MServerMain.class);

  private final Server serverPrivate;
  private final MServerConfiguration configuration;
  private final Server serverPublic;
  private final MMessageService messageQueue;

  private MServerMain(
    final MServerConfiguration inConfiguration,
    final Server inServerPublic,
    final Server inServerPrivate,
    final MMessageService inMessageQueue)
  {
    this.configuration =
      Objects.requireNonNull(inConfiguration, "configuration");
    this.serverPrivate =
      Objects.requireNonNull(inServerPrivate, "server");
    this.serverPublic =
      Objects.requireNonNull(inServerPublic, "server");
    this.messageQueue =
      Objects.requireNonNull(inMessageQueue, "messageQueue");
  }

  /**
   * Create a server.
   *
   * @param configuration The server configuration
   * @param database      The database
   *
   * @return A server instance
   *
   * @throws IOException On errors
   */

  public static MServerMain create(
    final MServerConfiguration configuration,
    final MDatabase database)
    throws IOException
  {
    Objects.requireNonNull(configuration, "configuration");
    Objects.requireNonNull(database, "database");

    final var publicThreadPool =
      new QueuedThreadPool(4, 1);
    final var privateThreadPool =
      new QueuedThreadPool(4, 1);

    final var publicServer =
      new Server(publicThreadPool);
    final var privateServer =
      new Server(privateThreadPool);

    final var mq =
      MMessageService.create(database);

    final var pages = new MPubPages(configuration.locale());
    final var httpConfig = new HttpConfiguration();
    httpConfig.setSendServerVersion(false);
    httpConfig.setSendXPoweredBy(false);

    createPublicConnectors(configuration, publicServer, httpConfig);
    createPublicHandlers(configuration, publicServer, pages, database);
    createPrivateHandlers(configuration, privateServer, database, mq);
    createPrivateConnectors(configuration, privateServer, httpConfig);

    return new MServerMain(
      configuration,
      publicServer,
      privateServer,
      mq
    );
  }

  private static void createPrivateHandlers(
    final MServerConfiguration configuration,
    final Server server,
    final MDatabase database,
    final MMessageService messageService)
  {
    final var contextRoot =
      new ContextHandler("/");
    contextRoot.setHandler(
      new MPrivRootHandler());

    final var contextUserPut =
      new ContextHandler("/user-put");
    contextUserPut.setHandler(
      new MPrivUserPutHandler(configuration, database));

    final var contextUserList =
      new ContextHandler("/user-list");
    contextUserList.setHandler(
      new MPrivUserListHandler(configuration, database));

    final var contextStreamPut =
      new ContextHandler("/stream-put");
    contextStreamPut.setHandler(
      new MPrivStreamPutHandler(configuration, database));

    final var contextStreamList =
      new ContextHandler("/stream-list");
    contextStreamList.setHandler(
      new MPrivStreamListHandler(configuration, database));

    final var contextStreamAddressPut =
      new ContextHandler("/stream-address-put");
    contextStreamAddressPut.setHandler(
      new MPrivStreamAddressPutHandler(configuration, database));

    final var contextStreamDelete =
      new ContextHandler("/stream-delete");
    contextStreamDelete.setHandler(
      new MPrivStreamDeleteHandler(configuration, database));

    final var contextStreamStart =
      new ContextHandler("/stream-start");
    contextStreamStart.setHandler(
      new MPrivStreamStartHandler(configuration, messageService, database));

    final var contextStreamStop =
      new ContextHandler("/stream-stop");
    contextStreamStop.setHandler(
      new MPrivStreamStopHandler(configuration, messageService, database));

    final var contextMqPut =
      new ContextHandler("/mq-put");
    contextMqPut.setHandler(
      new MPrivMessageQueuePutHandler(configuration, messageService, database));

    final var contexts = new ContextHandlerCollection();
    contexts.setHandlers(new Handler[]{
      contextRoot,
      contextUserPut,
      contextUserList,
      contextStreamPut,
      contextStreamList,
      contextStreamAddressPut,
      contextStreamDelete,
      contextStreamStart,
      contextStreamStop,
      contextMqPut,
    });
    server.setErrorHandler(new MPrivErrorHandler());
    server.setHandler(contexts);
  }

  private static void createPublicHandlers(
    final MServerConfiguration configuration,
    final Server server,
    final MPubPages pages,
    final MDatabase database)
  {
    /*
     * Set up a servlet container.
     */

    final var servlets = new ServletContextHandler();

    servlets.addServlet(
      new MPubServletHolder<>(
        MPubRootHandler.class, () -> new MPubRootHandler(pages, database)),
      "/"
    );

    servlets.addServlet(
      new MPubServletHolder<>(
        MPubStaticHandler.class, MPubStaticHandler::new),
      "/static/*"
    );

    servlets.addServlet(
      new MPubServletHolder<>(
        MPubStreamEditHandler.class,
        () -> new MPubStreamEditHandler(pages, database)),
      "/stream-edit/"
    );

    /*
     * Set up a session handler that allows for Servlets to have sessions
     * that can survive server restarts.
     */

    final var sessionIds = new DefaultSessionIdManager(server);
    final var sessionHandler = new SessionHandler();

    final var sessionStore = new FileSessionDataStore();
    sessionStore.setStoreDir(configuration.sessionDirectory().toFile());

    final var sessionCache = new DefaultSessionCache(sessionHandler);
    sessionCache.setSessionDataStore(sessionStore);

    sessionHandler.setSessionCache(sessionCache);
    sessionHandler.setSessionIdManager(sessionIds);
    sessionHandler.setHandler(servlets);

    /*
     * Set up an MBean container so that the statistics handler can export
     * statistics to JMX.
     */

    final var mbeanContainer =
      new MBeanContainer(ManagementFactory.getPlatformMBeanServer());
    server.addBean(mbeanContainer);

    /*
     * Set up a statistics handler that wraps everything.
     */

    final var statsHandler = new StatisticsHandler();
    statsHandler.setHandler(sessionHandler);
    server.setHandler(statsHandler);
  }

  private static void createPrivateConnectors(
    final MServerConfiguration configuration,
    final Server server,
    final HttpConfiguration httpConfig)
  {
    final var httpConnectionFactory =
      new HttpConnectionFactory(httpConfig);
    final var baseConnector =
      new ServerConnector(server, httpConnectionFactory);

    final var bindAddress =
      configuration.privateAddress();
    final var bindPort =
      configuration.privatePort();

    baseConnector.setReuseAddress(true);
    baseConnector.setHost(bindAddress.getHostAddress());
    baseConnector.setPort(bindPort);

    for (final var connector : server.getConnectors()) {
      try {
        connector.stop();
      } catch (final Exception e) {
        LOG.error("could not close connector: ", e);
      }
    }

    server.setConnectors(new Connector[]{
      baseConnector,
    });
  }

  private static void createPublicConnectors(
    final MServerConfiguration configuration,
    final Server server,
    final HttpConfiguration httpConfig)
  {
    final var httpConnectionFactory =
      new HttpConnectionFactory(httpConfig);
    final var baseConnector =
      new ServerConnector(server, httpConnectionFactory);

    final var bindAddress =
      configuration.publicAddress();
    final var bindPort =
      configuration.publicPort();

    baseConnector.setReuseAddress(true);
    baseConnector.setHost(bindAddress.getHostAddress());
    baseConnector.setPort(bindPort);

    for (final var connector : server.getConnectors()) {
      try {
        connector.stop();
      } catch (final Exception e) {
        LOG.error("could not close connector: ", e);
      }
    }

    server.setConnectors(new Connector[]{
      baseConnector,
    });
  }

  /**
   * Start the web server(s).
   *
   * @throws Exception On errors
   */

  public void start()
    throws Exception
  {
    LOG.info(
      "private server starting on http://{}:{}",
      this.configuration.privateAddress().getHostAddress(),
      Integer.valueOf(this.configuration.privatePort())
    );
    this.serverPrivate.start();

    LOG.info(
      "public server starting on http://{}:{}",
      this.configuration.publicAddress().getHostAddress(),
      Integer.valueOf(this.configuration.publicPort())
    );
    this.serverPublic.start();
  }

  @Override
  public void close()
    throws Exception
  {
    LOG.debug("stopping private server");
    this.serverPrivate.stop();
    LOG.debug("stopping public server");
    this.serverPublic.stop();
    LOG.debug("stopping message queue");
    this.messageQueue.close();
  }
}
