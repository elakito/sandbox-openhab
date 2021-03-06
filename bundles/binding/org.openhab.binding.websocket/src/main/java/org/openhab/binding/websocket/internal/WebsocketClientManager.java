package org.openhab.binding.websocket.internal;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.openhab.binding.websocket.WebsocketClientBindingProvider;

//REVISIT if this is a good way of managing the websockets used by all the configured items
class WebsocketClientManager {
    private WebsocketClientBinding clientBinding;
    // a map to store a client for the given url
    private Map<String, WebsocketClient> urlclients;
    // a map to store all clients associted for the given itemName
    private Map<String, List<WebsocketClient>> itemclients;

    public WebsocketClientManager(WebsocketClientBinding clientBinding) {
        this.clientBinding = clientBinding;
    }

    /**
     * Add a websocket client for the specified url if there is previously no
     * websocket client with this url has been added for this item.
     * 
     * @param itemName
     * @param url
     * @param listener
     */
    public WebsocketClient add(String itemName, String url, WebsocketClientBindingProvider provider) {
        WebsocketClient wc = null;
        synchronized (this) {
            List<WebsocketClient> icls = itemclients.get(itemName);
            if (icls == null) {
                icls = new LinkedList<WebsocketClient>();
                itemclients.put(itemName, icls);
            }
            for (WebsocketClient iwc : icls) {
                if (url.equals(iwc.getUrl())) {
                    wc = iwc;
                    break;
                }
            }
            if (wc == null) {
                wc = urlclients.get(url);
                if (wc == null) {
                    //TODO supply a custom configuration
                    wc = new WebsocketClient(url, null, this);
                    urlclients.put(url,  wc);
                }
                icls.add(wc);
            }
            // increment the reference count for the inbound item referencing this client
            wc.register(itemName, provider);
        }
        return wc;
    }

    /**
     * Remove the websocket clients previously assigned to the specified item
     * and close and remove them if they are no longer assigned to any other item.
     * @param itemName
     */
    public void remove(String itemName) {
        synchronized (this) {
            List<WebsocketClient> icls = itemclients.remove(itemName);
            if (icls != null) {
                for (WebsocketClient iwc : icls) {
                    iwc.unregister(itemName);
                    if (iwc.getReferenceCount() == 0) {
                        iwc.close();
                        urlclients.remove(iwc.getUrl());
                    }
                }
            }
        }
    }
        
    public WebsocketClient getWebsocketClient(String url) {
        return urlclients.get(url);     
    }
        
    public Collection<WebsocketClient> getAllWebsocketClients() {
        return urlclients.values();
    }

    public void postMessage(String itemName, String message, WebsocketClientBindingProvider provider) {
        clientBinding.postToBus(itemName, message, provider);
    }
        
    public void init() {
        urlclients = Collections.synchronizedMap(new HashMap<String, WebsocketClient>());       
        itemclients = new HashMap<String, List<WebsocketClient>>();     
    }
        
    public void release() {
        for (WebsocketClient wc : urlclients.values()) {
            wc.close();
        }
        urlclients = null;
        itemclients = null;
    }
}
