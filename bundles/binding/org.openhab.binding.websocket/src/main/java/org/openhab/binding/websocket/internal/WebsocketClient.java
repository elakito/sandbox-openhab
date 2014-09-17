package org.openhab.binding.websocket.internal;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.openhab.binding.websocket.WebsocketClientBindingProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClientConfig;
import com.ning.http.client.websocket.WebSocket;
import com.ning.http.client.websocket.WebSocketByteListener;
import com.ning.http.client.websocket.WebSocketTextListener;
import com.ning.http.client.websocket.WebSocketUpgradeHandler;

/**
 *
 */
class WebsocketClient {
    private static Logger logger = LoggerFactory.getLogger(WebsocketClient.class); 
    private WebsocketClientManager manager;
    private AsyncHttpClient client;
    private WebSocket websocket;
    // the websocket url of this client
    private String url;
    // a map to store all inbound items connected to this client : itemName -> bindingProvider*
    private Map<String, WebsocketClientBindingProvider> listeners = 
        Collections.synchronizedMap(new HashMap<String, WebsocketClientBindingProvider>());
    // the reference count of inbound and outbound items associated with this client
    private int count;
        
    public WebsocketClient(String url, AsyncHttpClientConfig config, WebsocketClientManager manager) {
        this.url = url;
        this.client = config == null ? new AsyncHttpClient() : new AsyncHttpClient(config);
        this.manager = manager;
    }
    
    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public void connect() throws IOException {
        logger.debug("connecting to {}", url);
        try {
            websocket = client.prepareGet(url).execute(
                new WebSocketUpgradeHandler.Builder().addWebSocketListener(new ClientListener()).build()).get();
            logger.debug("connected");
        } catch (Exception e) {
            logger.error("Failed to connect", url);
            throw e instanceof IOException ? (IOException)e : new IOException(e);
        }
    }

    public void close() {
        if (isConnected()) {
            websocket.close();
        }
        if (!client.isClosed()) {
            client.close();
        }
    }
    
    public boolean isConnected() {
        return websocket != null && websocket.isOpen();
    }
    
    public void register(String itemName, WebsocketClientBindingProvider provider) {
        if (provider != null) {
            listeners.put(itemName, provider);
        }
        count++;
    }

    public void unregister(String itemName) {
        count--;
        listeners.remove(itemName);
    }
    
    public int getReferenceCount() {
        return count;
    }
    
    public void ensureConnected() throws IOException {
        if (!isConnected()) {
                connect();
        }
    }
    
    public void sendTextMessage(String message) {
        logger.debug("sendTextMessage({})", message);
        websocket.sendTextMessage(message);
    }

    public void sendMessage(byte[] message) {
        logger.debug("sendMessage({})", message);
        websocket.sendMessage(message);
    }
    
    class ClientListener implements WebSocketTextListener, WebSocketByteListener {

        @Override
        public void onClose(WebSocket websocket) {
            logger.debug("onClose({})", websocket);
        }

        @Override
        public void onError(Throwable t) {
            logger.error("onError({}))", t);
        }

        @Override
        public void onOpen(WebSocket websocket) {
            logger.debug("onOpen({})", websocket);
        }

        @Override
        public void onFragment(byte[] message, boolean last) {
            logger.warn("Not supported: onFragment(byte[] message, boolean last)");
        }

        @Override
        public void onMessage(byte[] message) {
            logger.debug("onMessage({})", message);
            logger.warn("Not supported: onMessage(byte[] message");
            //TODO add the byte[] posting mode
        }

        @Override
        public void onFragment(String message, boolean last) {
            logger.warn("Not supported onFragment(String message, boolean last)");
        }

        @Override
        public void onMessage(String message) {
            logger.debug("onMessage({})", message);
            for (Map.Entry<String, WebsocketClientBindingProvider> entry : listeners.entrySet()) {
                manager.postMessage(entry.getKey(), message, entry.getValue());
            }
        }
    }
}
