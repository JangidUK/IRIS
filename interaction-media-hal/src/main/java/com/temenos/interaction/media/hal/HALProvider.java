package com.temenos.interaction.media.hal;

/*
 * #%L
 * interaction-media-hal
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


import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.ws.rs.Consumes;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;

import org.odata4j.core.OEntity;
import org.odata4j.core.OProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.temenos.interaction.core.command.InteractionContext;
import com.temenos.interaction.core.entity.Entity;
import com.temenos.interaction.core.entity.EntityMetadata;
import com.temenos.interaction.core.entity.EntityProperties;
import com.temenos.interaction.core.entity.EntityProperty;
import com.temenos.interaction.core.entity.Metadata;
import com.temenos.interaction.core.hypermedia.Event;
import com.temenos.interaction.core.hypermedia.Link;
import com.temenos.interaction.core.hypermedia.ResourceState;
import com.temenos.interaction.core.hypermedia.ResourceStateMachine;
import com.temenos.interaction.core.hypermedia.Transition;
import com.temenos.interaction.core.resource.CollectionResource;
import com.temenos.interaction.core.resource.EntityResource;
import com.temenos.interaction.core.resource.RESTResource;
import com.temenos.interaction.core.resource.ResourceTypeHelper;
import com.theoryinpractise.halbuilder.api.ReadableRepresentation;
import com.theoryinpractise.halbuilder.api.Representation;
import com.theoryinpractise.halbuilder.api.RepresentationException;
import com.theoryinpractise.halbuilder.api.RepresentationFactory;
import com.theoryinpractise.halbuilder.standard.StandardRepresentationFactory;

@Provider
@Consumes({com.temenos.interaction.media.hal.MediaType.APPLICATION_HAL_XML, com.temenos.interaction.media.hal.MediaType.APPLICATION_HAL_JSON, MediaType.APPLICATION_JSON})
@Produces({com.temenos.interaction.media.hal.MediaType.APPLICATION_HAL_XML, com.temenos.interaction.media.hal.MediaType.APPLICATION_HAL_JSON, MediaType.APPLICATION_JSON})
public class HALProvider implements MessageBodyReader<RESTResource>, MessageBodyWriter<RESTResource> {
	private final Logger logger = LoggerFactory.getLogger(HALProvider.class);

	@Context
	private UriInfo uriInfo;
	@Context
	private Request requestContext;
	private Metadata metadata = null;
	private ResourceStateMachine hypermediaEngine;
    private RepresentationFactory representationFactory = new StandardRepresentationFactory();

	public HALProvider(Metadata metadata, ResourceStateMachine hypermediaEngine) {
		this(metadata);
		this.hypermediaEngine = hypermediaEngine;
	}

	public HALProvider(Metadata metadata) {
		this.metadata = metadata;
		assert(metadata != null);
	}
	
	@Override
	public boolean isWriteable(Class<?> type, Type genericType,
			Annotation[] annotations, MediaType mediaType) {
		if (mediaType.equals(com.temenos.interaction.media.hal.MediaType.APPLICATION_HAL_XML_TYPE)
				|| mediaType.equals(com.temenos.interaction.media.hal.MediaType.APPLICATION_HAL_JSON_TYPE)
				|| mediaType.equals(MediaType.APPLICATION_JSON_TYPE)) {
			return ResourceTypeHelper.isType(type, genericType, EntityResource.class)
					|| ResourceTypeHelper.isType(type, genericType, CollectionResource.class);
		}
		return false;
	}

	@Override
	public long getSize(RESTResource t, Class<?> type, Type genericType,
			Annotation[] annotations, MediaType mediaType) {
		return -1;
	}

	/**
	 * Writes a Hypertext Application Language (HAL) representation of
	 * {@link EntityResource} to the output stream.
	 * 
	 * @precondition supplied {@link EntityResource} is non null
	 * @precondition {@link EntityResource#getEntity()} returns a valid OEntity, this 
	 * provider only supports serialising OEntities
	 * @postcondition non null HAL XML document written to OutputStream
	 * @invariant valid OutputStream
	 */
	@Override
	public void writeTo(RESTResource resource, Class<?> type, Type genericType,
			Annotation[] annotations, MediaType mediaType,
			MultivaluedMap<String, Object> httpHeaders,
			OutputStream entityStream) throws IOException,
			WebApplicationException {
		assert (resource != null);
		logger.debug("Writing " + mediaType);
		
		if (!ResourceTypeHelper.isType(type, genericType, EntityResource.class)
				&& !ResourceTypeHelper.isType(type, genericType, CollectionResource.class))
			throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);

		// create the hal resource
        Representation halResource = representationFactory.newRepresentation(uriInfo.getBaseUri());
		if (resource.getGenericEntity() != null) {
			// get the links
			Collection<Link> links = resource.getLinks();
			Link selfLink = findSelfLink(links);
			
			// build the HAL representation with self link
			if (selfLink != null)
				halResource = representationFactory.newRepresentation(selfLink.getHref());

			// add our links
			if (links != null) {
				for (Link l : links) {
					if (l.equals(selfLink))
						continue;
					logger.debug("Link: id=[" + l.getId() + "] rel=[" + l.getRel() + "] method=[" + l.getMethod() + "] href=[" + l.getHref() + "]");
					// Representation withLink(String rel, String href, String name, String title, String hreflang, String profile);
					String[] rels = new String[0];
					if (l.getRel() != null) {
						rels = l.getRel().split(" ");
					}
					
					if (rels != null) {
						for (int i = 0 ; i < rels.length; i++) {
							halResource.withLink(rels[i], l.getHref(), l.getId(), l.getTitle(), null, null); 
						}
					}
				}
			}
			
			// add the embedded resources
			Map<Transition, RESTResource> embedded = resource.getEmbedded();
			if (embedded != null) {
				for (Transition t : embedded.keySet()) {
					RESTResource embeddedResource = embedded.get(t);
					// TODO work our rel for embedded resource, just as we need to work out the rel for the other links
					Link link = findLinkByTransition(links, t);
					assert(link != null);
					String rel = (link.getRel() != null ? link.getRel() : "embedded/" + embeddedResource.getEntityName());
					logger.debug("Embedded: rel=[" + rel + "] href=[" + link.getHref() + "]");
					ReadableRepresentation embeddedRepresentation = buildRepresentation(representationFactory.newRepresentation(link.getHref()), embeddedResource, null, null);
					halResource.withRepresentation(rel, embeddedRepresentation);
				}
			}

			// add contents of supplied entity to the representation
			buildRepresentation(halResource, resource, type, genericType);

		}
				
		String representation = null;
		if (halResource != null && mediaType.isCompatible(com.temenos.interaction.media.hal.MediaType.APPLICATION_HAL_XML_TYPE)) {
			representation = halResource.toString(RepresentationFactory.HAL_XML);
		} else if (halResource != null && mediaType.isCompatible(com.temenos.interaction.media.hal.MediaType.APPLICATION_HAL_JSON_TYPE)) {
			representation = halResource.toString(RepresentationFactory.HAL_JSON);
		} else if (halResource != null && mediaType.isCompatible(MediaType.APPLICATION_JSON_TYPE)) {
			representation = halResource.toString(RepresentationFactory.HAL_JSON);
		} else {
			throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
		}
		assert(representation != null);
		logger.debug("Produced [" + representation + "]");
		// TODO handle requested encoding?
		entityStream.write(representation.getBytes("UTF-8"));
	}

	private Link findLinkByTransition(Collection<Link> links, Transition transition) {
		Link link = null;
		if (links != null) {
			for (Link l : links) {
				if (l.getTransition() != null && l.getTransition().equals(transition)) {
					link = l;
					break;
				}
			}
		}
		return link;
	}
	
	protected Link findSelfLink(Collection<Link> links) {
		Link selfLink = null;
		if (links != null) {
			for (Link l : links) {
				Transition t = l.getTransition();
				// TODO this bit is a bit hacky.  The latest version of the HAL spec should not require us to find a 'self' link for the subresource
				if (l.getRel().contains("self") ||
						(l.getTransition() != null 
						&& (t.getCommand().getMethod() == null || t.getCommand().getMethod().equals("GET"))
						&& t.getTarget().getEntityName().equals(t.getSource().getEntityName()))) {
					selfLink = l;
					break;
				}
			}
		}
		return selfLink;
	}

	protected void buildFromOEntity(Map<String, Object> map, OEntity entity, String entityName) {
		EntityMetadata entityMetadata = metadata.getEntityMetadata(entityName);
		if (entityMetadata == null)
			throw new IllegalStateException("Entity metadata could not be found [" + entityName + "]");

		for (OProperty<?> property : entity.getProperties()) {
			// add properties if they are present on the resolved entity
			if (entityMetadata.getPropertyVocabulary(property.getName()) != null && property.getValue() != null) {
				// call toString on object as a simple why of handling non simple types
				map.put(property.getName(), property.getValue().toString());				
			}
		}
	}
	
	protected void buildFromEntity(Map<String, Object> map, Entity entity) {

		EntityProperties entityProperties = entity.getProperties();
		Map<String, EntityProperty> properties = entityProperties.getProperties();
				
		for (Map.Entry<String, EntityProperty> property : properties.entrySet()) 
		{
			String propertyName = property.getKey(); 
			EntityProperty propertyValue = (EntityProperty) property.getValue();
	   		map.put(propertyName, propertyValue.getValue());	
		}
	}
	
	protected void buildFromBean(Map<String, Object> map, Object bean, String entityName) {
		EntityMetadata entityMetadata = metadata.getEntityMetadata(entityName);
		if (entityMetadata == null)
			throw new IllegalStateException("Entity metadata could not be found [" + entityName + "]");

		try {
			BeanInfo beanInfo = Introspector.getBeanInfo(bean.getClass());
			for (PropertyDescriptor propertyDesc : beanInfo.getPropertyDescriptors()) {
			    String propertyName = propertyDesc.getName();
				if (entityMetadata.getPropertyVocabulary(propertyName) != null) {
				    Object value = propertyDesc.getReadMethod().invoke(bean);
					map.put(propertyName, value);				
				}
			}
		} catch (IllegalArgumentException e) {
			logger.error("Error accessing bean property", e);
		} catch (IntrospectionException e) {
			logger.error("Error accessing bean property", e);
		} catch (IllegalAccessException e) {
			logger.error("Error accessing bean property", e);
		} catch (InvocationTargetException e) {
			logger.error("Error accessing bean property", e);
		}
	}

	private Representation buildRepresentation(Representation halResource, RESTResource resource, Class<?> type, Type genericType) {
		if (genericType == null)
			genericType = resource.getGenericEntity().getType();
		if (type == null)
			type = resource.getGenericEntity().getRawType();
		if (ResourceTypeHelper.isType(type, genericType, EntityResource.class, OEntity.class)) {
			@SuppressWarnings("unchecked")
			EntityResource<OEntity> oentityResource = (EntityResource<OEntity>) resource;
			Map<String, Object> propertyMap = new HashMap<String, Object>();
			buildFromOEntity(propertyMap, oentityResource.getEntity(), oentityResource.getEntityName());
			// add properties to HAL resource
			for (String key : propertyMap.keySet()) {
				halResource.withProperty(key, propertyMap.get(key));
			}
		} else if (ResourceTypeHelper.isType(type, genericType, EntityResource.class, Entity.class)) {
				@SuppressWarnings("unchecked")
				EntityResource<Entity> entityResource = (EntityResource<Entity>) resource;
				Map<String, Object> propertyMap = new HashMap<String, Object>();
				buildFromEntity(propertyMap, entityResource.getEntity());
				// add properties to HAL resource
				for (String key : propertyMap.keySet()) {
					halResource.withProperty(key, propertyMap.get(key));
				}
		} else if (ResourceTypeHelper.isType(type, genericType, EntityResource.class)) {
			EntityResource<?> entityResource = (EntityResource<?>) resource;
			Object entity = entityResource.getEntity();
			if (entity != null) {
				/*
				 * // regular java bean
				 * halResource.withBean(entity);
				 */
				// java bean, now limited to just the properties specified in the metadata entity model
				Map<String, Object> propertyMap = new HashMap<String, Object>();
				buildFromBean(propertyMap, entity, entityResource.getEntityName());
				for (String key : propertyMap.keySet()) {
					halResource.withProperty(key, propertyMap.get(key));
				}
			}
		} else if(ResourceTypeHelper.isType(type, genericType, CollectionResource.class, OEntity.class)) {
			@SuppressWarnings("unchecked")
			CollectionResource<OEntity> cr = (CollectionResource<OEntity>) resource;
			List<EntityResource<OEntity>> entities = (List<EntityResource<OEntity>>) cr.getEntities();
			for (EntityResource<OEntity> er : entities) {
				OEntity entity = er.getEntity();
				// the subresource is an item of the collection (http://tools.ietf.org/html/rfc6573)
				String rel = "item";
				// the properties
				Map<String, Object> propertyMap = new HashMap<String, Object>();
				buildFromOEntity(propertyMap, entity, cr.getEntityName());
				// create hal resource and add link for self - if there is one
				Representation subResource = representationFactory.newRepresentation();
				
				for (Link el : er.getLinks()) {
					subResource.withLink(el.getRel(), el.getHref());
				}
				// add properties to HAL sub resource
				for (String key : propertyMap.keySet()) {
					subResource.withProperty(key, propertyMap.get(key));
				}
				halResource.withRepresentation(rel, subResource);
			}
		} else if (ResourceTypeHelper.isType(type, genericType, CollectionResource.class)) {
			@SuppressWarnings("unchecked")
			CollectionResource<Object> cr = (CollectionResource<Object>) resource;
			List<EntityResource<Object>> entities = (List<EntityResource<Object>>) cr.getEntities();
			for (EntityResource<Object> er : entities) {
				Object entity = er.getEntity();
				// the subresource is part of a collection (maybe this link rel should be an 'item')
				String rel = "collection." + cr.getEntityName();
				// the properties
				Map<String, Object> propertyMap = new HashMap<String, Object>();
				buildFromBean(propertyMap, entity, cr.getEntityName());
				// create hal resource and add link for self
				Link itemSelfLink = findSelfLink(er.getLinks());
				if (itemSelfLink != null) {
					Representation subResource = representationFactory.newRepresentation(itemSelfLink.getHref());
					for (Link el : er.getLinks()) {
						String itemHref = el.getHref();
						/*
						don't add links twice, this break the client assertion of one rel per link (which seems wrong)
						List<com.theoryinpractise.halbuilder.api.Link> selfLinks = subResource.getLinksByRel("self");
						assert(selfLinks != null && selfLinks.size() == 1);
						*/
						if (!itemSelfLink.equals(el)) {
							subResource.withLink(el.getRel(), itemHref, el.getId(), el.getTitle(), null, null);
						}
					}
					// add properties to HAL sub resource
					for (String key : propertyMap.keySet()) {
						subResource.withProperty(key, propertyMap.get(key));
					}
					halResource.withRepresentation(rel, subResource);
				}
				
			}
			
		} else {
			logger.error("Accepted object for writing in isWriteable, but type not supported in writeTo method");
			throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
		}
		return halResource;
	}
	
	@Override
	public boolean isReadable(Class<?> type, Type genericType,
			Annotation[] annotations, MediaType mediaType) {
		// this class can only deserialise EntityResource
		return ResourceTypeHelper.isType(type, genericType, EntityResource.class);
	}

	/**
	 * Reads a Hypertext Application Language (HAL) representation of
	 * {@link EntityResource} from the input stream.
	 * 
	 * @precondition {@link InputStream} contains a valid HAL <resource/> document
	 * @postcondition {@link EntityResource} will be constructed and returned.
	 * @invariant valid InputStream
	 */
	@Override
	public RESTResource readFrom(Class<RESTResource> type, Type genericType,
			Annotation[] annotations, MediaType mediaType,
			MultivaluedMap<String, String> httpHeaders, InputStream entityStream)
			throws IOException, WebApplicationException {

		// check media type can be handled, isReadable must have been called
		assert(ResourceTypeHelper.isType(type, genericType, EntityResource.class) 
				&& (mediaType.isCompatible(com.temenos.interaction.media.hal.MediaType.APPLICATION_HAL_XML_TYPE) 
						|| mediaType.isCompatible(com.temenos.interaction.media.hal.MediaType.APPLICATION_HAL_JSON_TYPE)));

		//Parse hal+json into an OEntity object
		Entity entity = buildEntityFromHal(entityStream);
		return new EntityResource<Entity>(entity);
	}
	
	private Entity buildEntityFromHal(InputStream entityStream) {
		try {
			// create the hal resource
			String baseUri = uriInfo.getBaseUri().toASCIIString();
			RepresentationFactory representationFactory = new StandardRepresentationFactory();
			ReadableRepresentation halResource = representationFactory.readRepresentation(new InputStreamReader(entityStream));
			// assume the client providing the representation knows something we don't
			String resourcePath = halResource.getResourceLink() != null ? halResource.getResourceLink().getHref() : null;
			if (resourcePath == null) {
				// work out the resource path from UriInfo
				String path = uriInfo.getPath();
				resourcePath = path;
			}
			logger.info("Reading HAL content for [" + resourcePath + "]");
			if (resourcePath == null)
				throw new IllegalStateException("No resource found");
			// trim the baseuri
			if (resourcePath.length() > baseUri.length() && resourcePath.startsWith(baseUri))
				resourcePath = resourcePath.substring(baseUri.length() - 1);
			/*
			 * add a leading '/' if it needs it (when defining resources we must use a 
			 * full path, but requests can be relative, i.e. without a '/'
			 */
			if (!resourcePath.startsWith("/")) {
				resourcePath = "/" + resourcePath;
			}
			// get the entity name
			String entityName = getEntityName(resourcePath);
			EntityMetadata entityMetadata = metadata.getEntityMetadata(entityName);
			if (entityMetadata == null)
				throw new IllegalStateException("Entity metadata could not be found [" + entityName + "]");
			// add properties if they are present on the resolved entity
			EntityProperties entityFields = new EntityProperties();
			Map<String, Object> halProperties = halResource.getProperties();
			for (String propName : halProperties.keySet()) {
				if (entityMetadata.getPropertyVocabulary(propName) != null) {
					Object halValue = getHalPropertyValue(entityMetadata, propName, halProperties.get(propName));
					entityFields.setProperty(new EntityProperty(propName, halValue));
				}
			}
			return new Entity(entityName, entityFields);
		} catch (RepresentationException e) {
			logger.warn("Malformed request from client", e);
			throw new WebApplicationException(Status.BAD_REQUEST);
		} catch (IllegalStateException e) {
			logger.warn("Malformed request from client", e);
			throw new WebApplicationException(Status.BAD_REQUEST);
		}
	}
	
	private String getEntityName(String resourcePath) {
		String entityName = null;
		if (resourcePath != null) {
			MultivaluedMap<String, String> pathParameters = uriInfo.getPathParameters();
			if (pathParameters != null) {
				for (String key : pathParameters.keySet()) {
					List<String> values = pathParameters.get(key);
					for (String value : values) {
						resourcePath = resourcePath.replace(value, "{" + key + "}");
					}
				}
			}
			String httpMethod = requestContext.getMethod();
			Event event = new Event(httpMethod, httpMethod);
			ResourceState state = hypermediaEngine.determineState(event, resourcePath);
			if (state != null) {
				entityName = state.getEntityName();
			} else {
				logger.error("No state found, dropping back to path matching");
				Map<String, Set<ResourceState>> pathToResourceStates = hypermediaEngine.getResourceStatesByPath();
				for (String path : pathToResourceStates.keySet()) {
					for (ResourceState s : pathToResourceStates.get(path)) {
						String pathIdParameter = InteractionContext.DEFAULT_ID_PATH_ELEMENT;
						if (s.getPathIdParameter() != null) {
							pathIdParameter = s.getPathIdParameter();
						}
						Matcher matcher = Pattern.compile("(.*)\\{" + pathIdParameter + "\\}(.*)").matcher(path);
						if (matcher.find()) {
							int groupCount = matcher.groupCount();
							if ((groupCount == 1 && resourcePath.startsWith(matcher.group(1))) ||
								(groupCount == 2 && resourcePath.startsWith(matcher.group(1)) && resourcePath.endsWith(matcher.group(2)))) {
								entityName = s.getEntityName();
							}
						}
						if (entityName == null && path.startsWith(resourcePath)) {
							entityName = s.getEntityName();
						}
					}
				}
			}
		}
		return entityName;
	}
	
	/* Ugly testing support :-( */
	protected void setUriInfo(UriInfo uriInfo) {
		this.uriInfo = uriInfo;
	}
	protected void setRequestContext(Request request) {
		this.requestContext = request;
	}

	private Object getHalPropertyValue( EntityMetadata entityMetadata, String propertyName, Object halPropertyValue )
	{
		String stringValue = halPropertyValue.toString();
		Object typedValue;
		
		if ( entityMetadata.isPropertyText( propertyName ) )
		{
			typedValue = stringValue;
		}
		else if ( entityMetadata.isPropertyNumber( propertyName ) )
		{
			typedValue = Long.parseLong( stringValue );
		}
		else
		{
			typedValue = stringValue;
		}
		
		return typedValue;
	}
}
