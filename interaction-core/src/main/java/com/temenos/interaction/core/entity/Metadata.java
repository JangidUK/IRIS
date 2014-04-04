package com.temenos.interaction.core.entity;

/*
 * #%L
 * interaction-core
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


import java.util.HashMap;
import java.util.Map;

import com.temenos.interaction.core.resource.ResourceMetadataManager;

/**
 * This class holds metadata information about resource entities.
 */
public class Metadata  {
	public final static String MODEL_SUFFIX = "Model";

	//Map of <Entity name, Entity metadata>
	private Map<String, EntityMetadata> entitiesMetadata = new HashMap<String, EntityMetadata>();
	private String modelName;

	private ResourceMetadataManager resourceMetadataManager;
	
	/**
	 * Construct a new metadata object
	 * @param modelName name of this model
	 * Some unit test are using this
	 */
	public Metadata(String modelName) {
		this.modelName = modelName;
	}

	/*
	 * construct metadata with ResourceMetadataManager
	 */
	public Metadata(ResourceMetadataManager resourceMetadataManager) {
		setResourceMetadataManager(resourceMetadataManager);
	}
	
	/**
	 * Returns the metadata of the specified entity
	 * @param entityName Entity name
	 * @return entity metadata
	 */
	public EntityMetadata getEntityMetadata(String entityName) {		
		if( !entitiesMetadata.containsKey(entityName)) {
			if(resourceMetadataManager == null) {
				resourceMetadataManager = new ResourceMetadataManager();
			}
			Metadata metadata = resourceMetadataManager.getMetadata(entityName);
			entitiesMetadata.putAll(metadata.getEntitiesMetadata());
			setModelName(metadata.getModelName());
		} 
		return entitiesMetadata.get(entityName);
	}
	
	/**
	 * Sets the metadata for the specified entity
	 * @param entityName Entity name
	 * @param entityMetadata Entity metadata
	 */
	public void setEntityMetadata(EntityMetadata entityMetadata) {
		entitiesMetadata.put(entityMetadata.getEntityName(), entityMetadata);
	}
	
	/**
	 * Returns a map of <entity name, entity metadata> 
	 * @return entities metadata map
	 */
	public Map<String, EntityMetadata> getEntitiesMetadata() {
		return entitiesMetadata;
	}

	/**
	 * Returns the name of the model
	 * @return model name
	 */
	public String getModelName() {
		return modelName;
	}
	
	public void setModelName(String modelName) {
		this.modelName = modelName;
	}
	
	/*
	 * setter method 
	 */
	public void setResourceMetadataManager(ResourceMetadataManager rmManager) {
		this.resourceMetadataManager = rmManager;
	}
	

	/*
	 * return metadata
	 */
	public Metadata getMetadata() {
		return this;
	}	
}
