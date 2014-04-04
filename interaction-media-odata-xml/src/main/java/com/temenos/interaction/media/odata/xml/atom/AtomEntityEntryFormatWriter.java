package com.temenos.interaction.media.odata.xml.atom;

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


import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;
import javax.xml.namespace.QName;

import org.apache.abdera.Abdera;
import org.apache.abdera.model.Text.Type;
import org.apache.abdera.writer.StreamWriter;
import org.apache.commons.io.output.WriterOutputStream;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDateTime;
import org.odata4j.core.OAtomEntity;
import org.odata4j.edm.EdmSimpleType;
import org.odata4j.edm.EdmType;
import org.odata4j.internal.InternalUtil;

import com.temenos.interaction.core.entity.Entity;
import com.temenos.interaction.core.entity.EntityMetadata;
import com.temenos.interaction.core.entity.EntityProperties;
import com.temenos.interaction.core.entity.EntityProperty;
import com.temenos.interaction.core.entity.Metadata;
import com.temenos.interaction.core.entity.vocabulary.terms.TermValueType;
import com.temenos.interaction.core.hypermedia.CollectionResourceState;
import com.temenos.interaction.core.hypermedia.Link;
import com.temenos.interaction.core.hypermedia.ResourceState;
import com.temenos.interaction.core.hypermedia.Transition;
import com.temenos.interaction.core.resource.CollectionResource;
import com.temenos.interaction.core.resource.EntityResource;
import com.temenos.interaction.core.resource.RESTResource;
import com.temenos.interaction.odataext.entity.MetadataOData4j;

/**
 * Writes an IRIS Entity out to Atom XML format using the Apache Abdera library.
 * 
 * @author srushworth
 * 
 */
public class AtomEntityEntryFormatWriter {
	// Constants for OData
	public static final String d = "http://schemas.microsoft.com/ado/2007/08/dataservices";
	public static final String m = "http://schemas.microsoft.com/ado/2007/08/dataservices/metadata";
	public static final String scheme = "http://schemas.microsoft.com/ado/2007/08/dataservices/scheme";
	public static final String atom_feed_content_type = "application/atom+xml;type=feed";
	public static final String atom_entry_content_type = "application/atom+xml;type=entry";
	public static final String href_lang = "en";

	private ResourceState serviceDocument;
	private Metadata metadata;

	public AtomEntityEntryFormatWriter(ResourceState serviceDocument, Metadata metadata) {
		this.serviceDocument = serviceDocument;
		this.metadata = metadata;
	}
	
	public void write(UriInfo uriInfo, Writer w, 
			String entityName, 
			Entity entity,
			Collection<Link> links, 
			Map<Transition,RESTResource> embeddedResources) 
	{
		String baseUri = AtomXMLProvider.getBaseUri(serviceDocument, uriInfo);
		String absoluteId = uriInfo.getBaseUri() + uriInfo.getPath();
		
		DateTime utc = new DateTime().withZone(DateTimeZone.UTC);
		String updated = InternalUtil.toString(utc);

		Abdera abdera = new Abdera();
		StreamWriter writer = abdera.newStreamWriter();
		writer.setOutputStream(new WriterOutputStream(w));
		//writer.setOutputStream(System.out,"UTF-8");
		writer.setAutoflush(false);
		writer.setAutoIndent(true);
		writer.startDocument();

		writer.startEntry();
	    writer.writeNamespace("d", d);
	    writer.writeNamespace("m", m);
	    writer.writeAttribute("xml:base", baseUri);
		writeEntry(writer, entityName, entity, links, embeddedResources, baseUri, absoluteId, updated);
		writer.endEntry();
		writer.endDocument();
		writer.flush();
	}

	public void writeEntry(StreamWriter writer, String entitySetName, String entityName, Entity entity,
			Collection<Link> entityLinks, Map<Transition,RESTResource> embeddedResources,
			UriInfo uriInfo, String updated) 
	{
		EntityMetadata entityMetadata = metadata.getEntityMetadata(entityName);
		String baseUri = AtomXMLProvider.getBaseUri(serviceDocument, uriInfo);
		String absoluteId = getAbsoluteId(baseUri, entitySetName, entity, entityMetadata);
		
		writer.startEntry();
		writeEntry(writer, entityName, entity, entityLinks, embeddedResources, baseUri, absoluteId, updated);
		writer.endEntry();
	}
	
