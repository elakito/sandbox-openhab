/**
 * Copyright (c) 2010-2014, openHAB.org and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.camel.internal;

import org.openhab.binding.camel.CamelDispatcherBindingProvider;
import org.openhab.core.items.Item;
import org.openhab.model.item.binding.BindingConfigParseException;


/**
 * This class is responsible for parsing the binding configuration.
 * 
 * in:  cameldispatcher:"<[<name>:<transformationrule>]"
 * out: cameldispatcher:">[<command>:<name>:<transformationrule>]"
 * 
 * cameldispatcher="<[//dispatcher-name:'some text']" - for String Items
 * cameldispatcher=">[ON://dispatcher-name:'some text'], >[OFF://dispatcher-name:'some other command']"
 * 
 * @author elakito
 * @since 1.6.0
 */
public class CamelDispatcherGenericBindingProvider extends AbstractCamelConnectorGenericBindingProvider implements CamelDispatcherBindingProvider {

    /**
     * {@inheritDoc}
     */
    public String getBindingType() {
        return "cameldispatcher";
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
        
}
