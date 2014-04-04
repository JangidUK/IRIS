package com.temenos.interaction.commands.odata;

/*
 * #%L
 * interaction-commands-odata
 * %%
 * Copyright (C) 2012 - 2013 Temenos Holdings N.V.
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * #L%
 */


import org.odata4j.edm.EdmDataServices;
import org.odata4j.producer.ODataProducer;

import com.temenos.interaction.core.command.InteractionContext;
import com.temenos.interaction.core.hypermedia.Action;

public abstract class AbstractODataCommand {
	/**
	 * Use this property to configure an action to use this entity 
	 * instead of the entity specified for the Resource.
	 */
	public final static String ENTITY_PROPERTY = "entity";
	protected ODataProducer producer; 
	
	public AbstractODataCommand(ODataProducer producer) {
		this.producer = producer;
	}

	
	public String getEntityName(InteractionContext ctx) {
		String entityName = ctx.getCurrentState().getEntityName();
		// TODO improve this naive implmentation, only using properties from first action
		Action action = null;
		if (ctx.getCurrentState().getActions().size() > 0)
			action = ctx.getCurrentState().getActions().get(0);
		
		if (action != null && action.getProperties() != null && action.getProperties().getProperty(ENTITY_PROPERTY) != null) {
			entityName = action.getProperties().getProperty(ENTITY_PROPERTY);
		}
		return entityName;
	}
	
	protected EdmDataServices getEdmMetadata() {
		return producer.getMetadata();
	}
	
}