	protected void writeEntry(StreamWriter writer,
			String entityName, 
			Entity entity,
			Collection<Link> entityLinks, 
			Map<Transition,RESTResource> embeddedResources,
			String baseUri, String absoluteId, String updated) 
	{
		assert(entityName != null);
		EntityMetadata entityMetadata = metadata.getEntityMetadata(entityName);
		String modelName = metadata.getModelName();
		writer.writeId(absoluteId);
		OAtomEntity oae = getAtomInfo(entity);

		writer.writeTitle(oae.getAtomEntityTitle());
		String summary = oae.getAtomEntitySummary();
		if (!summary.isEmpty()) {
			writer.writeSummary(summary);
		}

		LocalDateTime updatedTime = oae.getAtomEntityUpdated();
		if (updatedTime != null) {
			updated = InternalUtil.toString(updatedTime
					.toDateTime(DateTimeZone.UTC));
		}
		
		writer.writeUpdated(updated);
		writer.writeAuthor(oae.getAtomEntityAuthor());

		if (entityLinks != null) {
			for (Link link : entityLinks) {
		        String type = (link.getTransition().getTarget() instanceof CollectionResourceState)
			              ? atom_feed_content_type
			              : atom_entry_content_type;
			    String href = link.getRelativeHref(baseUri);
			    String rel = link.getRel();
	            writer.startLink(href, rel);
	            if (!"self".equals(link.getRel()) &&
	            		!"edit".equals(link.getRel())) {
		            writer.writeAttribute("type", type);
	            }
	            writer.writeAttribute("title", link.getTitle());
	            // TODO add support for hreflang
//	            writer.writeAttribute("hreflang", href_lang);
				if (embeddedResources != null && embeddedResources.get(link.getTransition()) != null) {
		            // write the inlined entities inside the link element
					String embeddedAbsoluteId = link.getHref();
					writeLinkInline(writer, metadata, link, embeddedResources.get(link.getTransition()), link.getHref(), baseUri, embeddedAbsoluteId, updated);
				}
	            writer.endLink();
			}
		}

		writer.writeCategory(modelName + Metadata.MODEL_SUFFIX + "." + entityName, scheme);
		writer.flush();
		
		writer.startContent(MediaType.APPLICATION_XML);
		
		writer.startElement(new QName(m, "properties", "m"));
		if (entity != null) {
			writeProperties(writer, entityMetadata, entity.getProperties(), modelName);
		}
		writer.endElement();
		
		writer.endContent();
	}

	@SuppressWarnings("unchecked")
	protected void writeLinkInline(StreamWriter writer, Metadata metadata, Link linkToInline, RESTResource embeddedResource,
			String href, String baseUri, String absoluteId, String updated) {

		writer.startElement(new QName(m, "inline", "m"));
		if (embeddedResource instanceof CollectionResource) {
			CollectionResource<Entity> collectionResource = (CollectionResource<Entity>) embeddedResource;
			Collection<EntityResource<Entity>> entities = collectionResource.getEntities();

			if (entities != null && !entities.isEmpty()) {
				writer.startFeed();
				writer.writeTitle(Type.TEXT, linkToInline.getTitle());
				writer.writeId(baseUri + href);
				writer.writeUpdated(updated);
				writer.startLink(href, "self");
	            writer.writeAttribute("title", linkToInline.getTitle());
				writer.endLink();
				
				for (EntityResource<Entity> entityResource : entities) {
					writer.startEntry();
					writeEntry(writer, entityResource.getEntityName(), entityResource.getEntity(), entityResource.getLinks(), entityResource.getEmbedded(),
							baseUri, absoluteId, updated);
					writer.endEntry();
				}
				writer.endFeed();
			}
		} else if (embeddedResource instanceof EntityResource) {
			EntityResource<Entity> entityResource = (EntityResource<Entity>) embeddedResource;
			Entity entity = entityResource.getEntity();
			writer.startEntry();
			writeEntry(writer, entityResource.getEntityName(), entity, entityResource.getLinks(), entityResource.getEmbedded(),
					baseUri, absoluteId, updated);

			writer.endEntry();
		} else
			throw new RuntimeException("Unknown OLink type "
					+ linkToInline.getClass());
		writer.endElement();  // end inline
	}
	  
	private String getAbsoluteId(String baseUri, String entitySetName, Entity entity, EntityMetadata entityMetadata) {
		String absId = "";
		for(String key : entityMetadata.getIdFields()) {		
			EntityProperty prop = entity.getProperties().getProperty(entityMetadata.getSimplePropertyName(key));
			if(prop != null) {
				absId += absId.isEmpty() ? (!baseUri.endsWith("/") ? baseUri + "/" : baseUri) + entitySetName : ",";
				if(entityMetadata.isPropertyNumber(prop.getFullyQualifiedName())) {
					absId += "(" + entityMetadata.getPropertyValueAsString(prop) + ")";
				}
				else {
					absId += "('" + entityMetadata.getPropertyValueAsString(prop) + "')";
				}
			}
		}
		return absId;
	}
	
