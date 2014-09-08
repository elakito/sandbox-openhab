/**
 * Copyright (c) 2010-2014, openHAB.org and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.websocketclient.internal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.openhab.binding.websocketclient.WebsocketClientBindingProvider;
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
 * This class is responsible for parsing the binding configuration.
 * 
 * in:  websocketclient:"<[<url>:<transformationrule>]"
 * out: websocketclient:">[<command>:<url>:<transformationrule>]"
 * 
 * websocketclient="<[ws://192.168.0.1:3000/service:'some text']" - for String Items
 * websocketclient=">[ON:ws://192.168.0.1:3000/service:'some text'], >[OFF:ws://192.168.0.1:3000/service:'some other command']"
 * 
 * @author elakito
 * @since 1.6.0
 */
public class WebsocketClientGenericBindingProvider extends AbstractGenericBindingProvider implements WebsocketClientBindingProvider {
    private static final Logger logger = LoggerFactory.getLogger(WebsocketClientGenericBindingProvider.class);

    protected static final Command IN_COMMAND_KEY = StringType.valueOf("IN");
    protected static final Command CHANGED_COMMAND_KEY = StringType.valueOf("CHANGED");
    protected static final Command WILDCARD_COMMAND_KEY = StringType.valueOf("*");

    /** {@link Pattern} which matches a binding configuration part */
    private static final Pattern BASE_CONFIG_PATTERN = Pattern.compile("([<|>]\\[.*?\\])*");
    private static final Pattern  CONFIG_PATTERN = Pattern.compile("(<|>)\\[(.*?):?(wss?://.*):(?!\\d+)\'?(.*?)\'?\\]");

    /**
     * {@inheritDoc}
     */
    public String getBindingType() {
        return "websocketclient";
    }

    /**
     * @{inheritDoc}
     */
    @Override
    public void validateItemType(Item item, String bindingConfig) throws BindingConfigParseException {
        //if (!(item instanceof SwitchItem || item instanceof DimmerItem)) {
        //      throw new BindingConfigParseException("item '" + item.getName()
        //                      + "' is of type '" + item.getClass().getSimpleName()
        //                      + "', only Switch- and DimmerItems are allowed - please check your *.items configuration");
        //}
    }
        
    /**
     * {@inheritDoc}
     */
    @Override
    public void processBindingConfiguration(String context, Item item, String bindingConfig) throws BindingConfigParseException {
        super.processBindingConfiguration(context, item, bindingConfig);
        if (bindingConfig != null) {
            WebsocketClientBindingConfig config = parseBindingConfig(item, bindingConfig);
            addBindingConfig(item, config);
        }
        else {
            logger.warn("bindingConfig is NULL (item=" + item + ") -> process bindingConfig aborted!");
        }
    }
        
    protected WebsocketClientBindingConfig parseBindingConfig(Item item, String bindingConfig) throws BindingConfigParseException {
                
        WebsocketClientBindingConfig config = new WebsocketClientBindingConfig();
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

    protected void parseBindingConfig(WebsocketClientBindingConfig config, Item item, String bindingConfig) 
        throws BindingConfigParseException {
        Matcher matcher = CONFIG_PATTERN.matcher(bindingConfig);
        
        if (!matcher.matches()) {
            throw new BindingConfigParseException(getBindingType() +
                                                  " binding configuration must consist of two or three parts [config=" + matcher + "]");
        } else {
            WebsocketClientBindingConfigElement configElement;
            if(matcher.matches()) {
                String directionStr = matcher.group(1);
                String commandStr = matcher.group(2);
                String url = matcher.group(3);
                String transformation = matcher.group(4);
                if (logger.isDebugEnabled()) {
                        logger.debug("adding a binding for direction={}, command={}, url={}, transformation={}",
                        directionStr, commandStr, url, transformation);
                }

                configElement = new WebsocketClientBindingConfigElement(url, transformation);
                Command command = commandStr.length() == 0 ? IN_COMMAND_KEY : createCommandFromString(item, commandStr);

                config.put(command, configElement);
            }
        }
    }
    
    class WebsocketClientBindingConfig extends HashMap<Command, WebsocketClientBindingConfigElement>implements BindingConfig {
        private static final long serialVersionUID = -108946006112637386L;
        Class<? extends Item> itemType;
    }
    
    static class WebsocketClientBindingConfigElement implements BindingConfig {
        private String url;
        private String transformation;
        
        public WebsocketClientBindingConfigElement(String url, String transformation) {
            this.url = url;
            this.transformation = transformation;
        }
        
        public String getUrl() {
            return url;
        }
        
        public String getTransformation() {
            return transformation;
        }
        
        @Override
        public String toString() {
            return "WebsocketClientBindingConfigElement [url=" + url + ", transformation=" + transformation + "]";
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
    public String getUrl(String itemName, Command command) {
        WebsocketClientBindingConfig config = (WebsocketClientBindingConfig) bindingConfigs.get(itemName);
        return config != null && config.get(command) != null ? config.get(command).getUrl() : null;
    }
    
    @Override
    public String getUrl(String itemName) {
        return getUrl(itemName, IN_COMMAND_KEY);
    }

    @Override
    public String getTransformation(String itemName, Command command) {
        WebsocketClientBindingConfig config = (WebsocketClientBindingConfig) bindingConfigs.get(itemName);
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
            WebsocketClientBindingConfig config = (WebsocketClientBindingConfig) bindingConfigs.get(itemName);
            if (config.containsKey(command)) {
                bindings.add(itemName);
            }
        }
        return bindings;
    }

    @Override
    public Class<? extends Item> getItemType(String itemName) {
        WebsocketClientBindingConfig config = (WebsocketClientBindingConfig) bindingConfigs.get(itemName);
        return config != null ? config.itemType : null;
    }

    @Override
    public List<Command> getCommands(String itemName) {
        List<Command> commands = new ArrayList<Command>();
        WebsocketClientBindingConfig config = (WebsocketClientBindingConfig) bindingConfigs.get(itemName);
        commands.addAll(config.keySet());
        return commands;
    }
}
