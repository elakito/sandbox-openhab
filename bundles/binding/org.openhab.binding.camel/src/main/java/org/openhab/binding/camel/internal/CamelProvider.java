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
import org.apache.camel.Processor;
import org.apache.camel.component.directvm.DirectVmConsumer;
import org.apache.camel.component.directvm.DirectVmEndpoint;
import org.openhab.binding.camel.CamelBindingProvider;
import org.openhab.binding.camel.CamelProviderBindingProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * @author elakito
 * @since 1.6.0
 */
class CamelProvider extends AbstractCamelConnector<CamelProviderBindingProvider> {
    private static Logger logger = LoggerFactory.getLogger(CamelProvider.class); 
    private DirectVmConsumer consumer;

    public CamelProvider(String name, DirectVmEndpoint endpoint, CamelProviderManager manager) {
        super(name, endpoint, manager);
    }
    
    public void onMessage(Message message) {
        logger.debug("onMessage({})", message);
		for (Map.Entry<String, CamelBindingProvider> entry : listeners.entrySet()) {
            ((CamelProviderManager)manager).postMessage(entry.getKey(), message.getBody(), entry.getValue());
        }	
	}

    public void start() {
    	if (consumer == null) {
        	try {
		    	consumer = (DirectVmConsumer)endpoint.createConsumer(new Processor() {
			    	@Override
			    	public void process(Exchange exchange) throws Exception {
			        	Message request = exchange.getIn();
			        	onMessage(request);
			    	}
				});
			} catch (Exception e) {
				//REVISIT throw this exception?
				logger.error("failed to create a consumer for " + endpoint, e);
			}
    	}
    	if (consumer != null) {
    		try {
				consumer.start();
			} catch (Exception e) {
				logger.error("failed to start the consumer", e);
			}
    	}
    }
    
    public void stop() {
    	if (consumer != null) {
    		try {
				consumer.shutdown();
			} catch (Exception e) {
				logger.error("failed to stop the consumer", e);
			}
			consumer = null;
    	}
    }
}
