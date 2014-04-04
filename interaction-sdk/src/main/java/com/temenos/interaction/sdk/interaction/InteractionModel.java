package com.temenos.interaction.sdk.interaction;

/*
 * #%L
 * interaction-sdk
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


import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.HttpMethod;

import org.odata4j.edm.EdmDataServices;
import org.odata4j.edm.EdmEntitySet;
import org.odata4j.edm.EdmEntityType;
import org.odata4j.edm.EdmSimpleType;
import org.odata4j.edm.EdmType;

import com.temenos.interaction.core.entity.EntityMetadata;
import com.temenos.interaction.core.entity.Metadata;
import com.temenos.interaction.core.entity.vocabulary.terms.TermValueType;
import com.temenos.interaction.sdk.interaction.state.IMState;

/**
 * This class holds information about the interaction model
 */
public class InteractionModel {

	private String domain;
	private String name;
	private IMState exceptionState = null;
	private Map<String, IMState> errorHandlerStates = new HashMap<String, IMState>();		//state name, state
	private Map<String, IMResourceStateMachine> entityRsmMap = new HashMap<String, IMResourceStateMachine>();

	/**
	 * Construct an empty model
	 */
	public InteractionModel() {
	}	
	
	/**
	 * Construct an initial model from odata4j metadata
	 * @param edmDataServices odata4j metadata 
	 */
	public InteractionModel(EdmDataServices edmDataServices) {
		name = edmDataServices.getSchemas().get(0).getEntityContainers().get(0).getName();
		for (EdmEntitySet entitySet : edmDataServices.getEntitySets()) {
			addResourceStateMachine(createInitialResourceStateMachine(entitySet));
		}
	}
	
	/**
	 * Construct an initial model from entity metadata
	 * @param metadata metadata 
	 */
	public InteractionModel(Metadata metadata) {
		name = metadata.getModelName();
		for (EntityMetadata entityMetadata : metadata.getEntitiesMetadata().values()) {
			addResourceStateMachine(createInitialResourceStateMachine(entityMetadata));
		}
	}

	/**
	 * Create an initial RSM with a collection and entity state
	 * @param entitySet Entity metadata
	 * @return resource state machine
	 */
	public IMResourceStateMachine createInitialResourceStateMachine(EdmEntitySet entitySet) {
		EdmEntityType entityType = entitySet.getType();
		String entityName = entityType.getName();
		String collectionStateName = entitySet.getName();
		String entityStateName = entityName.toLowerCase();
		String mappedEntityProperty = entityType.getKeys().size() > 0 ? entityType.getKeys().get(0) : "id";
		String pathParametersTemplate = getUriTemplateParameters(entityType);
		return new IMResourceStateMachine(entityName, collectionStateName, entityStateName, mappedEntityProperty, pathParametersTemplate);
	}

	/**
	 * Create an initial RSM with a collection and entity state
	 * @param entityMetadata Entity metadata
	 * @return resource state machine
	 */
	public IMResourceStateMachine createInitialResourceStateMachine(EntityMetadata entityMetadata) {
		return createInitialResourceStateMachine(entityMetadata, HttpMethod.GET);
	}

	/**
	 * Create an initial RSM with a collection and entity state
	 * @param entityMetadata Entity metadata
	 * @param methodGetEntity Method for GET entity 
	 * @return resource state machine
	 */
	public IMResourceStateMachine createInitialResourceStateMachine(EntityMetadata entityMetadata, String methodGetEntity) {
		return createInitialResourceStateMachine(entityMetadata, methodGetEntity, null);
	}
	
	/**
	 * Create an initial RSM with a collection and entity state
	 * @param entityMetadata Entity metadata
	 * @param methodGetEntity Method for GET entity 
	 * @return resource state machine
	 */
	public IMResourceStateMachine createInitialResourceStateMachine(EntityMetadata entityMetadata, String methodGetEntity, String collectionRels) {
		String entityName = entityMetadata.getEntityName();
		String collectionStateName = entityName + "s";
		String entityStateName = entityName.toLowerCase();
		List<String> idFields = entityMetadata.getIdFields();
		String mappedEntityProperty = idFields.size() > 0 ? idFields.get(0) : "id";
		String pathParametersTemplate = getUriTemplateParameters(entityMetadata);
		return new IMResourceStateMachine(entityName, collectionStateName, entityStateName, methodGetEntity, mappedEntityProperty, pathParametersTemplate, collectionRels);
	}
	
	public String getUriTemplateParameters(EntityMetadata entityMetadata) {
		String paramTemplate = "";
		List<String> keys = entityMetadata.getIdFields();
		for(String key : keys) {
			if(!paramTemplate.equals("")) {
				paramTemplate += ",";				
			}
			String keyPropertyType = entityMetadata.getTermValue(key, TermValueType.TERM_NAME);
			if(keyPropertyType != null && isQuotedUriTemplateParameter(keyPropertyType)) {
				paramTemplate += "'{" + (keys.size() > 1 ? key : "id") + "}'";		//These types should be enclosed in single quotes				
			}
			else {
				paramTemplate += "{" + (keys.size() > 1 ? key : "id") + "}";				
			}
		}
		return paramTemplate;		
	}

	private boolean isQuotedUriTemplateParameter(String entityType) {
		return entityType.equals(TermValueType.IMAGE) ||
				entityType.equals(TermValueType.TIMESTAMP) ||
				entityType.equals(TermValueType.DATE) ||
				entityType.equals(TermValueType.TEXT) ||
				entityType.equals(TermValueType.ENCRYPTED_TEXT);
	}

	public String getUriTemplateParameters(EdmEntityType entityType) {
		String paramTemplate = "";
		List<String> keys = entityType.getKeys();
		for(String key : keys) {
			if(!paramTemplate.equals("")) {
				paramTemplate += ",";				
			}
			EdmType keyPropertyType = entityType.findDeclaredProperty(key).getType();
			if(keyPropertyType != null && isQuotedUriTemplateParameter(keyPropertyType)) {
				paramTemplate += "'{" + (keys.size() > 1 ? key : "id") + "}'";		//These types should be enclosed in single quotes				
			}
			else {
				paramTemplate += "{" + (keys.size() > 1 ? key : "id") + "}";				
			}
		}
		return paramTemplate;		
	}
	
	private boolean isQuotedUriTemplateParameter(EdmType type) {
		return type.equals(EdmSimpleType.DATETIME) || type.equals(EdmSimpleType.STRING);
	}
	
	public void addResourceStateMachine(IMResourceStateMachine resourceStateMachine) {
		entityRsmMap.put(resourceStateMachine.getEntityName(), resourceStateMachine);
	}
	
	public List<IMResourceStateMachine> getResourceStateMachines() {
		return new ArrayList<IMResourceStateMachine>(entityRsmMap.values());
	}
	
	public IMResourceStateMachine findResourceStateMachine(String entityName) {
		return entityRsmMap.get(entityName);
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDomain() {
		return domain;
	}

	public void setDomain(String domain) {
		this.domain = domain;
	}
	
	public void setExceptionState(IMState exceptionState) {
		this.exceptionState = exceptionState;
	}
	
	public IMState getExceptionState() {
		return exceptionState;
	}
	
	public boolean hasExceptionState() {
		return exceptionState != null;
	}

	public Collection<IMState> getErrorHandlerStates() {
		return errorHandlerStates.values();
	}

	public void addErrorHandlerState(IMState errorHandlerState) {
		errorHandlerStates.put(errorHandlerState.getName(), errorHandlerState);
	}
	
	public IMState getErrorHandlerState(String stateName) {
		return errorHandlerStates.get(stateName);
	}
}
