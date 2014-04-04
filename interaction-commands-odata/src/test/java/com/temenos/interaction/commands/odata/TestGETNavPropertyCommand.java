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


import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedMap;

import org.junit.Test;
import org.odata4j.core.OEntity;
import org.odata4j.core.OEntityKey;
import org.odata4j.edm.EdmDataServices;
import org.odata4j.edm.EdmEntitySet;
import org.odata4j.edm.EdmEntityType;
import org.odata4j.edm.EdmProperty;
import org.odata4j.edm.EdmType;
import org.odata4j.producer.BaseResponse;
import org.odata4j.producer.EntityQueryInfo;
import org.odata4j.producer.EntityResponse;
import org.odata4j.producer.ODataProducer;
import org.odata4j.producer.QueryInfo;
import org.odata4j.producer.Responses;

import com.temenos.interaction.core.MultivaluedMapImpl;
import com.temenos.interaction.core.command.InteractionCommand;
import com.temenos.interaction.core.command.InteractionContext;
import com.temenos.interaction.core.command.InteractionException;
import com.temenos.interaction.core.entity.Metadata;
import com.temenos.interaction.core.hypermedia.Action;
import com.temenos.interaction.core.hypermedia.ResourceState;

public class TestGETNavPropertyCommand {

	class MyEdmType extends EdmType {
		public MyEdmType(String name) {
			super(name);
		}
		public boolean isSimple() { return false; }
	}

	@Test(expected = AssertionError.class)
	public void testEntitySetNotFound() throws InteractionException {
		ODataProducer mockProducer = createMockODataProducer("MyEntity", "Edm.String");
		GETNavPropertyCommand gec = new GETNavPropertyCommand(mockProducer);
		
		// test assertion for entity set not found
        InteractionContext ctx = createInteractionContext("DOESNOTMATCH", "1");
		InteractionCommand.Result result = gec.execute(ctx);
		assertEquals(InteractionCommand.Result.SUCCESS, result);
	}

	@Test(expected = InteractionException.class)
	public void testActionConfigurationNotFound() throws InteractionException {
		ODataProducer mockProducer = createMockODataProducer("MyEntity", "Edm.String");
		GETNavPropertyCommand gec = new GETNavPropertyCommand(mockProducer);
		
		// test IllegalArgumentException when entity not found
		ResourceState resourceState = mock(ResourceState.class);
		when(resourceState.getViewAction()).thenReturn(mock(Action.class));
        InteractionContext ctx = mock(InteractionContext.class);
        when(ctx.getCurrentState()).thenReturn(resourceState);
		gec.execute(ctx);
	}

	@Test
	public void testInvalidKeyType() throws InteractionException {
		ODataProducer mockProducer = createMockODataProducer("MyEntity", "KeyType");
		GETNavPropertyCommand command = new GETNavPropertyCommand(mockProducer);
		InteractionCommand.Result result = command.execute(createInteractionContext("MyEntity", "1"));
		assertEquals(InteractionCommand.Result.FAILURE, result);
	}

	@Test
	public void testNullQueryParams() throws InteractionException {
		GETNavPropertyCommand command = new GETNavPropertyCommand(createMockODataProducer("MyEntity", "Edm.String"));
		InteractionCommand.Result result = command.execute(createInteractionContext("MyEntity", "1"));
		assertEquals(InteractionCommand.Result.FAILURE, result);
	}

	@Test
	public void testNullNavResponse() throws InteractionException {
		ODataProducer mockProducer = createMockODataProducer("MyEntity", "Edm.String");
		// mock return of an unsupported BaseResponse
		when(mockProducer.getNavProperty(anyString(), any(OEntityKey.class), eq("navProperty"), any(QueryInfo.class)))
			.thenReturn(new BaseResponse() {});
		GETNavPropertyCommand command = new GETNavPropertyCommand(mockProducer);
		InteractionCommand.Result result = command.execute(createInteractionContext("MyEntity", "1"));
		assertEquals(InteractionCommand.Result.FAILURE, result);
	}

