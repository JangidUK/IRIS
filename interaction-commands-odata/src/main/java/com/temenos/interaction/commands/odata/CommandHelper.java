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


import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.MultivaluedMap;

import org.odata4j.core.OEntity;
import org.odata4j.core.OEntityKey;
import org.odata4j.edm.EdmDataServices;
import org.odata4j.edm.EdmEntitySet;
import org.odata4j.edm.EdmEntityType;
import org.odata4j.edm.EdmProperty;
import org.odata4j.edm.EdmSchema;
import org.odata4j.edm.EdmType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.temenos.interaction.core.command.InteractionContext;
import com.temenos.interaction.core.entity.Entity;
import com.temenos.interaction.core.hypermedia.ActionPropertyReference;
import com.temenos.interaction.core.resource.CollectionResource;
import com.temenos.interaction.core.resource.EntityResource;
import com.temenos.interaction.core.resource.MetaDataResource;
import com.temenos.interaction.core.resource.ResourceTypeHelper;

public class CommandHelper {
	private final static Logger logger = LoggerFactory.getLogger(CommandHelper.class);
	private static Pattern parameterPattern = Pattern.compile("\\{(.*?)\\}");

	/**
	 * To handle generic case where user do not know the type, here we will be looking for a type
	 * and then generate EntityResource<E>
	 * @param entity
	 * @return EntityResource<E>
	 */
	public static<E> EntityResource<E> createEntityResource(E entity) {
		GenericEntity<E> ge = new GenericEntity<E>(entity) {};
		Type t = com.temenos.interaction.core.command.CommandHelper.getEffectiveGenericType(ge.getType(), entity);
		if(ResourceTypeHelper.isType(ge.getRawType(), t, OEntity.class, OEntity.class)) {
			OEntity te = (OEntity) entity;
			String entityName = te != null && te.getEntityType() != null ? te.getEntityType().getName() : null;
			return com.temenos.interaction.core.command.CommandHelper.createEntityResource(entityName, entity, OEntity.class);
		} else if(ResourceTypeHelper.isType(ge.getRawType(), t, Entity.class, Entity.class)) {
			Entity te = (Entity) entity;
			String entityName = te != null ? te.getName() : null; 
			return com.temenos.interaction.core.command.CommandHelper.createEntityResource(entityName, entity, Entity.class);
		} else {
			// Call the generic and lets see what happens
			return com.temenos.interaction.core.command.CommandHelper.createEntityResource(entity, null);
		}
	}
	
	
	/**
	 * To help converting Entity object into EntityResource as we are no longer extending the core CommandHelper
	 * @param entity
	 * @return EntityResource<Entity> EntityResource of parameter type Entity
	 */
	public static EntityResource<Entity> createEntityResource(Entity entity) {
		return com.temenos.interaction.core.command.CommandHelper.createEntityResource(entity);
	}
	
	/**
	 * Create an OData entity resource (entry)
	 * @param e OEntity
	 * @return entity resource EntityResource of parameter type OEntity
	 */
	public static EntityResource<OEntity> createEntityResource(OEntity e) {
		String entityName = e != null && e.getEntityType() != null ? e.getEntityType().getName() : null;
		return com.temenos.interaction.core.command.CommandHelper.createEntityResource(entityName, e, OEntity.class);
	}

	/**
	 * Create an OData collection resource (feed)
	 * @param entitySetName Entity set name
	 * @param entities List of OData entities
	 * @return collection resource
	 */
	public static CollectionResource<OEntity> createCollectionResource(String entitySetName, List<OEntity> entities) {
		List<EntityResource<OEntity>> subResources = new ArrayList<EntityResource<OEntity>>();
		for (OEntity entity : entities)
			subResources.add(createEntityResource(entity));
		return new CollectionResource<OEntity>(entitySetName, subResources) {};
	}

	/**
	 * Create an OData service document (atomsvc)
	 * @param metadata Edmx
	 * @return Service document
	 */
	public static EntityResource<EdmDataServices> createServiceDocumentResource(EdmDataServices metadata) {
		return new EntityResource<EdmDataServices>(metadata) {};	
	}

	/**
	 * Create an OData metadata document (edmx)
	 * @param metadata Edmx
	 * @return metadata resource
	 */
	public static MetaDataResource<EdmDataServices> createMetaDataResource(EdmDataServices metadata) {
		return new MetaDataResource<EdmDataServices>(metadata) {};	
	}
	
