package com.rmac.comms;

import com.google.gson.Gson;
import com.pty4j.WinSize;
import com.rmac.RMAC;
import com.rmac.core.Terminal;
import com.rmac.utils.Pair;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.drafts.Draft;
import org.java_websocket.handshake.ServerHandshake;

/**
 * The web-socket client of the socket communication between this RMAC Host and the RMAC Bridge
 * Server.
 */
@Slf4j
public class Socket extends WebSocketClient {

  public static Gson GSON = new Gson();
  /**
   * A collection of all the open or orphaned terminal connections between this RMAC Host and the
   * RMAC Console proxied via the Bridge Server.<br/><br/>
   * <p>The identifier for each terminal connection is of the format:
   * '{ConsoleId}:{TerminalId}'</p>
   */
  public Map<String, Terminal> terminals = new ConcurrentHashMap<>();

  public Socket(URI serverUri, Draft draft) {
    super(serverUri, draft);
  }

  /**
   * Create new Web socket client from url.
   *
   * @param url The websocket url.
   * @throws URISyntaxException when the provided url is malformed.
   */
  public Socket(String url) throws URISyntaxException {
    super(new URI(url));
  }

  @Override
  public void onOpen(ServerHandshake serverHandshake) {
    log.info("Connected to RMAC bridging server");

    this.emit(Message.create("identity", null, RMAC.config));
  }

  @Override
  public void onMessage(String raw) {
    this.parse(raw);
  }

  @Override
  public void onClose(int i, String s, boolean b) {
    log.warn("Connection closed to RMAC bridging server");

    this.terminals.forEach((id, terminal) -> terminal.orphaned());

    if (Objects.isNull(RMAC.bridgeClient.thread)) {
      RMAC.bridgeClient.reconnect();
    }
  }

  @Override
  public void onError(Exception e) {
    log.warn("BridgeClient error", log.isDebugEnabled() ? e : null);
  }

  /**
   * Send a message in JSON.
   *
   * @param message The message to be sent.
   */
  public void emit(Message message) {
    if (this.isOpen()) {
      this.send(GSON.toJson(message));
    }
  }

  /**
   * Parse a received JSON message and act upon it.
   *
   * @param data The received message in JSON string.
   */
  public void parse(String data) {
    Message message = GSON.fromJson(data, Message.class);

    switch (message.getEvent()) {
      case "config": {
        this.emit(Message.create("config", message.getRayId(), RMAC.config));
        break;
      }
      case "command": {
        String command = (String) message.data;
        String[] parts = command.split(" ");

        if (parts.length > 0 && !"".equals(parts[0])) {
          try {
            RMAC.commandHandler.execute(new String[]{command});
          } catch (IOException e) {
            log.error("Socket:command", e);
          }
        }
        break;
      }
      case "terminal:open": {
        if (!terminals.containsKey(message.rayId)) {
          Terminal terminal = Terminal.create(message.rayId, this, null);
          terminals.put(message.rayId, terminal);
          terminal.start();
        }
        log.info("Active Terminals: {}", terminals.size());
        break;
      }
      case "terminal:data": {
        if (!terminals.containsKey(message.rayId)) {
          return;
        }

        Terminal term = this.terminals.get(message.rayId);
        try {
          term.write((String) message.data);
        } catch (IOException e) {
          log.error("Terminal Error", e);
        }
        break;
      }
      case "terminal:resize": {
        if (terminals.containsKey(message.rayId)) {
          String[] dimensions = ((String) message.data).split(":");

          Terminal term = this.terminals.get(message.rayId);
          if (Objects.isNull(term.process)) {
            term.initialDimension = new Pair<>(Integer.parseInt(dimensions[0]),
                Integer.parseInt(dimensions[1]));
          } else {
            term.process.setWinSize(
                new WinSize(Integer.parseInt(dimensions[0]), Integer.parseInt(dimensions[1])));
          }
        }
        break;
      }
      case "terminal:close": {
        if (terminals.containsKey(message.rayId)) {
          terminals.get(message.rayId).shutdown(false);
        }
        log.info("Active Terminals: {}", terminals.size());
        break;
      }
      default:
        break;
    }
  }

  public static Socket create(String url) throws URISyntaxException {
    return new Socket(url);
  }
}
