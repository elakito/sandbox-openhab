/**
 * Copyright (c) 2010-2014, openHAB.org and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.camel.internal;

import org.apache.camel.component.directvm.DirectVmEndpoint;
import org.openhab.binding.camel.CamelDispatcherBindingProvider;

/**
 * @author elakito
 * @since 1.6.0
 */
class CamelDispatcherManager extends AbstractCamelConnectorManager<CamelDispatcherBindingProvider> {

    public CamelDispatcherManager(CamelDispatcherBinding binding) {
		super(binding);
    }

    /**
     * Sends the message to the specified camel dispatcher target.
     * 
     * @param name
     * @param message
     */
	public void sendTextMessage(String name, String message) {
		CamelDispatcher connector = (CamelDispatcher)connectors.get(name);
		connector.sendTextMessageToCamel(message);
	}

    /**
     * Sends the message to the specified camel dispatcher target.
     * 
     * @param name
     * @param message
     */
	public void sendBytesMessage(String name, byte[] message) {
		CamelDispatcher connector = (CamelDispatcher)connectors.get(name);
		connector.sendBytesMessageToCamel(message);
	}

	@Override
	protected AbstractCamelConnector<CamelDispatcherBindingProvider> createConnector(String name, DirectVmEndpoint endpoint, AbstractCamelConnectorManager<CamelDispatcherBindingProvider> connectorManager) {
		return new CamelDispatcher(name, endpoint, this);
	}
}
