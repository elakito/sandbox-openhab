/**
 * Copyright (c) 2010-2014, openHAB.org and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.camel.internal;

import org.openhab.binding.camel.CamelProviderBindingProvider;
import org.openhab.core.types.Command;
import org.openhab.core.types.State;
import org.osgi.service.cm.ManagedService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
	

/**
 * Implement this class if you are going create an actively polling service
 * like querying a Website/Device.
 * 
 * @author elakito
 * @since 1.6.0
 */
public class CamelProviderBinding extends AbstractCamelConnectorBinding<CamelProviderBindingProvider> implements ManagedService {

	private static final Logger logger = LoggerFactory.getLogger(CamelProviderBinding.class);


	/**
	 * @{inheritDoc}
	 */
	@Override
	protected void internalReceiveCommand(String itemName, Command command) {
		// the code being executed when a command was sent on the openHAB
		// event bus goes here. This method is only called if one of the 
		// BindingProviders provide a binding for the given 'itemName'.
		logger.debug("internalReceiveCommand() is called!");
	}
	
	/**
	 * @{inheritDoc}
	 */
	@Override
	protected void internalReceiveUpdate(String itemName, State newState) {
		// the code being executed when a state was sent on the openHAB
		// event bus goes here. This method is only called if one of the 
		// BindingProviders provide a binding for the given 'itemName'.
		logger.debug("internalReceiveCommand() is called!");
	}

	@Override
	protected AbstractCamelConnectorManager<CamelProviderBindingProvider> createConnectorManager(AbstractCamelConnectorBinding<CamelProviderBindingProvider> binding) {
		return new CamelProviderManager((CamelProviderBinding)binding);
	}

}
