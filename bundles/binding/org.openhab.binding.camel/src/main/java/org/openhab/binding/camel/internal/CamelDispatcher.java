/**
 * Copyright (c) 2010-2014, openHAB.org and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.camel.internal;

import java.util.Map;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.component.directvm.DirectVmEndpoint;
import org.apache.camel.impl.DefaultExchange;
import org.openhab.binding.camel.CamelBindingProvider;
import org.openhab.binding.camel.CamelDispatcherBindingProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author elakito
 * @since 1.6.0
 */
class CamelDispatcher extends AbstractCamelConnector<CamelDispatcherBindingProvider> {
    private static Logger logger = LoggerFactory.getLogger(CamelDispatcher.class); 
    public CamelDispatcher(String name, DirectVmEndpoint endpoint, CamelDispatcherManager manager) {
    	super(name, endpoint, manager);
    }

    public void sendMessageToCamel(Object message) {
        logger.debug("sendMessage({})", message);
        
        Exchange exchange = new DefaultExchange(manager.getCamelContext());
        exchange.getIn().setBody(message);
        try {
			manager.getCamelComponent().getConsumer(endpoint).getProcessor().process(exchange);
		} catch (Exception e) {
			logger.error("failed to send message to " + endpoint, e);
		}
        Message response = exchange.getIn();
		if (response != null && response.getBody() != null) {
			for (Map.Entry<String, CamelBindingProvider> entry : listeners.entrySet()) {
                ((CamelDispatcherManager)manager).postMessage(entry.getKey(), response.getBody(), entry.getValue());
            }	
		}
    }

    public void sendTextMessageToCamel(String message) {
        logger.debug("sendTextMessage({})", message);
        sendMessageToCamel(message);
    }

    public void sendBytesMessageToCamel(byte[] message) {
        logger.debug("sendMessage({})", message);
        sendMessageToCamel(message);
    }

    public void start() {
    }
    
    public void stop() {
    }
}
