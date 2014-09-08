/**
 * Copyright (c) 2010-2014, openHAB.org and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.websocketclient;

import java.util.List;

import org.openhab.core.binding.BindingProvider;
import org.openhab.core.items.Item;
import org.openhab.core.types.Command;

/**
 * @author elakito
 * @since 1.6.0
 */
public interface WebsocketClientBindingProvider extends BindingProvider {
    String getUrl(String itemName, Command command);
    String getUrl(String itemName);
    String getTransformation(String itemName, Command command);
    String getTransformation(String itemName);
    List<String> getItemNames(Command command);
    Class<? extends Item> getItemType(String itemName);
    List<Command> getCommands(String itemName);
}
