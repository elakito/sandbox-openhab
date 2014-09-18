/**
 * Copyright (c) 2010-2014, openHAB.org and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.camel.internal;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.camel.CamelContext;
import org.apache.camel.component.directvm.DirectVmComponent;
import org.apache.camel.component.directvm.DirectVmEndpoint;
import org.apache.camel.impl.DefaultCamelContext;
import org.openhab.binding.camel.CamelBindingProvider;

/**
 * @author elakito
 * @since 1.6.0
 */
abstract class AbstractCamelConnectorManager<P extends CamelBindingProvider> {
	private static final String ENDPOINT_PREFIX = "direct-vm:";
	protected AbstractCamelConnectorBinding<P> binding;
	private CamelContext context;
	private DirectVmComponent component;

    // a map to store a camel native endpoint for the given endpoint name
    private Map<String, DirectVmEndpoint> endpoints;
    // a map to store a connector for the given endpoint name
    protected Map<String, AbstractCamelConnector<P>> connectors;
    // a map to store all connectors for the given itemName
    private Map<String, List<AbstractCamelConnector<P>>> itemconnectors;

    public AbstractCamelConnectorManager(AbstractCamelConnectorBinding<P> binding) {
    	this.binding = binding;
        this.context = new DefaultCamelContext();
       	this.component = new DirectVmComponent();
        component.setCamelContext(context);
    }

    DirectVmComponent getCamelComponent() {
		return component;
	}

    CamelContext getCamelContext() {
		return context;
	}

	/**
     * Add a camel consumer endpoint for the specified name if there is previously no
     * consumer with this name has been added for this item.
     * 
     * @param itemName
     * @param name
     * @param listener
     * @throws Exception 
     */
    public AbstractCamelConnector<P> add(String itemName, String name, CamelBindingProvider bindingProvider) throws Exception {
    	AbstractCamelConnector<P> connector = null;
        synchronized (this) {
            List<AbstractCamelConnector<P>> icos = itemconnectors.get(itemName);
            if (icos == null) {
                icos = new LinkedList<AbstractCamelConnector<P>>();
                itemconnectors.put(itemName, icos);
            }
            for (AbstractCamelConnector<P> ic : icos) {
                if (name.equals(ic.getName())) {
                    connector = ic;
                    break;
                }
            }
            if (connector == null) {
                connector = connectors.get(name);
                if (connector == null) {
                	DirectVmEndpoint endpoint = endpoints.get(name);
                	if (endpoint == null) {
                		endpoint = (DirectVmEndpoint)component.createEndpoint(ENDPOINT_PREFIX + name);
                		endpoints.put(name,  endpoint);
                	}
                    connector = createConnector(name, endpoint, this);
                    connector.start();
                    connectors.put(name,  connector);
                }
                icos.add(connector);
            }
            // increment the reference count for the inbound item referencing this consumer
            connector.register(itemName, bindingProvider);
        }
        return connector;
    }

    /**
     * Remove the dispatchers previously assigned to the specified item
     * and close and remove them if they are no longer assigned to any other item.
     * @param itemName
     */
    public void remove(String itemName) {
        synchronized (this) {
            List<AbstractCamelConnector<P>> icos = itemconnectors.remove(itemName);
            if (icos != null) {
                for (AbstractCamelConnector<P> ic : icos) {
                	ic.stop();
                	ic.unregister(itemName);
                    if (ic.getReferenceCount() == 0) {
                        connectors.remove(ic.getName());
                    }
                }
            }
        }
    }

    /**
     * Posts the specified message to the event bus.
     * 
     * @param itemName
     * @param message
     * @param provider
     */
    public void postMessage(String itemName, Object message, CamelBindingProvider provider) {
    	//TODO for now, we just convert the message to string and post it to the bus.
    	binding.postToBus(itemName, message.toString(), provider);
    }


    public void init() {
    	connectors = Collections.synchronizedMap(new HashMap<String, AbstractCamelConnector<P>>());       
        itemconnectors = new HashMap<String, List<AbstractCamelConnector<P>>>();
        endpoints = new HashMap<String, DirectVmEndpoint>();
    }
        
    public void release() {
    	for (AbstractCamelConnector<P> connector : connectors.values()) {
    		connector.stop();
    	}
    	connectors.clear();
    	itemconnectors.clear();
    	endpoints.clear();
    	
        connectors = null;
        itemconnectors = null;
        endpoints = null;
    }

    abstract protected AbstractCamelConnector<P> createConnector(String name, DirectVmEndpoint endpoint, AbstractCamelConnectorManager<P> connectorManager);
}
