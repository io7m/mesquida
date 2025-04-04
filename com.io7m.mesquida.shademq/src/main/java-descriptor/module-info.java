open module com.io7m.mesquida.shademq
{
  requires java.desktop;
  requires java.logging;
  requires java.naming;
  requires java.sql;
  requires org.slf4j;

  uses org.apache.activemq.artemis.spi.core.remoting.ssl.SSLContextFactory;
  uses org.apache.commons.logging.LogFactory;

  exports javax.jms;

  exports org.apache.activemq.artemis.api.core;
  exports org.apache.activemq.artemis.api.jms;
  exports org.apache.activemq.artemis.core.remoting.impl.netty;
  exports org.apache.activemq.artemis.jms.client;
}
