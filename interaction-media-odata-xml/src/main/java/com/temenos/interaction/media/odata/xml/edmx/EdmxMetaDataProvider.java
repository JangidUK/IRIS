package com.temenos.interaction.media.odata.xml.edmx;

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

import javax.ws.rs.Consumes;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;

import org.odata4j.edm.EdmDataServices;

import com.temenos.interaction.core.resource.MetaDataResource;
import com.temenos.interaction.core.resource.RESTResource;
import com.temenos.interaction.core.resource.ResourceTypeHelper;

/**
 * JAX-RS Provider class for marshalling EDMX metadata resources.
 * 
 * EDMX representations have an xml media type but we allow atom+xml accept headers to cater
 * for OData clients which provide '* / *' accept headers.
 */
@Provider
@Consumes({MediaType.APPLICATION_XML})
@Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_ATOM_XML, MediaType.APPLICATION_XHTML_XML, MediaType.TEXT_HTML})
public class EdmxMetaDataProvider implements MessageBodyReader<RESTResource>, MessageBodyWriter<RESTResource> {
	
	public EdmxMetaDataProvider() {}
	
	@Override
	public boolean isWriteable(Class<?> type, Type genericType,
			Annotation[] annotations, MediaType mediaType) {
		return ResourceTypeHelper.isType(type, genericType, MetaDataResource.class);
	}

	@Override
	public long getSize(RESTResource t, Class<?> type, Type genericType,
			Annotation[] annotations, MediaType mediaType) {
		return -1;
	}

	/**
	 * Writes representation of {@link MetaDataResource} to the output stream.
	 * 
	 * @precondition supplied {@link MetaDataResource} is non null
	 * @precondition {@link MetaDataResource#getMetadata()} returns a valid EdmDataServices
	 * @postcondition non null meta data document written to OutputStream
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

		final String edmxString;
		if(ResourceTypeHelper.isType(type, genericType, MetaDataResource.class, EdmDataServices.class)) {
			//Set response headers
			if (httpHeaders != null) {
				httpHeaders.putSingle(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_XML);
			}
			MetaDataResource<EdmDataServices> metadataResource = (MetaDataResource<EdmDataServices>) resource;
			StringWriter sw = new StringWriter();
			EdmxMetaDataWriter.write(metadataResource.getMetadata(), sw);
			edmxString = sw.toString();
		}
		else {
			throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
		}
		outputStream.write(edmxString.getBytes("UTF-8"));
		outputStream.flush();
	}

	@Override
	public boolean isReadable(Class<?> type, Type genericType,
			Annotation[] annotations, MediaType mediaType) {
		return ResourceTypeHelper.isType(type, genericType, MetaDataResource.class);
	}

	/**
	 * Reads a representation of {@link MetaDataResource} from the input stream.
	 * 
	 * @precondition {@link InputStream} contains a valid meta data representation
	 * @postcondition {@link MetaDataResource} will be constructed and returned.
	 * @invariant valid InputStream
	 */
	@Override
	public MetaDataResource<EdmDataServices> readFrom(Class<RESTResource> type,
			Type genericType, Annotation[] annotations, MediaType mediaType,
			MultivaluedMap<String, String> httpHeaders, InputStream entityStream)
			throws IOException, WebApplicationException {
		return null;
	}

}
