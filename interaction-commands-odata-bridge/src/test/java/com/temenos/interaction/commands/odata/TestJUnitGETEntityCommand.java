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
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedMap;

import org.joda.time.LocalDateTime;
import org.joda.time.LocalTime;
import org.junit.Test;
import org.mockito.ArgumentMatcher;
import org.odata4j.consumer.ODataConsumer;
import org.odata4j.consumer.ODataConsumerAdapter;
import org.odata4j.core.OEntity;
import org.odata4j.core.OEntityKey;
import org.odata4j.core.OEntityKey.KeyType;
import org.odata4j.edm.EdmDataServices;
import org.odata4j.edm.EdmEntitySet;
import org.odata4j.edm.EdmEntityType;
import org.odata4j.edm.EdmProperty;
import org.odata4j.edm.EdmType;
import org.odata4j.producer.EntityQueryInfo;
import org.odata4j.producer.EntityResponse;
import org.odata4j.producer.ODataProducer;

import com.temenos.interaction.commands.odata.consumer.GETEntityCommand;
import com.temenos.interaction.core.MultivaluedMapImpl;
import com.temenos.interaction.core.command.InteractionCommand;
import com.temenos.interaction.core.command.InteractionContext;
import com.temenos.interaction.core.entity.Metadata;
import com.temenos.interaction.core.hypermedia.ResourceState;
import com.temenos.interaction.core.resource.EntityResource;

public class TestJUnitGETEntityCommand {

	class MyEdmType extends EdmType {
		public MyEdmType(String name) {
			super(name);
		}
		public boolean isSimple() { return false; }
	}

	@Test(expected = AssertionError.class)
	public void testEntitySetNotFound() {
		ODataProducer mockProducer = createMockODataProducer("MyEntity", "Edm.String");
		ODataConsumer mockConsumer = new ODataConsumerAdapter(mockProducer);
		GETEntityCommand gec = new GETEntityCommand(mockConsumer);
		
		// test assertion for entity set not found
        InteractionContext ctx = createInteractionContext("DOESNOTMATCH", "1");
		InteractionCommand.Result result = gec.execute(ctx);
		assertEquals(InteractionCommand.Result.SUCCESS, result);
	}

	@Test
	public void testEntityKeyTypeString() {
		ODataProducer mockProducer = createMockODataProducer("MyEntity", "Edm.String");
		ODataConsumer mockConsumer = new ODataConsumerAdapter(mockProducer);
		GETEntityCommand gec = new GETEntityCommand(mockConsumer);
		
		// test our method
        InteractionContext ctx = createInteractionContext("MyEntity", "abc");
		InteractionCommand.Result result = gec.execute(ctx);
		assertEquals(InteractionCommand.Result.SUCCESS, result);
		assertTrue(ctx.getResource() instanceof EntityResource);
		// check the key was constructed correctly
		class StringOEntityKey extends ArgumentMatcher<OEntityKey> {
		      public boolean matches(Object obj) {
		    	  OEntityKey ek = (OEntityKey) obj;
		    	  assertNotNull(ek);
		    	  assertEquals(KeyType.SINGLE, ek.getKeyType());
		    	  assertEquals("abc", (String) ek.asSingleValue());
		          return true;
		      }
		   }
		verify(mockProducer).getEntity(eq("MyEntity"), argThat(new StringOEntityKey()), any(EntityQueryInfo.class));
	}

	@Test
	public void testEntityKeyTypeStringQuoted() {
		ODataProducer mockProducer = createMockODataProducer("MyEntity", "Edm.String");
		ODataConsumer mockConsumer = new ODataConsumerAdapter(mockProducer);
		GETEntityCommand gec = new GETEntityCommand(mockConsumer);
		
		// test our method
        InteractionContext ctx = createInteractionContext("MyEntity", "'abc'");
		InteractionCommand.Result result = gec.execute(ctx);
		assertEquals(InteractionCommand.Result.SUCCESS, result);
		assertTrue(ctx.getResource() instanceof EntityResource);
		// check the key was constructed correctly
		class StringOEntityKey extends ArgumentMatcher<OEntityKey> {
		      public boolean matches(Object obj) {
		    	  OEntityKey ek = (OEntityKey) obj;
		    	  assertNotNull(ek);
		    	  assertEquals(KeyType.SINGLE, ek.getKeyType());
		    	  assertEquals("abc", (String) ek.asSingleValue());
		          return true;
		      }
		   }
		verify(mockProducer).getEntity(eq("MyEntity"), argThat(new StringOEntityKey()), any(EntityQueryInfo.class));
	}

	@Test
	public void testEntityKeyTypeInt32() {
		ODataProducer mockProducer = createMockODataProducer("MyEntity", "Edm.Int32");
		ODataConsumer mockConsumer = new ODataConsumerAdapter(mockProducer);
		GETEntityCommand gec = new GETEntityCommand(mockConsumer);
		
		// test our method
        InteractionContext ctx = createInteractionContext("MyEntity", "1");
		InteractionCommand.Result result = gec.execute(ctx);
		assertEquals(InteractionCommand.Result.SUCCESS, result);
		assertTrue(ctx.getResource() instanceof EntityResource);
		// check the key was constructed correctly
		class StringOEntityKey extends ArgumentMatcher<OEntityKey> {
		      public boolean matches(Object obj) {
		    	  OEntityKey ek = (OEntityKey) obj;
		    	  assertNotNull(ek);
		    	  assertEquals(KeyType.SINGLE, ek.getKeyType());
		    	  assertEquals(new Integer(1), (Integer) ek.asSingleValue());
		          return true;
		      }
		   }
		verify(mockProducer).getEntity(eq("MyEntity"), argThat(new StringOEntityKey()), any(EntityQueryInfo.class));
	}

