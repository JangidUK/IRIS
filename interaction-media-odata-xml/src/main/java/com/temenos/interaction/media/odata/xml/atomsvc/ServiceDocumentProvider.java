package com.temenos.interaction.media.odata.xml.atomsvc;

/*
 * #%L
 * interaction-media-odata-xml
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


import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringWriter;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Arrays;

import javax.ws.rs.Consumes;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;

import org.odata4j.edm.EdmDataServices;
import org.odata4j.format.FormatWriter;
import org.odata4j.format.FormatWriterFactory;

import com.temenos.interaction.core.ExtendedMediaTypes;
import com.temenos.interaction.core.hypermedia.ResourceState;
import com.temenos.interaction.core.hypermedia.ResourceStateMachine;
import com.temenos.interaction.core.resource.EntityResource;
import com.temenos.interaction.core.resource.RESTResource;
import com.temenos.interaction.core.resource.ResourceTypeHelper;

/**
 * JAX-RS Provider class for marshalling Service document resources.
 * 
 * Service document representations are atomsvc+xml media types but we allow 
 * atom+xml accept headers to cater for OData clients which provide '* / *' 
 * accept headers.
 */
@Provider
@Consumes({ExtendedMediaTypes.APPLICATION_ATOMSVC_XML})
@Produces({ExtendedMediaTypes.APPLICATION_ATOMSVC_XML, MediaType.APPLICATION_ATOM_XML, MediaType.APPLICATION_XML})
public class ServiceDocumentProvider implements MessageBodyReader<RESTResource>, MessageBodyWriter<RESTResource> {
	@Context
	private UriInfo uriInfo;
	private final ResourceState serviceDocument;

	public ServiceDocumentProvider(ResourceStateMachine hypermediaEngine) {
		this.serviceDocument = hypermediaEngine.getResourceStateByName("ServiceDocument");
		if (serviceDocument == null)
			throw new RuntimeException("No 'ServiceDocument' found.");
	}

	@Override
	public boolean isWriteable(Class<?> type, Type genericType,
			Annotation[] annotations, MediaType mediaType) {
		if (mediaType.equals(ExtendedMediaTypes.APPLICATION_ATOMSVC_XML_TYPE)
				|| mediaType.equals(MediaType.APPLICATION_ATOM_XML_TYPE) 
				|| mediaType.equals(MediaType.APPLICATION_XML_TYPE)) {
			return ResourceTypeHelper.isType(type, genericType, EntityResource.class, EdmDataServices.class);			
		}
		return false;
	}

	@Override
	public long getSize(RESTResource t, Class<?> type, Type genericType,
			Annotation[] annotations, MediaType mediaType) {
		return -1;
	}

	/**
	 * Writes representation of {@link EntityResource} to the output stream.
	 * 
	 * @precondition supplied {@link EntityResource} is non null
	 * @precondition {@link EntityResource#getEntity()} returns a valid EdmDataServices
	 * @postcondition non null service document written to OutputStream
	 * @invariant valid OutputStream
	 */
	@SuppressWarnings("unchecked")
	@Override
	public void writeTo(RESTResource resource, Class<?> type, Type genericType,
			Annotation[] annotations, MediaType mediaType,
			MultivaluedMap<String, Object> httpHeaders,
			OutputStream outputStream) throws IOException,
			WebApplicationException {
		assert (resource != null);

		final String svcDocString;
		if(ResourceTypeHelper.isType(type, genericType, EntityResource.class, EdmDataServices.class)) {
			EntityResource<EdmDataServices> serviceDocumentResource = (EntityResource<EdmDataServices>) resource;
		    EdmDataServices metadata = (EdmDataServices) serviceDocumentResource.getEntity();
		    StringWriter sw = new StringWriter();
		    MediaType[] acceptedMediaTypes = { ExtendedMediaTypes.APPLICATION_ATOMSVC_XML_TYPE };
		    FormatWriter<EdmDataServices> fw = FormatWriterFactory.getFormatWriter(EdmDataServices.class, Arrays.asList(acceptedMediaTypes), "atom", null);
		    ExUriInfo extUriInfo = new ExUriInfo(serviceDocument, uriInfo);
		    fw.write(extUriInfo, sw, metadata);
			svcDocString = sw.toString();
			
			//Set HTTP response headers
			if(httpHeaders != null) {
				httpHeaders.putSingle(HttpHeaders.CONTENT_TYPE, ExtendedMediaTypes.APPLICATION_ATOMSVC_XML_TYPE);
			}
		}
		else {
			throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
		}
		outputStream.write(svcDocString.getBytes("UTF-8"));
		outputStream.flush();
	}

	@Override
	public boolean isReadable(Class<?> type, Type genericType,
			Annotation[] annotations, MediaType mediaType) {
		return ResourceTypeHelper.isType(type, genericType, EntityResource.class);
	}

	/**
	 * Reads a representation of {@link EntityResource} from the input stream.
	 * 
	 * @precondition {@link InputStream} contains a valid service document representation
	 * @postcondition {@link EntityResource} will be constructed and returned.
	 * @invariant valid InputStream
	 */
	@Override
	public EntityResource<EdmDataServices> readFrom(Class<RESTResource> type,
			Type genericType, Annotation[] annotations, MediaType mediaType,
			MultivaluedMap<String, String> httpHeaders, InputStream entityStream)
			throws IOException, WebApplicationException {
		return null;
	}

	protected void setUriInfo(UriInfo uriInfo) {
		this.uriInfo = uriInfo;
	}
}
