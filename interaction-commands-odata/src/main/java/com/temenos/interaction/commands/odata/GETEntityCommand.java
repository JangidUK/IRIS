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


import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response.Status;

import org.odata4j.core.OEntity;
import org.odata4j.core.OEntityKey;
import org.odata4j.edm.EdmDataServices;
import org.odata4j.edm.EdmEntitySet;
import org.odata4j.exceptions.ODataProducerException;
import org.odata4j.producer.EntityQueryInfo;
import org.odata4j.producer.EntityResponse;
import org.odata4j.producer.ODataProducer;
import org.odata4j.producer.resources.OptionsQueryParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.temenos.interaction.core.command.InteractionCommand;
import com.temenos.interaction.core.command.InteractionContext;
import com.temenos.interaction.core.command.InteractionException;
import com.temenos.interaction.core.resource.EntityResource;

public class GETEntityCommand extends AbstractODataCommand implements InteractionCommand {
	private final static Logger logger = LoggerFactory.getLogger(GETEntityCommand.class);

	private ODataProducer producer;
	private EdmDataServices edmDataServices;

	public GETEntityCommand(ODataProducer producer) {
		this.producer = producer;
		this.edmDataServices = producer.getMetadata();
	}
	
	protected ODataProducer getProducer() {
		return producer;
	}

	/* Implement InteractionCommand interface */
	
	@Override
	public Result execute(InteractionContext ctx) throws InteractionException {
		assert(ctx != null);
		assert(ctx.getCurrentState() != null);
		assert(ctx.getCurrentState().getEntityName() != null && !ctx.getCurrentState().getEntityName().equals(""));
		
		String entityName = getEntityName(ctx);
		logger.debug("Getting entity for " + entityName);
		try {
			EdmEntitySet entitySet = CommandHelper.getEntitySet(entityName, edmDataServices);
			String entitySetName = entitySet.getName();

			//Create entity key (simple types only)
			OEntityKey key = CommandHelper.createEntityKey(edmDataServices, entitySetName, ctx.getId());
			
			//Get the entity
			EntityResponse er = getProducer().getEntity(entitySetName, key, getEntityQueryInfo(ctx));
			OEntity entity = er.getEntity();
			EntityResource<OEntity> oer = CommandHelper.createEntityResource(entity);
			oer.setEntityTag(entity.getEntityTag());		//Set the E-Tag
			ctx.setResource(oer);		
		}
		catch(ODataProducerException ope) {
			logger.debug("GET entity on [" + entityName + ", " + ctx.getId() + "] failed: " + ope.getMessage());
			throw new InteractionException(ope.getHttpStatus(), ope.getMessage());
		}
		catch(Exception e) {
			logger.error("Failed to GET entity [" + entityName + ", " + ctx.getId() + "]: " + e.getMessage());
			throw new InteractionException(Status.INTERNAL_SERVER_ERROR, e.getMessage());
		}
		return Result.SUCCESS;
	}

	/*
	 * Obtain the odata query information from the context's query parameters
	 * @param ctx interaction context
	 * @return query details
	 */
	private EntityQueryInfo getEntityQueryInfo(InteractionContext ctx) {
		MultivaluedMap<String, String> queryParams = ctx.getQueryParameters();
		String actionFilter = CommandHelper.getViewActionProperty(ctx, "filter");		//Filter defined as action property 
		String filter = CommandHelper.getURLDecodedString(queryParams.getFirst("$filter"));	//Query filter
		if(actionFilter != null && !actionFilter.isEmpty()) {
			filter = filter != null ? filter + " and " + actionFilter : actionFilter;
		}
		String expand = queryParams.getFirst("$expand");
		String select = queryParams.getFirst("$select");
	      
		return new EntityQueryInfo(
				OptionsQueryParser.parseFilter(filter),
				null,
				OptionsQueryParser.parseExpand(expand),
				OptionsQueryParser.parseSelect(select));		
	}
}
