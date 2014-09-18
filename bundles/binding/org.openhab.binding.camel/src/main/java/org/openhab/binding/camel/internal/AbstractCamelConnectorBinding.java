/**
 * Copyright (c) 2010-2014, openHAB.org and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.camel.internal;

import java.util.Dictionary;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.openhab.binding.camel.CamelBindingProvider;
import org.openhab.core.binding.AbstractBinding;
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
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author elakito
 * @since 1.6.0
 */
public abstract class AbstractCamelConnectorBinding<P extends CamelBindingProvider> extends AbstractBinding<P> implements ManagedService {
    private static final Logger logger = LoggerFactory.getLogger(CamelDispatcherBinding.class);

    /** RegEx to extract a parse a function String <code>'(.*?)\((.*)\)'</code> */
    private static final Pattern EXTRACT_FUNCTION_PATTERN = Pattern.compile("(.*?)\\((.*)\\)");

    protected AbstractCamelConnectorManager<P> connectorManager;
        
    public AbstractCamelConnectorBinding() {
        this.connectorManager = createConnectorManager(this);
    }

    public void activate() {
        logger.debug("activate");
        super.activate();
        connectorManager.init();
    }
    
    public void deactivate() {
        // deallocate resources here that are no longer needed and 
        // should be reset when activating this binding again
        logger.debug("deactivate");
        connectorManager.release();
    }


    @Override
    public void bindingChanged(BindingProvider provider, String itemName) {
        super.bindingChanged(provider, itemName);
        CamelBindingProvider bindingProvider = (CamelBindingProvider)provider;
        if (bindingProvider.getItemType(itemName) != null) {
            // added
            initializeItem(itemName, bindingProvider);
        } else {
            // removed
            releaseItem(itemName);
        }
    }

    //TODO this is for testing for now
    void postToBus(String itemName, String message, CamelBindingProvider provider) {
        String transformedMessage = transformMessage(provider.getTransformation(itemName), message);
        
        Class<? extends Item> itemType = provider.getItemType(itemName);
        State state = createState(itemType, transformedMessage);
                
        if (state != null) {
            eventPublisher.postUpdate(itemName, state);
        }
    }

    protected static State createState(Class<? extends Item> itemType, String message) {
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

    protected CamelBindingProvider findFirstMatchingBindingProvider(String itemName, Command command) {
        CamelBindingProvider firstMatchingProvider = null;
        
        for (CamelBindingProvider provider : providers) {
            String name = provider.getName(itemName, command);
            if (name != null) {
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
			// setup            
        }
        for (CamelBindingProvider provider : providers) {
            for (String itemName : provider.getItemNames()) {
                initializeItem(itemName, provider);
            }
        }
    }

    private void initializeItem(String itemName, CamelBindingProvider provider) {
        logger.debug("initialize item={}", itemName);
        for (Command command : provider.getCommands(itemName)) {
            String name = provider.getName(itemName, command);
            try {
            	connectorManager.add(itemName, name, provider);
            } catch (Exception e) {
            	logger.error("Failed to add dispatcher " + name, e);
            }
	    }
    }
        
    private void releaseItem(String itemName) {
        logger.debug("release item={}", itemName);
        connectorManager.remove(itemName);
    }
        
    protected static String transformMessage(String transformation, String message) {
        logger.debug("transforming message {} using transform {}", message, transformation);
        String transformedMessage;
        try {
            String[] parts = splitTransformationConfig(transformation);
            String transformationType = parts[0];
            String transformationFunction = parts[1];
            
            TransformationService transformationService = 
                TransformationHelper.getTransformationService(CamelActivator.getContext(), transformationType);
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

    protected static String[] splitTransformationConfig(String transformation) {
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
    
    protected abstract AbstractCamelConnectorManager<P> createConnectorManager(AbstractCamelConnectorBinding<P> binding);
    	
}
