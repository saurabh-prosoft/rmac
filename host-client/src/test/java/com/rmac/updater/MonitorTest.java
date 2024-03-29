package com.rmac.updater;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.Thread.State;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.junit.After;
import org.junit.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.Silent.class)
public class MonitorTest {

  @After
  public void resetAllMocks() {
    Mockito.reset();
  }

  @Test
  @DisplayName("Initialize monitor")
  public void createMonitor() {
    Monitor monitor = spy(new Monitor());

    assertEquals(State.NEW, monitor.thread.getState());
  }

  @Test
  @DisplayName("Start monitor")
  public void startMonitor() {
    Thread mockThread = spy(new Thread(() -> {
    }));
    Monitor monitor = new Monitor();
    monitor.thread = mockThread;

    monitor.start();

    verify(mockThread).start();
  }

  @Test
  @DisplayName("Health check when connection is not established")
  public void healthCheck_NoConnection() {
    Updater.client = null;

    Monitor monitor = new Monitor();
    boolean result = monitor.healthCheck();

    assertFalse(result);
  }

  @Test
  @DisplayName("Health check succeeds")
  public void healthCheck_Success() {
    SocketClient mockSC = mock(SocketClient.class);
    when(mockSC.sendMessage(any())).thenReturn("Up");
    Updater.client = mockSC;

    Monitor monitor = new Monitor();
    boolean result = monitor.healthCheck();

    assertTrue(result);
  }

  @Test
  @DisplayName("Shutdown")
  public void shutdown() {
    Thread mockThread = mock(Thread.class);
    doNothing().when(mockThread).interrupt();

    Monitor monitor = new Monitor();
    monitor.thread = mockThread;
    monitor.shutdown();

    verify(mockThread).interrupt();
  }

  @Test
  @DisplayName("Run Monitor when health is up and sleep is interrupted")
  public void run_HealthUp_Interrupted() throws InterruptedException {
    Monitor monitor = spy(Monitor.class);
    doReturn(true).when(monitor).healthCheck();

    monitor.start();
    ScheduledExecutorService stopper = Executors.newScheduledThreadPool(1);
    stopper.schedule(() -> monitor.thread.interrupt(), 100, TimeUnit.MILLISECONDS);
    monitor.thread.join();

    assertEquals(Long.MAX_VALUE, monitor.healthCheckFailStart);
  }

  @Test
  @DisplayName("Run Monitor when health is down and threshold is reached")
  public void run_HealthDown_Threshold_NotReached() throws InterruptedException {
    Monitor monitor = spy(Monitor.class);
    when(monitor.healthCheck()).thenReturn(false);

    Updater.HEALTH_CHECK_INTERVAL = -1;
    monitor.thread.start();
    monitor.thread.join();

    assertNotEquals(Long.MAX_VALUE, monitor.healthCheckFailStart);
    Mockito.reset(monitor);
  }
}