	@Test
	public void testEntityKeyInt64() {
		ODataProducer mockProducer = createMockODataProducer("MyEntity", "Edm.Int64");
		ODataConsumer mockConsumer = new ODataConsumerAdapter(mockProducer);
		GETEntityCommand gec = new GETEntityCommand(mockConsumer);
		
		// test our method
        InteractionContext ctx = createInteractionContext("MyEntity", Long.toString(Long.MAX_VALUE));
		InteractionCommand.Result result = gec.execute(ctx);
		assertEquals(InteractionCommand.Result.SUCCESS, result);
		assertTrue(ctx.getResource() instanceof EntityResource);
		// check the key was constructed correctly
		class Int64OEntityKey extends ArgumentMatcher<OEntityKey> {
		      public boolean matches(Object obj) {
		    	  OEntityKey ek = (OEntityKey) obj;
		    	  assertNotNull(ek);
		    	  assertEquals(KeyType.SINGLE, ek.getKeyType());
		    	  assertEquals(new Long(Long.MAX_VALUE), (Long) ek.asSingleValue());
		          return true;
		      }
		   }
		verify(mockProducer).getEntity(eq("MyEntity"), argThat(new Int64OEntityKey()), any(EntityQueryInfo.class));
	}

	@Test
	public void testEntityKeyInt64Error() {
		ODataProducer mockProducer = createMockODataProducer("MyEntity", "Edm.Int64");
		ODataConsumer mockConsumer = new ODataConsumerAdapter(mockProducer);
		GETEntityCommand gec = new GETEntityCommand(mockConsumer);
		
		// test our method
        InteractionContext ctx = createInteractionContext("MyEntity", "A1");
		InteractionCommand.Result result = gec.execute(ctx);
		// command should return FAILURE
		assertEquals(InteractionCommand.Result.FAILURE, result);
		assertTrue(ctx.getResource() == null);
	}
	
	@Test
	public void testEntityKeyDateTime() {
		ODataProducer mockProducer = createMockODataProducer("MyEntity", "Edm.DateTime");
		ODataConsumer mockConsumer = new ODataConsumerAdapter(mockProducer);
		GETEntityCommand gec = new GETEntityCommand(mockConsumer);
		
		// test our method
        InteractionContext ctx = createInteractionContext("MyEntity", "datetime'2012-02-06T18:05:53'");
		InteractionCommand.Result result = gec.execute(ctx);
		assertEquals(InteractionCommand.Result.SUCCESS, result);
		assertTrue(ctx.getResource() instanceof EntityResource);
		// check the key was constructed correctly
		class TimestampOEntityKey extends ArgumentMatcher<OEntityKey> {
		      public boolean matches(Object obj) {
		    	  OEntityKey ek = (OEntityKey) obj;
		    	  assertNotNull(ek);
		    	  assertEquals(KeyType.SINGLE, ek.getKeyType());
		    	  assertEquals(new LocalDateTime(2012,2,6,18,5,53), (LocalDateTime) ek.asSingleValue());
		          return true;
		      }
		   }
		verify(mockProducer).getEntity(eq("MyEntity"), argThat(new TimestampOEntityKey()), any(EntityQueryInfo.class));
	}

	@Test
	public void testEntityKeyTime() {
		ODataProducer mockProducer = createMockODataProducer("MyEntity", "Edm.Time");
		ODataConsumer mockConsumer = new ODataConsumerAdapter(mockProducer);
		GETEntityCommand gec = new GETEntityCommand(mockConsumer);
		
		// test our method
        InteractionContext ctx = createInteractionContext("MyEntity", "time'PT18H05M53S'");
		InteractionCommand.Result result = gec.execute(ctx);
		assertEquals(InteractionCommand.Result.SUCCESS, result);
		assertTrue(ctx.getResource() instanceof EntityResource);
		// check the key was constructed correctly
		class DateOEntityKey extends ArgumentMatcher<OEntityKey> {
		      public boolean matches(Object obj) {
		    	  OEntityKey ek = (OEntityKey) obj;
		    	  assertNotNull(ek);
		    	  assertEquals(KeyType.SINGLE, ek.getKeyType());
		    	  assertEquals(new LocalTime(18,5,53), (LocalTime) ek.asSingleValue());
		          return true;
		      }
		   }
		verify(mockProducer).getEntity(eq("MyEntity"), argThat(new DateOEntityKey()), any(EntityQueryInfo.class));
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
		MultivaluedMap<String, String> pathParams = new MultivaluedMapImpl<String>();
		pathParams.add("id", id);
        InteractionContext ctx = new InteractionContext(mock(HttpHeaders.class), pathParams, mock(MultivaluedMap.class), resourceState, mock(Metadata.class));
        return ctx;
	}

}