	private void writeProperties(StreamWriter writer, EntityMetadata entityMetadata, EntityProperties entityProperties, String modelName) {
		assert(entityMetadata != null);
		// Loop round all properties writing out fields and MV and SV sets
		Map<String, EntityProperty> properties = entityProperties.getProperties();
		
		for (String propertyName:properties.keySet())
		{
			// Work out what the property looks like by looking at the metadata
			EntityProperty property = (EntityProperty) properties.get(propertyName);
			boolean isComplex = entityMetadata.isPropertyComplex(property.getFullyQualifiedName());
	   		if( !isComplex )
	   		{
	   			// Simple field
	   			writeProperty( writer, entityMetadata, property);
	   		}
	   		else
	   		{
	   			// Complex List
	   			writePropertyComplexList( writer, entityMetadata, property, modelName);
	   		}
	   	}
	}
	
	private void writeProperty( StreamWriter writer, EntityMetadata entityMetadata, EntityProperty property ) {
		String elementText = entityMetadata.getPropertyValueAsString( property );
		writer.startElement(new QName(d, property.getName(), "d"));
		EdmType type = MetadataOData4j.termValueToEdmType(entityMetadata.getTermValue(property.getFullyQualifiedName(), TermValueType.TERM_NAME));
		boolean isNullable = entityMetadata.isPropertyNullable(property.getFullyQualifiedName());
		// Append Type Attribute
		if(!type.equals(EdmSimpleType.STRING)) {
			writer.writeAttribute(new QName(m, "type", "m"), type.getFullyQualifiedTypeName());
		}
		// Append Null attribute
		if ( isNullable && (elementText.isEmpty()) && !type.equals(EdmSimpleType.STRING)) {
			writer.writeAttribute(new QName(m, "null", "m"), "true");
		}
		//Write the property text
		if(type.equals(EdmSimpleType.DATETIME) && !elementText.isEmpty()) {
			//Write dates in UTC format
			SimpleDateFormat formatUTC = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
			formatUTC.setTimeZone(TimeZone.getTimeZone("UTC"));
			writer.writeElementText(formatUTC.format((Date) property.getValue()));
		}		
		else if (elementText != null) {
			writer.writeElementText(elementText);
		}
		writer.endElement();
	}
	
	/**
	 * Method to prepare Complex type representation. 
	 * @param writer
	 * @param entityMetadata
	 * @param propertyName
	 * @param propertiesList
	 * @param modelName
	 */
	private void writePropertyComplexList( StreamWriter writer, EntityMetadata entityMetadata, EntityProperty property, String modelName) {
		@SuppressWarnings("unchecked")
		List<EntityProperties> propertiesList = (List<EntityProperties>) property.getValue();
		String name = entityMetadata.getEntityName() + "_" + property.getName();
		int parseCount = 0;
		for ( EntityProperties properties : propertiesList ) {
			String fqTypeName = modelName + Metadata.MODEL_SUFFIX + "." + name;
			// We should be able to differentiate List<ComplexType> with regular ComplexType 
			if (entityMetadata.isPropertyList(property.getFullyQualifiedName())) {
				if (parseCount == 0) {
					writer.startElement(new QName(d, name, "d"));
					writer.writeAttribute(new QName(m, "type", "m"), "Bag(" + fqTypeName + ")");
					writer.startElement(new QName(d, "element", "d"));
					parseCount++;
				} else {
					writer.startElement(new QName(d, "element", "d"));
				}
			} else {
				writer.startElement(new QName(d, name, "d"));
				writer.writeAttribute(new QName(m, "type", "m"), fqTypeName);
			}
			writeProperties( writer, entityMetadata, properties, modelName );
			writer.endElement();
		}
		// For List<ComplexTypes> we should end the complex node here
		if (entityMetadata.isPropertyList(property.getFullyQualifiedName())) {
			writer.endElement();
		}
	}
	
	private OAtomEntity getAtomInfo(Entity e) {

		return new OAtomEntity() {
			@Override
			public String getAtomEntityTitle() {
				return "";
			}

			@Override
			public String getAtomEntitySummary() {
				return "";
			}

			@Override
			public String getAtomEntityAuthor() {
				return "";
			}

			@Override
			public LocalDateTime getAtomEntityUpdated() {
				return null;
			}
		};
	}
}
