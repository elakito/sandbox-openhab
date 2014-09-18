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
import java.util.Map;

import org.apache.camel.component.directvm.DirectVmEndpoint;
import org.openhab.binding.camel.CamelBindingProvider;

/**
 * @author elakito
 * @since 1.6.0
 */
abstract class AbstractCamelConnector<P extends CamelBindingProvider> {
    
    protected AbstractCamelConnectorManager<P> manager;
    protected DirectVmEndpoint endpoint;
    protected String name;
    protected Map<String, CamelBindingProvider> listeners = 
        Collections.synchronizedMap(new HashMap<String, CamelBindingProvider>());
    private int count;
        
    public AbstractCamelConnector(String name, DirectVmEndpoint endpoint, AbstractCamelConnectorManager<P> manager) {
        this.name = name;
        this.endpoint = endpoint;
        this.manager = manager;
    }
    
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void register(String itemName, CamelBindingProvider bindingProvider) {
        if (bindingProvider != null) {
            listeners.put(itemName, bindingProvider);
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
    
    
    public abstract void start();

    public abstract void stop();

}
