package cern.c2mon.server.client.lifecycle;

import java.util.concurrent.ThreadPoolExecutor;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.SmartLifecycle;
import org.springframework.jms.connection.SingleConnectionFactory;
import org.springframework.jms.listener.DefaultMessageListenerContainer;
import org.springframework.stereotype.Service;

import cern.tim.server.common.config.ServerConstants;

/**
 * Bean managing lifecycle of client module.
 * 
 * @author Mark Brightwell
 *
 */
@Service
public class ClientModuleLifecycle implements SmartLifecycle {

  /**
   * Class logger.
   */
  private static final Logger LOGGER = Logger.getLogger(ClientModuleLifecycle.class);
  
  /**
   * Flag for lifecycle.
   */
  private volatile boolean running = false;
  
  /**
   * JMS client container.
   */
  private DefaultMessageListenerContainer clientJmsContainer;
  
  /**
   * Thread pool used by container.
   */
  private ThreadPoolExecutor clientExecutor;
  
  /**
   * Need to close down the underlying connection.
   */
  private SingleConnectionFactory singleConnectionFactory;
  
  /**
   * Constructor.
   * @param clientJmsContainer JMS container used in client module
   * @param clientExecutor thread pool used by container
   */
  @Autowired
  public ClientModuleLifecycle(@Qualifier("clientRequestJmsContainer") final DefaultMessageListenerContainer clientJmsContainer, 
                                    @Qualifier("clientExecutor") final ThreadPoolExecutor clientExecutor,
                                    @Qualifier("clientSingleConnectionFactory") final SingleConnectionFactory singleConnectionFactory) {
    super();
    this.clientJmsContainer = clientJmsContainer;
    this.clientExecutor = clientExecutor;
    this.singleConnectionFactory = singleConnectionFactory;
  }

  @Override
  public boolean isAutoStartup() {   
    return false;
  }

  @Override
  public void stop(Runnable callback) {
    start();
    callback.run();
  }

  @Override
  public boolean isRunning() {
    return running;
  }

  @Override
  public synchronized void start() {
    running = true;
    clientJmsContainer.start();
    
  }

  @Override
  public synchronized void stop() {    
    running = false;
    try {
      singleConnectionFactory.destroy(); //closes underlying connection
      clientJmsContainer.stop();
      clientExecutor.shutdown();
    } catch (Exception e) {
      LOGGER.error("Exception caught while shutting down Client JMS connection/JMS container", e);
    }
    
  }

  @Override
  public int getPhase() {
    return ServerConstants.PHASE_START_LAST;
  }

}
