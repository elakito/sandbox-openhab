/**
 * Copyright (c) 2010-2014, openHAB.org and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.camel.internal;

import static org.openhab.binding.camel.internal.CamelDispatcherGenericBindingProvider.CHANGED_COMMAND_KEY;

import org.openhab.binding.camel.CamelBindingProvider;
import org.openhab.binding.camel.CamelDispatcherBindingProvider;
import org.openhab.core.types.Command;
import org.openhab.core.types.State;
import org.openhab.core.types.Type;
import org.osgi.service.cm.ManagedService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
        

/**
 * 
 * @author elakito
 * @since 1.6.0
 */
public class CamelDispatcherBinding extends AbstractCamelConnectorBinding<CamelDispatcherBindingProvider> implements ManagedService {

    private static final Logger logger = LoggerFactory.getLogger(CamelDispatcherBinding.class);

    /**
     * @{inheritDoc}
     */
    @Override
    protected void internalReceiveCommand(String itemName, Command command) {
        // the code being executed when a command was sent on the openHAB
        // event bus goes here. This method is only called if one of the 
        // BindingProviders provide a binding for the given 'itemName'.
        logger.debug("internalReceiveCommand() is called!");
        sendToCamel(itemName, command, command);
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
        sendToCamel(itemName, CHANGED_COMMAND_KEY, newState);
    }

    private void sendToCamel(String itemName, Command command, Type type) {
        CamelBindingProvider provider = findFirstMatchingBindingProvider(itemName, command);

        if (logger.isDebugEnabled()) {
            logger.debug("sendToCamel: item={}, command={}, type={}", itemName, command, type);
        }
        
        if (provider == null) {
            logger.trace("doesn't find matching binding provider [itemName={}, command={}]", itemName, command);
            return;
        }
        String name = provider.getName(itemName, command);
        String transformation = provider.getTransformation(itemName, command);
        String transformedMessage = transformMessage(transformation, command.toString());
        try {
        	((CamelDispatcherManager)connectorManager).sendTextMessage(name, transformedMessage);
        } catch (Exception e) {
            logger.error("Unabled to send message to " + name);
        }
    }

	@Override
	protected
	AbstractCamelConnectorManager<CamelDispatcherBindingProvider> createConnectorManager(AbstractCamelConnectorBinding<CamelDispatcherBindingProvider> binding) {
		return new CamelDispatcherManager((CamelDispatcherBinding)binding);
	}
}
