/**
 * Copyright (c) 2010-2014, openHAB.org and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.websocketclient.internal;

import static org.openhab.binding.websocketclient.internal.WebsocketClientGenericBindingProvider.CHANGED_COMMAND_KEY;
import static org.openhab.binding.websocketclient.internal.WebsocketClientGenericBindingProvider.IN_COMMAND_KEY;

import java.util.Dictionary;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.openhab.binding.websocketclient.WebsocketClientBindingProvider;
import org.apache.commons.lang.StringUtils;
import org.openhab.core.binding.AbstractActiveBinding;
import org.openhab.core.binding.BindingProvider;
import org.openhab.core.items.Item;
import org.openhab.core.library.items.ContactItem;
import org.openhab.core.library.items.DateTimeItem;
import org.openhab.core.library.items.NumberItem;
import org.openhab.core.library.items.RollershutterItem;
import org.openhab.core.library.items.SwitchItem;
import org.openhab.core.library.types.DateTimeType;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.library.types.OpenClosedType;
import org.openhab.core.library.types.PercentType;
import org.openhab.core.library.types.StringType;
import org.openhab.core.transform.TransformationHelper;
import org.openhab.core.transform.TransformationService;
import org.openhab.core.types.Command;
import org.openhab.core.types.State;
import org.openhab.core.types.Type;
import org.osgi.service.cm.ConfigurationException;
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
public class WebsocketClientBinding extends AbstractActiveBinding<WebsocketClientBindingProvider> implements ManagedService {

    private static final Logger logger = 
        LoggerFactory.getLogger(WebsocketClientBinding.class);

    /** RegEx to extract a parse a function String <code>'(.*?)\((.*)\)'</code> */
    private static final Pattern EXTRACT_FUNCTION_PATTERN = Pattern.compile("(.*?)\\((.*)\\)");

	// flag to use the reply of the remote end to update the status of the Item receving the data
	private static boolean updateWithResponse = true;

    /** 
     * the refresh interval which is used to check if the inbound connection is open
     * (optional, defaults to 60000ms)
     */
    private long refreshInterval = 60000;

    private WebsocketClientManager clientManager;
        
    
    public WebsocketClientBinding() {
        this.clientManager = new WebsocketClientManager(this);
    }
                
    public void activate() {
        logger.debug("activate");
        super.activate();
        clientManager.init();
        setProperlyConfigured(true);
    }
    
    public void deactivate() {
        // deallocate resources here that are no longer needed and 
        // should be reset when activating this binding again
        logger.debug("deactivate");
        clientManager.release();
    }

    @Override
    public void bindingChanged(BindingProvider provider, String itemName) {
        super.bindingChanged(provider, itemName);
        WebsocketClientBindingProvider bindingProvider = (WebsocketClientBindingProvider)provider;
        if (bindingProvider.getItemType(itemName) != null) {
            // added
            initializeItem(itemName, bindingProvider);
        } else {
            // removed
            releaseItem(itemName);
        }
    }


    /**
     * @{inheritDoc}
     */
    @Override
    protected long getRefreshInterval() {
        return refreshInterval;
    }

    /**
     * @{inheritDoc}
     */
    @Override
    protected String getName() {
        return "WebsocketClient Refresh Service";
    }
    
    /**
     * @{inheritDoc}
     */
    @Override
    protected void execute() {
        // currently, we use this execute method to keep the inbound connections alive.
        logger.debug("execute() method is called!");
        for (WebsocketClientBindingProvider provider : providers) {
            for (String itemName : provider.getItemNames(IN_COMMAND_KEY)) {
                String url = provider.getUrl(itemName);
                logger.debug("Check websocket connection for item={} for url={}", itemName, url);
                WebsocketClient wc = clientManager.getWebsocketClient(url);
                if (wc != null) {
                    try {
                        wc.ensureConnected();
                    } catch (Exception e) {
                        logger.error("Failed to keep the connection open", e);
                    }
                } else {
                    logger.warn("Unable to find the websocket client for {}", url);
                }
            }
        }
    }

    /**
     * @{inheritDoc}
     */
    @Override
    protected void internalReceiveCommand(String itemName, Command command) {
        // the code being executed when a command was sent on the openHAB
        // event bus goes here. This method is only called if one of the 
        // BindingProviders provide a binding for the given 'itemName'.
        logger.debug("internalReceiveCommand() is called!");
        writeToWebsocket(itemName, command, command);
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
        writeToWebsocket(itemName, CHANGED_COMMAND_KEY, newState);
    }

    private void writeToWebsocket(String itemName, Command command, Type type) {
        WebsocketClientBindingProvider provider = findFirstMatchingBindingProvider(itemName, command);

        if (logger.isDebugEnabled()) {
            logger.debug("writeToWebsocket: item={}, command={}, type={}", itemName, command, type);
        }
        
        if (provider == null) {
            logger.trace("doesn't find matching binding provider [itemName={}, command={}]", itemName, command);
            return;
        }
        String url = provider.getUrl(itemName, command);
        String transformation = provider.getTransformation(itemName, command);
        String transformedMessage = transformMessage(transformation, command.toString());
        try {
            WebsocketClient wc = clientManager.getWebsocketClient(url);
            wc.ensureConnected();
            wc.sendTextMessage(transformedMessage);
        } catch (Exception e) {
            logger.error("Unabled to send message to " + url);
        }
    }
    
    //TODO this is for testing for now
    void postToBus(String itemName, String message, WebsocketClientBindingProvider provider) {
        String transformedMessage = transformMessage(provider.getTransformation(itemName), message);
        
        Class<? extends Item> itemType = provider.getItemType(itemName);
        State state = createState(itemType, transformedMessage);
                
        if (state != null) {
            eventPublisher.postUpdate(itemName, state);
        }
    }

    private State createState(Class<? extends Item> itemType, String message) {
        try {
            if (itemType.isAssignableFrom(NumberItem.class)) {
                return DecimalType.valueOf(message);
            } else if (itemType.isAssignableFrom(ContactItem.class)) {
                return OpenClosedType.valueOf(message);
            } else if (itemType.isAssignableFrom(SwitchItem.class)) {
                return OnOffType.valueOf(message);
            } else if (itemType.isAssignableFrom(RollershutterItem.class)) {
                return PercentType.valueOf(message);
            } else if (itemType.isAssignableFrom(DateTimeItem.class)) {
                return DateTimeType.valueOf(message);
            } else {
                return StringType.valueOf(message);
            }
        } catch (Exception e) {
            logger.debug("Couldn't create state of type '{}' for value '{}'", itemType, message);
            return StringType.valueOf(message);
        }
    }


    private WebsocketClientBindingProvider findFirstMatchingBindingProvider(String itemName, Command command) {
        WebsocketClientBindingProvider firstMatchingProvider = null;
        
        for (WebsocketClientBindingProvider provider : providers) {
            String url = provider.getUrl(itemName, command);
            if (url != null) {
                firstMatchingProvider = provider;
                break;
            }
        }
        
        return firstMatchingProvider;
    }
    
    /**
     * @{inheritDoc}
     */
    @Override
    public void updated(Dictionary<String, ?> config) throws ConfigurationException {
        logger.debug("Configuration updated with provided {}", config != null);
        if (config != null) {
            
            // to override the default refresh interval one has to add a 
            // parameter to openhab.cfg like <bindingName>:refresh=<intervalInMs>
            String refreshIntervalString = (String) config.get("refresh");
            if (StringUtils.isNotBlank(refreshIntervalString)) {
                refreshInterval = Long.parseLong(refreshIntervalString);
            }
            
            // read further config parameters here ...
            setProperlyConfigured(true);
        }
        for (WebsocketClientBindingProvider provider : this.providers) {
            for (String itemName : provider.getItemNames()) {
                initializeItem(itemName, provider);
            }
        }
    }

    private void initializeItem(String itemName, WebsocketClientBindingProvider provider) {
        logger.debug("initialize item={}", itemName);
        for (Command command : provider.getCommands(itemName)) {
            String url = provider.getUrl(itemName, command);
            clientManager.add(itemName, url, IN_COMMAND_KEY.equals(command) ? provider : null);
            //REVISIT may connect the websocket for the inbound here instead of doing it in the execute method as of now
        }
    }
        
    private void releaseItem(String itemName) {
        logger.debug("release item={}", itemName);
        clientManager.remove(itemName);
    }
        
    protected String transformMessage(String transformation, String message) {
        logger.debug("transforming message {} using transform {}", message, transformation);
        String transformedMessage;
        try {
            String[] parts = splitTransformationConfig(transformation);
            String transformationType = parts[0];
            String transformationFunction = parts[1];
            
            TransformationService transformationService = 
                TransformationHelper.getTransformationService(WebsocketClientActivator.getContext(), transformationType);
            if (transformationService != null) {
                transformedMessage = transformationService.transform(transformationFunction, message);
            } else {
                transformedMessage = message;
                logger.warn("couldn't transform message because transformationService of type '{}' is unavailable", transformationType);
            }
        }
        catch (Exception te) {
            logger.error("transformation throws exception [transformation="
                         + transformation + ", message=" + message + "]", te);

            // in case of an error we return the message without any transformation
            transformedMessage = message;
        }

        logger.debug("transformed message is '{}'", transformedMessage);
        return transformedMessage;
    }

    protected String[] splitTransformationConfig(String transformation) {
        Matcher matcher = EXTRACT_FUNCTION_PATTERN.matcher(transformation);

        if (!matcher.matches()) {
            throw new IllegalArgumentException("given transformation function '" + transformation + "' does not follow the expected pattern '<function>(<pattern>)'");
        }
        matcher.reset();

        matcher.find();                 
        String type = matcher.group(1);
        String pattern = matcher.group(2);
        
        return new String[] { type, pattern };
    }
}
