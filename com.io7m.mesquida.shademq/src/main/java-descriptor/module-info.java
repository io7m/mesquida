open module com.io7m.mesquida.shademq
{
  requires java.naming;

  exports javax.jms;

  exports org.apache.activemq.artemis.api.core;
  exports org.apache.activemq.artemis.api.jms;
  exports org.apache.activemq.artemis.core.remoting.impl.netty;
  exports org.apache.activemq.artemis.jms.client;
}
