package com.temenos.interaction.commands.odata;

/*
 * #%L
 * interaction-commands-odata-bridge
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


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;

import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedMap;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.odata4j.consumer.ODataConsumer;
import org.odata4j.consumer.ODataConsumerAdapter;
import org.odata4j.core.OEntity;
import org.odata4j.core.OEntityKey;
import org.odata4j.edm.EdmDataServices;
import org.odata4j.edm.EdmEntitySet;
import org.odata4j.edm.EdmEntityType;
import org.odata4j.edm.EdmProperty;
import org.odata4j.edm.EdmSimpleType;
import org.odata4j.producer.EntityQueryInfo;
import org.odata4j.producer.EntityResponse;
import org.odata4j.producer.ODataProducer;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.temenos.interaction.commands.odata.consumer.GETEntityCommand;
import com.temenos.interaction.core.MultivaluedMapImpl;
import com.temenos.interaction.core.command.InteractionCommand;
import com.temenos.interaction.core.command.InteractionContext;
import com.temenos.interaction.core.entity.Metadata;
import com.temenos.interaction.core.hypermedia.ResourceState;
import com.temenos.interaction.core.resource.EntityResource;


@RunWith(PowerMockRunner.class)
@PrepareForTest({OEntityKey.class, GETEntityCommand.class})
public class TestPowermockGETEntityCommand {

	// test when exception from OEntityKey.parse then Response.Status.NOT_ACCEPTABLE
	@Test
	public void testOEntityKeyParseException() {
		// our test object
		GETEntityCommand gec = new GETEntityCommand(createMockODataConsumer("MyEntity"));

		// make parse pass ok
		mockStatic(OEntityKey.class);
        when(OEntityKey.parse(anyString())).thenThrow(new IllegalArgumentException());
        when(OEntityKey.create(anyString())).thenThrow(new IllegalArgumentException());

		// test our method
        InteractionContext ctx = createInteractionContext("MyEntity", "test");
        InteractionCommand.Result result = gec.execute(ctx);
		assertNotNull(result);
		assertEquals(InteractionCommand.Result.FAILURE, result);
		assertNull(ctx.getResource());
		
		// verify static calls
		verifyStatic();
		OEntityKey.parse("test");

	}
	
	@SuppressWarnings("unchecked")
	private InteractionContext createInteractionContext(String entity, String id) {
		ResourceState resourceState = mock(ResourceState.class);
		when(resourceState.getEntityName()).thenReturn(entity);
		MultivaluedMap<String, String> pathParams = new MultivaluedMapImpl<String>();
		pathParams.add("id", id);
        InteractionContext ctx = new InteractionContext(mock(HttpHeaders.class), pathParams, mock(MultivaluedMap.class), resourceState, mock(Metadata.class));
        return ctx;
	}
	
	// test when parse ok, then Response.Status.OK
	@Test
	public void testOEntityKeyParseSuccessful() {
		// our test object
		GETEntityCommand gec = new GETEntityCommand(createMockODataConsumer("MyEntity"));

		// make parse pass ok
		mockStatic(OEntityKey.class);
        when(OEntityKey.parse(anyString())).thenReturn(mock(OEntityKey.class));
        when(OEntityKey.create(anyString())).thenReturn(mock(OEntityKey.class));

		// test our method
        InteractionContext ctx = createInteractionContext("MyEntity", "test");
        InteractionCommand.Result result = gec.execute(ctx);
		assertNotNull(result);
		assertEquals(InteractionCommand.Result.SUCCESS, result);
		assertNotNull(ctx.getResource());
		assertTrue(ctx.getResource() instanceof EntityResource);
		
		// verify static calls
		verifyStatic();
		OEntityKey.parse("test");
	}
	
	private ODataConsumer createMockODataConsumer(String entityName) {
		return new ODataConsumerAdapter(createMockODataProducer(entityName));
	}
	
	private ODataProducer createMockODataProducer(String entityName) {
		ODataProducer mockProducer = mock(ODataProducer.class);
		List<String> keys = new ArrayList<String>();
		keys.add("MyId");
		List<EdmProperty.Builder> properties = new ArrayList<EdmProperty.Builder>();
		properties.add(EdmProperty.newBuilder("MyId").setType(EdmSimpleType.STRING));
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


}
