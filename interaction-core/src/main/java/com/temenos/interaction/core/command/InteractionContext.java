package com.temenos.interaction.core.command;

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


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.temenos.interaction.core.entity.EntityMetadata;
import com.temenos.interaction.core.entity.Metadata;
import com.temenos.interaction.core.hypermedia.Link;
import com.temenos.interaction.core.hypermedia.ResourceState;
import com.temenos.interaction.core.resource.RESTResource;
import com.temenos.interaction.core.rim.AcceptLanguageHeaderParser;
import com.temenos.interaction.core.rim.HTTPHypermediaRIM;

/**
 * This object holds the execution context for processing of the 
 * interaction commands.  {@link InteractionCommand}
 * @author aphethean
 */
public class InteractionContext {
	private final static Logger logger = LoggerFactory.getLogger(InteractionContext.class);

	/**
	 * The default path element key used if no other is specified when defining the resource.
	 */
	public final static String DEFAULT_ID_PATH_ELEMENT = "id";
	
	/* Execution context */
	private final HttpHeaders headers;
	private final MultivaluedMap<String, String> queryParameters;
	private final MultivaluedMap<String, String> pathParameters;
	private final ResourceState currentState;
	private final Metadata metadata;
	private ResourceState targetState;
	private Link linkUsed;
	private InteractionException exception;

	/* Command context */
	private RESTResource resource;
	private Map<String, Object> attributes = new HashMap<String, Object>();
	private String preconditionIfMatch = null;
	private List<String> preferredLanguages = new ArrayList<String>();
	private Map<String, String> responseHeaders = new HashMap<String, String>();

	/**
	 * Construct the context for execution of an interaction.
	 * @see HTTPHypermediaRIM for pre and post conditions of this InteractionContext
	 * 			following the execution of a command.
	 * @invariant pathParameters not null
	 * @invariant queryParameters not null
	 * @invariant currentState not null
	 * @param pathParameters
	 * @param queryParameters
	 */
	public InteractionContext(final HttpHeaders headers, final MultivaluedMap<String, String> pathParameters, final MultivaluedMap<String, String> queryParameters, final ResourceState currentState, final Metadata metadata) {
		this.headers = headers;
		this.pathParameters = pathParameters;
		this.queryParameters = queryParameters;
		this.currentState = currentState;
		this.metadata = metadata;
		assert(pathParameters != null);
		assert(queryParameters != null);
// TODO, should be able to enable this assertion, its just that alot of tests currently mock this 'new InteractionContext'
//		assert(currentState != null);
		assert(metadata != null);
	}

	/**
	 * Shallow copy constructor with extra parameters to override final attributes.
	 * @param ctx interaction context
	 * @param pathParameters new path parameters or null to not override
	 * @param queryParameters new query parameters or null to not override
	 * @param currentState new current state or null to not override
	 */
	public InteractionContext(InteractionContext ctx, final HttpHeaders headers, final MultivaluedMap<String, String> pathParameters, final MultivaluedMap<String, String> queryParameters, final ResourceState currentState) {
		this.headers = headers != null ? headers : ctx.getHeaders();
		this.pathParameters = pathParameters != null ? pathParameters : ctx.pathParameters;
		this.queryParameters = queryParameters != null ? queryParameters : ctx.queryParameters;
		this.currentState = currentState != null ? currentState : ctx.currentState;
		this.metadata = ctx.metadata;
		
		this.resource = ctx.resource;
		this.targetState = ctx.targetState;
		this.linkUsed = ctx.linkUsed;
		this.exception = ctx.exception;
		this.attributes = ctx.attributes;
	}
	
	/**
	 * <p>The query part of the uri (after the '?')</p>
	 * URI query parameters as a result of jax-rs {@link UriInfo#getQueryParameters(true)}
	 */
	public MultivaluedMap<String, String> getQueryParameters() {
		return queryParameters;
	}

