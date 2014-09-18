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
import org.openhab.core.items.Item;
import org.openhab.model.item.binding.BindingConfigParseException;


/**
 * This class is responsible for parsing the binding configuration.
 * 
 * in:  camelprovider:"<[<name>:<transformationrule>]"
 * 
 * camelprovider="<[//provider-name:'some text']" - for String Items
 * 
 * @author elakito
 * @since 1.6.0
 */
public class CamelProviderGenericBindingProvider extends AbstractCamelConnectorGenericBindingProvider implements CamelProviderBindingProvider {

	/**
	 * {@inheritDoc}
	 */
	public String getBindingType() {
		return "camelprovider";
	}

	/**
	 * @{inheritDoc}
	 */
	@Override
	public void validateItemType(Item item, String bindingConfig) throws BindingConfigParseException {
		//if (!(item instanceof SwitchItem || item instanceof DimmerItem)) {
		//	throw new BindingConfigParseException("item '" + item.getName()
		//			+ "' is of type '" + item.getClass().getSimpleName()
		//			+ "', only Switch- and DimmerItems are allowed - please check your *.items configuration");
		//}
	}
}