	@Test
	public void testUnsupportedNavResponse() throws InteractionException {
		ODataProducer mockProducer = createMockODataProducer("MyEntity", "Edm.String");
		// mock return of an unsupported BaseResponse
		when(mockProducer.getNavProperty(anyString(), any(OEntityKey.class), eq("navProperty"), any(QueryInfo.class)))
			.thenReturn(new BaseResponse() {});
		GETNavPropertyCommand command = new GETNavPropertyCommand(mockProducer);
		InteractionCommand.Result result = command.execute(createInteractionContext("MyEntity", "1"));
		assertEquals(InteractionCommand.Result.FAILURE, result);
	}

	@Test
	public void testPropertyNavResponse() throws InteractionException {
		ODataProducer mockProducer = createMockODataProducer("MyEntity", "Edm.String");
		// mock return of an unsupported BaseResponse
		when(mockProducer.getNavProperty(anyString(), any(OEntityKey.class), eq("navProperty"), any(QueryInfo.class)))
			.thenReturn(Responses.property(null));
		GETNavPropertyCommand command = new GETNavPropertyCommand(mockProducer);
		InteractionCommand.Result result = command.execute(createInteractionContext("MyEntity", "1"));
		assertEquals(InteractionCommand.Result.FAILURE, result);
	}

	private ODataProducer createMockODataProducer(String entityName, String keyTypeName) {
		ODataProducer mockProducer = mock(ODataProducer.class);
		List<String> keys = new ArrayList<String>();
		keys.add("MyId");
		List<EdmProperty.Builder> properties = new ArrayList<EdmProperty.Builder>();
		EdmProperty.Builder ep = EdmProperty.newBuilder("MyId").setType(new MyEdmType(keyTypeName));
		properties.add(ep);
		EdmEntityType.Builder eet = EdmEntityType.newBuilder().setNamespace("MyNamespace").setAlias("MyAlias").setName(entityName).addKeys(keys).addProperties(properties);
		EdmEntitySet.Builder ees = EdmEntitySet.newBuilder().setName(entityName).setEntityType(eet);

		List<EdmEntityType> mockEntityTypes = new ArrayList<EdmEntityType>();
		mockEntityTypes.add(eet.build());

		EdmDataServices mockEDS = mock(EdmDataServices.class);
		when(mockEDS.getEdmEntitySet(anyString())).thenReturn(ees.build());
		when(mockEDS.getEntityTypes()).thenReturn(mockEntityTypes);
		when(mockProducer.getMetadata()).thenReturn(mockEDS);

		EntityResponse mockEntityResponse = mock(EntityResponse.class);
		when(mockEntityResponse.getEntity()).thenReturn(mock(OEntity.class));
		when(mockProducer.getEntity(anyString(), any(OEntityKey.class), any(EntityQueryInfo.class))).thenReturn(mockEntityResponse);
				        
        return mockProducer;
	}
	
	@SuppressWarnings("unchecked")
	private InteractionContext createInteractionContext(String entity, String id) {
		ResourceState resourceState = mock(ResourceState.class);
		when(resourceState.getEntityName()).thenReturn(entity);
		when(resourceState.getUriSpecification()).thenReturn(new ODataUriSpecification().getTemplate("/" + entity, ODataUriSpecification.NAVPROPERTY_URI_TYPE));
		MultivaluedMap<String, String> pathParams = new MultivaluedMapImpl<String>();
		pathParams.add("id", id);
		// action configuration is required to make the NavPropertyCommand work
		Properties properties = new Properties();
		properties.put("entity", entity);
		properties.put("navproperty", "mock");
		when(resourceState.getViewAction()).thenReturn(new Action("NavPropertyCommand", Action.TYPE.VIEW, properties));
		
        InteractionContext ctx = new InteractionContext(mock(HttpHeaders.class), pathParams, mock(MultivaluedMap.class), resourceState, mock(Metadata.class));
        return ctx;
	}

}