	/**
	 * <p>the path part of the uri (up to the '?')</p>
	 * URI path parameters as a result of jax-rs {@link UriInfo#getPathParameters(true)}
	 */
	public MultivaluedMap<String, String> getPathParameters() {
		return pathParameters;
	}

	/**
	 * The HTTP headers of the current request.
	 * @return
	 */
	public HttpHeaders getHeaders() {
		return headers;
	}

	/**
	 * The HTTP headers to be set in the response.
	 * @return
	 */
	public Map<String,String> getResponseHeaders() {
		return responseHeaders;
	}

	/**
	 * The object form of the resource this interaction is dealing with.
	 * @return
	 */
	public RESTResource getResource() {
		return resource;
	}

	/**
	 * In terms of the hypermedia interactions this is the current application state.
	 * @return
	 */
	public ResourceState getCurrentState() {
		return currentState;
	}

	/**
	 * @see InteractionContext#getResource()
	 * @param resource
	 */
	public void setResource(RESTResource resource) {
		this.resource = resource;
	}
	
    public ResourceState getTargetState() {
		return targetState;
	}

	public void setTargetState(ResourceState targetState) {
		this.targetState = targetState;
	}

	public Link getLinkUsed() {
		return linkUsed;
	}

	public void setLinkUsed(Link linkUsed) {
		this.linkUsed = linkUsed;
	}

	public String getId() {
    	String id = null;
    	if (pathParameters != null) {
        	id = pathParameters.getFirst(DEFAULT_ID_PATH_ELEMENT);
        	if (id == null) {
        		if (getCurrentState().getPathIdParameter() != null) {
        			id = pathParameters.getFirst(getCurrentState().getPathIdParameter());
        		} else {
            		EntityMetadata entityMetadata = metadata.getEntityMetadata(getCurrentState().getEntityName());
            		if (entityMetadata != null) {
            			List<String> idFields = entityMetadata.getIdFields();
            			// TODO add support for composite ids
            			assert(idFields.size() == 1) : "ERROR we currently only support simple ids";
            			id = pathParameters.getFirst(idFields.get(0));
            		}
        		}
        	}
    		if (logger.isDebugEnabled()) {
            	for (String pathParam : pathParameters.keySet()) {
            		logger.debug("PathParam " + pathParam + ":" + pathParameters.get(pathParam));
            	}
    		}
    	}
    	return id;
    }

    /**
     * Store an attribute in this interaction context.
     * @param name
     * @param value
     */
    public void setAttribute(String name, Object value) {
    	attributes.put(name, value);
    }
    
    /**
     * Retrieve an attribute from this interaction context.
     * @param name
     * @return
     */
    public Object getAttribute(String name) {
    	return attributes.get(name);
    }
    
    /**
     * Returns the metadata from this interaction context
     * @return metadata
     */
    public Metadata getMetadata() {
    	return metadata;
    }

    /**
     * Obtain the last exception
     * @return exception
     */
	public InteractionException getException() {
		return exception;
	}

	/**
	 * Set an exception
	 * @param exception exception
	 */
	public void setException(InteractionException exception) {
		this.exception = exception;
	}

	/**
	 * Get If-Match precondition
	 * @return If-Match precondition
	 */
	public String getPreconditionIfMatch() {
		return preconditionIfMatch;
	}

	/**
	 * Set the If-Match precondition
	 * @param preconditionIfMatch If-Match precondition
	 */
	public void setPreconditionIfMatch(String preconditionIfMatch) {
		this.preconditionIfMatch = preconditionIfMatch;
	}
	
	/**
	 * Sets the http header AcceptLanguage value to the context.
	 * @param acceptLanguageValue
	 */
	public void setAcceptLanguage(String acceptLanguageValue) {
		preferredLanguages = new AcceptLanguageHeaderParser(acceptLanguageValue).getLanguageCodes();
	}
	
	/**
	 * Returns the list of language codes in the order of their preference.
	 * @return language codes
	 */
	public List<String> getLanguagePreference(){
		return preferredLanguages;
	}
}
