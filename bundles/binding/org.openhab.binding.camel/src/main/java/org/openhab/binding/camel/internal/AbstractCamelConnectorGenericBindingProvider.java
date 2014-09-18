/**
 * Copyright (c) 2010-2014, openHAB.org and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.camel.internal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.openhab.binding.camel.CamelBindingProvider;
import org.openhab.core.binding.BindingConfig;
import org.openhab.core.items.Item;
import org.openhab.core.library.types.StringType;
import org.openhab.core.types.Command;
import org.openhab.core.types.TypeParser;
import org.openhab.model.item.binding.AbstractGenericBindingProvider;
import org.openhab.model.item.binding.BindingConfigParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * This class contains the common part of the dispatcher and provider binding configuration.
 * 
 * @see CamelDispatcherGenericBindingProvider
 * @see CamelProviderGenericBindingProvider
 * 
 * @author elakito
 * @since 1.6.0
 */
public abstract class AbstractCamelConnectorGenericBindingProvider extends AbstractGenericBindingProvider implements CamelBindingProvider {
    private static final Logger logger = LoggerFactory.getLogger(AbstractCamelConnectorGenericBindingProvider.class);

    protected static final Command IN_COMMAND_KEY = StringType.valueOf("IN");
    protected static final Command CHANGED_COMMAND_KEY = StringType.valueOf("CHANGED");
    protected static final Command WILDCARD_COMMAND_KEY = StringType.valueOf("*");

    /** {@link Pattern} which matches a binding configuration part */
    private static final Pattern BASE_CONFIG_PATTERN = Pattern.compile("([<|>]\\[.*?\\])*");
    private static final Pattern  CONFIG_PATTERN = Pattern.compile("(<|>)\\[(.*?):?(//.*):\'?(.*?)\'?\\]");

    /**
     * {@inheritDoc}
     */
    public abstract String getBindingType();

    
	/**
	 * {@inheritDoc}
 	*/
	@Override
	public void processBindingConfiguration(String context, Item item, String bindingConfig) throws BindingConfigParseException {
    	super.processBindingConfiguration(context, item, bindingConfig);
    	if (bindingConfig != null) {
        	CamelConnectorBindingConfig config = parseBindingConfig(item, bindingConfig);
        	addBindingConfig(item, config);
    	}
    	else {
	        logger.warn("bindingConfig is NULL (item=" + item + ") -> process bindingConfig aborted!");
    	}
	}

	protected CamelConnectorBindingConfig parseBindingConfig(Item item, String bindingConfig) throws BindingConfigParseException {
                
        CamelConnectorBindingConfig config = new CamelConnectorBindingConfig();
        config.itemType = item.getClass();
        
        Matcher matcher = BASE_CONFIG_PATTERN.matcher(bindingConfig);
        
        if (!matcher.matches()) {
            throw new BindingConfigParseException("bindingConfig '" + bindingConfig + "' doesn't contain a valid binding configuration");
        }
        matcher.reset();
        while (matcher.find()) {
            String configPart = matcher.group(1);
            if (StringUtils.isNotBlank(configPart)) {
                parseBindingConfig(config, item, configPart);
            }
        }
        return config;
    }

    protected void parseBindingConfig(CamelConnectorBindingConfig config, Item item, String bindingConfig) 
        throws BindingConfigParseException {
        Matcher matcher = CONFIG_PATTERN.matcher(bindingConfig);
        
        if (!matcher.matches()) {
            throw new BindingConfigParseException(getBindingType() +
                                                  " binding configuration must consist of two or three parts [config=" + matcher + "]");
        } else {
            CamelConnectorBindingConfigElement configElement;
            if(matcher.matches()) {
                String directionStr = matcher.group(1);
                String commandStr = matcher.group(2);
                String name = matcher.group(3);
                String transformation = matcher.group(4);
                if (logger.isDebugEnabled()) {
                        logger.debug("adding a binding for direction={}, command={}, name={}, transformation={}",
                        directionStr, commandStr, name, transformation);
                }

                configElement = new CamelConnectorBindingConfigElement(name, transformation);
                Command command = commandStr.length() == 0 ? IN_COMMAND_KEY : createCommandFromString(item, commandStr);

                config.put(command, configElement);
            }
        }
    }
    
    protected class CamelConnectorBindingConfig extends HashMap<Command, CamelConnectorBindingConfigElement>implements BindingConfig {
		private static final long serialVersionUID = 8524944700047776248L;
		Class<? extends Item> itemType;
    }
    
    static class CamelConnectorBindingConfigElement implements BindingConfig {
        private String name;
        private String transformation;
        
        public CamelConnectorBindingConfigElement(String name, String transformation) {
            this.name = name;
            this.transformation = transformation;
        }
        
        public String getName() {
            return name;
        }
        
        public String getTransformation() {
            return transformation;
        }
        
        @Override
        public String toString() {
            return "CamelConnectorBindingConfigElement [name=" + name + ", transformation=" + transformation + "]";
        }
        
    }
    
    private static Command createCommandFromString(Item item, String commandAsString) throws BindingConfigParseException {
        Command command = TypeParser.parseCommand(item.getAcceptedCommandTypes(), commandAsString);
        
        if (command == null) {
            throw new BindingConfigParseException("couldn't create Command from '" + commandAsString + "' ");
        } 
        
        return command;
    }
    
    @Override
    public String getName(String itemName, Command command) {
        CamelConnectorBindingConfig config = (CamelConnectorBindingConfig) bindingConfigs.get(itemName);
        return config != null && config.get(command) != null ? config.get(command).getName() : null;
    }
    
    @Override
    public String getName(String itemName) {
        return getName(itemName, IN_COMMAND_KEY);
    }

    @Override
    public String getTransformation(String itemName, Command command) {
        CamelConnectorBindingConfig config = (CamelConnectorBindingConfig) bindingConfigs.get(itemName);
        return config != null && config.get(command) != null ? config.get(command).getTransformation() : null;
    }

    @Override
    public String getTransformation(String itemName) {
        return getTransformation(itemName, IN_COMMAND_KEY);
    }

    @Override
    public List<String> getItemNames(Command command) {
        List<String> bindings = new ArrayList<String>();
        for (String itemName : bindingConfigs.keySet()) {
            CamelConnectorBindingConfig config = (CamelConnectorBindingConfig) bindingConfigs.get(itemName);
            if (config.containsKey(command)) {
                bindings.add(itemName);
            }
        }
        return bindings;
    }

    @Override
    public Class<? extends Item> getItemType(String itemName) {
        CamelConnectorBindingConfig config = (CamelConnectorBindingConfig) bindingConfigs.get(itemName);
        return config != null ? config.itemType : null;
    }

    @Override
    public List<Command> getCommands(String itemName) {
        List<Command> commands = new ArrayList<Command>();
        CamelConnectorBindingConfig config = (CamelConnectorBindingConfig) bindingConfigs.get(itemName);
        commands.addAll(config.keySet());
        return commands;
    }
}
