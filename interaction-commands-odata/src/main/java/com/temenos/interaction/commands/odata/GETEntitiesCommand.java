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
import org.odata4j.edm.EdmEntitySet;
import org.odata4j.exceptions.ODataProducerException;
import org.odata4j.producer.EntitiesResponse;
import org.odata4j.producer.ODataProducer;
import org.odata4j.producer.QueryInfo;
import org.odata4j.producer.resources.OptionsQueryParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.temenos.interaction.core.command.InteractionCommand;
import com.temenos.interaction.core.command.InteractionContext;
import com.temenos.interaction.core.command.InteractionException;
import com.temenos.interaction.core.resource.CollectionResource;

public class GETEntitiesCommand extends AbstractODataCommand implements InteractionCommand {
	private final Logger logger = LoggerFactory.getLogger(GETEntitiesCommand.class);

	public GETEntitiesCommand(ODataProducer producer) {
		super(producer);
	}

	/* Implement InteractionCommand interface */

	@Override
	public Result execute(InteractionContext ctx) throws InteractionException {
		assert(ctx != null);
		assert(ctx.getCurrentState() != null);
		assert(ctx.getCurrentState().getEntityName() != null && !ctx.getCurrentState().getEntityName().equals(""));
		assert(ctx.getResource() == null);

		String entityName = getEntityName(ctx);
		logger.debug("Getting entities for " + entityName);
		try {
			EdmEntitySet entitySet = CommandHelper.getEntitySet(entityName, getEdmMetadata());
			String entitySetName = entitySet.getName();

			EntitiesResponse response = producer.getEntities(entitySetName, getQueryInfo(ctx));
			    
			CollectionResource<OEntity> cr = CommandHelper.createCollectionResource(entitySetName, response.getEntities());
			ctx.setResource(cr);
		}
		catch(ODataProducerException ope) {
			logger.debug("GET entities on [" + entityName + ", " + ctx.getId() + "] failed: " + ope.getMessage());
			throw new InteractionException(ope.getHttpStatus(), ope.getMessage());
		} catch (InteractionException e) {
			throw e;
		}
		catch(Exception e) {
			logger.error("Failed to GET entities [" + entityName + ", " + ctx.getId() + "]: " + e.getMessage());
			throw new InteractionException(Status.INTERNAL_SERVER_ERROR, e.getMessage());
		}
		return Result.SUCCESS;
	}

	/*
	 * Obtain the odata query information from the context's query parameters
	 * @param ctx interaction context
	 * @return query details
	 */
	private QueryInfo getQueryInfo(InteractionContext ctx) throws InteractionException {
		MultivaluedMap<String, String> queryParams = ctx.getQueryParameters();
		String inlineCount = queryParams.getFirst("$inlinecount");
		String top = queryParams.getFirst("$top");
		String skip = queryParams.getFirst("$skip");
		String actionFilter = CommandHelper.getViewActionProperty(ctx, "filter");		//Filter defined as action property 
		String filter = CommandHelper.getURLDecodedString(queryParams.getFirst("$filter"));	//Query filter
		if (filter == null 
				&& actionFilter != null && !actionFilter.isEmpty()
				&& !actionFilter.contains("{") && !actionFilter.contains("}")) {
			filter = actionFilter;
		}
		String orderBy = CommandHelper.getURLDecodedString(queryParams.getFirst("$orderby"));
		String skipToken = queryParams.getFirst("$skiptoken");
		String expand = queryParams.getFirst("$expand");
		String select = queryParams.getFirst("$select");

		try {
			return new QueryInfo(
					OptionsQueryParser.parseInlineCount(inlineCount),
					OptionsQueryParser.parseTop(top),
					OptionsQueryParser.parseSkip(skip),
					OptionsQueryParser.parseFilter(filter),
					OptionsQueryParser.parseOrderBy(orderBy),
					OptionsQueryParser.parseSkipToken(skipToken),
					null,
					OptionsQueryParser.parseExpand(expand),
					OptionsQueryParser.parseSelect(select));
		} catch (RuntimeException e) {
			// all runtime exceptions are due to failure in parsing the query options
			throw new InteractionException(Status.BAD_REQUEST,"Invalid query option in '" + queryParams + "'. Error: " + e.getMessage());
		}
	}
}
