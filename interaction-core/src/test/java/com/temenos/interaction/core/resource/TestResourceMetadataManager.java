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


import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;

import java.io.InputStream;
import java.io.StringWriter;

import org.apache.commons.io.IOUtils;
import org.junit.Test;

import com.temenos.interaction.core.entity.Metadata;
import com.temenos.interaction.core.entity.vocabulary.TermFactory;
import com.temenos.interaction.core.hypermedia.ResourceStateMachine;

public class TestResourceMetadataManager {

	@Test
	public void testConstructor() throws Exception {
		InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream("AirlinesMetadata.xml");
		StringWriter writer = new StringWriter();
		IOUtils.copy(inputStream, writer, "UTF-8");
		String xml = writer.toString();
		ResourceStateMachine stateMachine = mock(ResourceStateMachine.class);
		ResourceMetadataManager mdProducer = new ResourceMetadataManager(xml, stateMachine);
		Metadata metadata = mdProducer.getMetadata();
		assertNotNull(metadata);
	}
	
	@Test
	public void testCurrencyList() throws Exception {
		TermFactory termFactory  = new TermFactory();
		ResourceMetadataManager mdProducer = new ResourceMetadataManager(termFactory);
		Metadata metadata = mdProducer.getMetadata("CountryList");
		assertNotNull(metadata);
	}

	@Test
	public void testCustomerInfo() throws Exception {
		TermFactory termFactory  = new TermFactory();
		ResourceMetadataManager mdProducer = new ResourceMetadataManager(termFactory);
		Metadata metadata = mdProducer.getMetadata("CustomerInfo");
		assertNotNull(metadata);
	}
}
