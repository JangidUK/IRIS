package com.temenos.interaction.core.resource;

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


import java.io.ByteArrayInputStream;
import java.io.InputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.temenos.interaction.core.entity.Metadata;
import com.temenos.interaction.core.entity.MetadataParser;
import com.temenos.interaction.core.entity.vocabulary.TermFactory;
import com.temenos.interaction.core.hypermedia.ResourceStateMachine;


/**
 * This class provides EDM metadata for the current service.
 */
public class ResourceMetadataManager {
	private final static Logger logger = LoggerFactory.getLogger(ResourceMetadataManager.class);

	private final static String METADATA_XML_FILE = "metadata.xml";

	private Metadata metadata = null;
	private TermFactory termFactory = null;
	
	/**
	 * Construct the metadata object
	 */
	public ResourceMetadataManager(Metadata metadata, ResourceStateMachine hypermediaEngine)
	{
		this.metadata = metadata;
	}

	/**
	 * Construct the metadata object
	 */
	public ResourceMetadataManager(String metdataXml, ResourceStateMachine hypermediaEngine)
	{
		metadata = parseMetadataXML(metdataXml);
	}
	
	/**
	 * Construct the metadata object
	 */
	public ResourceMetadataManager(ResourceStateMachine hypermediaEngine)
	{
		metadata = parseMetadataXML();
	}

	/**
	 * Construct the metadata object
	 */
	public ResourceMetadataManager(ResourceStateMachine hypermediaEngine, TermFactory termFactory)
	{
		metadata = parseMetadataXML(termFactory);
		this.termFactory = termFactory;
	}
	
	/*
	 * construct termFactory & Metadata
	 */
	public ResourceMetadataManager(TermFactory termFactory, Metadata entityMetadata)
	{
		this.termFactory = termFactory;
		this.metadata = entityMetadata;
	}
	

	/*
	 * construct only term factory
	 */
 	public ResourceMetadataManager()
	{
		termFactory = new TermFactory();
	}

	/*
	 * construct only term factory
	 */
 	public ResourceMetadataManager(TermFactory termFactory)
	{
		this.termFactory = termFactory;
	}
 	
	
	/**
	 * Return the entity model metadata
	 * @return metadata
	 */
	public Metadata getMetadata() {
		return this.metadata;
	}
	
	/*
	 * Parse the XML metadata file with the default Vocabulary Term Factory
	 */
	protected Metadata parseMetadataXML() {
		return parseMetadataXML(new TermFactory());
	}

	/*
	 * Parse the XML metadata file
	 */
	protected Metadata parseMetadataXML(TermFactory termFactory) {
		try {
			InputStream is = getClass().getClassLoader().getResourceAsStream(METADATA_XML_FILE);
			if(is == null) {
				throw new Exception("Unable to load " + METADATA_XML_FILE + " from classpath.");
			}
			return new MetadataParser(termFactory).parse(is);
		}
		catch(Exception e) {
			logger.error("Failed to parse " + METADATA_XML_FILE + ": " + e.getMessage());
			e.printStackTrace();
			throw new RuntimeException("Failed to parse " + METADATA_XML_FILE + ": " + e.getMessage());
		}
	}
	
	/*
	 * Parse the XML metadata string
	 */
	protected Metadata parseMetadataXML(String xml) {
		try {
			InputStream is = new ByteArrayInputStream(xml.getBytes());
			return new MetadataParser().parse(is);
		}
		catch(Exception e) {
			logger.error("Failed to parse metadata xml: " + e.getMessage());
			e.printStackTrace();
			throw new RuntimeException("Failed to parse metadata xml: " + e.getMessage());
		}
	}
	
	
	/*
	 *  get metadadata
	 */
	public Metadata getMetadata(String entityName) {
		if(termFactory == null) {
			logger.error("TermFactory Missing");
			throw new RuntimeException("TermFactory Missing");
		}
		return parseMetadataXML(entityName, termFactory);
	}

	
	/*
	 * Parse the XML entity metadata file
	 */
	protected Metadata parseMetadataXML(String entityName, TermFactory termFactory) {
		String metadataFilename;
		if(entityName == null ) {
			logger.error(entityName + " entity name received, loading " + METADATA_XML_FILE);
			metadataFilename = METADATA_XML_FILE;
		} else {
			metadataFilename = "metadata-" + entityName + ".xml";
		}
		
		try {
			InputStream is = getClass().getClassLoader().getResourceAsStream(metadataFilename);
			if(is == null) {
				logger.error("Unable to load " + metadataFilename + " from classpath.");
				is = getClass().getClassLoader().getResourceAsStream(METADATA_XML_FILE);
				if(is == null) {
					logger.error("Unable to load " + METADATA_XML_FILE + " from classpath.");
					throw new Exception("Unable to load " + metadataFilename + " and " + METADATA_XML_FILE + " from classpath.");
				}
			}
			return new MetadataParser(termFactory).parse(is);
		}
		catch(Exception e) {
			logger.error("Failed to parse " + metadataFilename + ": " + e.getMessage());
			throw new RuntimeException("Failed to parse " + metadataFilename + ": " + e.getMessage());
		}
	}
}