	/**
	 * Create an OEntityKey instance for the specified entity id
	 * @param edmDataServices edmDataServices
	 * @param entity Entity set name
	 * @param id Id
	 * @return An OEntityKey instance
	 * @throws Exception Error creating key 
	 */
	public static OEntityKey createEntityKey(EdmDataServices edmDataServices, String entitySetName, String id) throws Exception {
		//Lookup type of entity key (simple keys only)
		String keyType = null;
		EdmEntitySet entitySet = edmDataServices.getEdmEntitySet(entitySetName);
		if(entitySet != null) {
			EdmEntityType entityType = entitySet.getType();
			List<String> keys = entityType.getKeys();
			if(keys.size() == 1) {
				EdmProperty prop = entityType.findDeclaredProperty(keys.get(0));
				if(prop != null && prop.getType() != null) {
					keyType = prop.getType().getFullyQualifiedTypeName();
				}
			}
		}
		assert(keyType != null) : "Should not be possible to get this far and find no key type";
		
		//Create an entity key
		OEntityKey key = null;
		try {
			if (keyType.equals("Edm.Int64")) {
				key = OEntityKey.parse(id);
			} else if(keyType.equals("Edm.Int32")) {
				key = OEntityKey.parse(id);
			} else if(keyType.equals("Edm.DateTime")) {
				key = OEntityKey.parse(id);
			} else if(keyType.equals("Edm.Time")) {
				key = OEntityKey.parse(id);
			} else if(keyType.equals("Edm.String")) {
				key = OEntityKey.parse(id);
			}
		} catch (Exception e) {
			logger.warn("Entity key type " + keyType + " is not supported by CommandHelper, trying OEntityKey.parse");
		}
		// could not parse the key, have one last attempt with OEntityKey create
		if (key == null) {
			try {
				if (keyType.equals("Edm.Int64")) {
					key = OEntityKey.create(Long.parseLong(id));
				} else if(keyType.equals("Edm.Int32")) {
					key = OEntityKey.create(Integer.parseInt(id));
				} else {
					key = OEntityKey.create(id);
				}					
			} catch (Exception e) {
				logger.error("OEntityKey.parse failed to parse id [" + id + "]");
			}
		}
		if (key == null)
			throw new Exception("Entity key type " + id + " is not supported.");
		return key;
	}
	
	/**
	 * Returns the entity set holding the specified entity (type) name
	 * @param entityName entity type name
	 * @param edmDataServices metadata
	 * @return entity set
	 * @throws Exception if entity set cannot be found
	 */
	public static EdmEntitySet getEntitySet(String entityName, EdmDataServices edmDataServices) throws Exception {
		//Find entity type
		EdmType entityType = null;
		Iterator<EdmSchema> itSchema = edmDataServices.getSchemas().iterator();
		while(entityType == null && itSchema.hasNext()) {
			entityType = edmDataServices.findEdmEntityType(itSchema.next().getNamespace() + "." + entityName);
		}
		if(entityType == null || !(entityType instanceof EdmEntityType)) {
			throw new Exception("Entity type does not exist");
		}
		
		//Find entity set
		EdmEntitySet entitySet = edmDataServices.getEdmEntitySet((EdmEntityType) entityType);
		if (entitySet == null) {
			throw new Exception("Entity set does not exist");
		}
		return entitySet;
	}
	
	/**
	 * Returns a view action property
	 * This method will try to replace template parameters "{param}"
	 * with query parameters if available 
	 * @param ctx interaction context
	 * @param property action property
	 * @return property value
	 */
	public static String getViewActionProperty(InteractionContext ctx, String property) {
		String prop = null;
		if(ctx.getCurrentState().getViewAction() != null) {
			Properties properties = ctx.getCurrentState().getViewAction().getProperties();
			if(properties != null && properties.containsKey(property)) {
				//Get the specified action property
				prop = getActionProperty(property, properties, ctx.getQueryParameters());
				
				//Fill in template parameters
				if(prop != null) {
					Matcher m = parameterPattern.matcher(prop);
					while(m.find()) {
						String templateParam = m.group(1);			//e.g. code
						if(ctx.getQueryParameters().containsKey(templateParam)) {
							prop = prop.replaceAll("\\{" + templateParam + "\\}", ctx.getQueryParameters().getFirst(templateParam));
						}
						else if(ctx.getPathParameters().containsKey(templateParam)) {
							prop = prop.replaceAll("\\{" + templateParam + "\\}", ctx.getPathParameters().getFirst(templateParam));
						}
					}
				}
			}
		}
		return prop != null && !prop.equals(property) ? prop : null;
	}
	
	/**
	 * Obtain the specified action property.
	 * An action property can either contain a simple value or reference a link property.
	 * If it is the latter it will obtain the referenced value stored in the link property.
	 * e.g. GetEntities filter=myfilter and myfilter="CreditAcctNo eq '{Acc}'", Acc="CreditAcctNo"
	 * => filter=CreditAcctNo eq '{CreditAcctNo}'
	 * @param propertyName Action property name
	 * @param actionProperties Action properties
	 * @param queryParameters Query parameters (keys)
	 * @return Action property string
	 */
	protected static String getActionProperty(String propertyName, Properties actionProperties, MultivaluedMap<String, String> queryParameters) {
		Object propObj = actionProperties.get(propertyName);
		if(propObj != null && propObj instanceof ActionPropertyReference) {
			ActionPropertyReference propRef = (ActionPropertyReference) propObj;
			Set<String> queryParamKeys = queryParameters.keySet();

			if (queryParameters.containsKey(propRef.getKey())) {
				return queryParameters.getFirst(propRef.getKey());
			}
			
			String key = "_";
			for(String queryParamKey : queryParamKeys) {
				if(!queryParamKey.startsWith("$")) {		//Do not consider $filter, $select, etc.
					key += "_" + queryParamKey;
				}
			}
			return propRef.getProperty(key);
		}
		else {
			return actionProperties.get(propertyName).toString();		//e.g. fld eq '{code}'
		}		
	}
	
	
	/**
	 * 
	 * @param filterString
	 * @return decoded string 
	 */
	protected static String getURLDecodedString(String filterString) {
		if(filterString == null || filterString.length() == 0) {
			return filterString;
		}
		try {
			return URLDecoder.decode(filterString, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
	}
}
