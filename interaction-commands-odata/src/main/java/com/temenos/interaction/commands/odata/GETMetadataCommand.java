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

import com.temenos.interaction.core.command.InteractionCommand;
import com.temenos.interaction.core.command.InteractionContext;
import com.temenos.interaction.core.resource.EntityResource;
import com.temenos.interaction.core.resource.MetaDataResource;
import com.temenos.interaction.odataext.entity.MetadataOData4j;

/**
 * GET command for obtaining meta data defining either the
 * resource model or the service document. 
 */
public class GETMetadataCommand implements InteractionCommand {

	// command configuration
	// TODO remove this when we no longer use a MetaDataResource
	private String resourceToProvide;
	
//	@Deprecated
//	private EdmDataServices edmDataServices;
	private MetadataOData4j metadataOData4j;

	/**
	 * Construct an instance of this command
	 * @param resourceToProvide Configure this command to provide either an EntityResource for the
	 * service document or a MetaDataResource for the metadata.
	 * @param resourceMetadata Description of the resources and their types.
	 */
//	@Deprecated
//	public GETMetadataCommand(String resourceToProvide, EdmDataServices resourceMetadata) {
//		this.resourceToProvide = resourceToProvide;
//		this.edmDataServices = resourceMetadata;
//	}
	
	/**
	 * Construct an instance of this command
	 * @param resourceToProvide Configure this command to provide either an EntityResource for the
	 * @param metadataOData4j contain resource metadata.
	 */
	public GETMetadataCommand(String resourceToProvide, MetadataOData4j metadataOData4j) {
		this.resourceToProvide = resourceToProvide;
		this.metadataOData4j = metadataOData4j;
	}	
	/* Implement InteractionCommand interface */
	
	@Override
	public Result execute(InteractionContext ctx) {
		assert(ctx != null);
		if(resourceToProvide.equals("ServiceDocument")) {
			EntityResource<EdmDataServices> sdr = 
					CommandHelper.createServiceDocumentResource(metadataOData4j.getMetadata());
			ctx.setResource(sdr);
		} else {
			MetaDataResource<EdmDataServices> mdr = 
					CommandHelper.createMetaDataResource(metadataOData4j.getMetadata());
			ctx.setResource(mdr);
		}
		return Result.SUCCESS;
	}

}
