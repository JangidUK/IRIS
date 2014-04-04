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


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;

import java.util.ArrayList;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedMap;

import org.apache.wink.common.internal.MultivaluedMapImpl;
import org.junit.Test;

import com.temenos.interaction.core.entity.EntityMetadata;
import com.temenos.interaction.core.entity.Metadata;
import com.temenos.interaction.core.entity.vocabulary.Vocabulary;
import com.temenos.interaction.core.entity.vocabulary.terms.TermComplexType;
import com.temenos.interaction.core.entity.vocabulary.terms.TermIdField;
import com.temenos.interaction.core.hypermedia.Action;
import com.temenos.interaction.core.hypermedia.ResourceState;

public class TestInteractionContext {

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Test
	public void testResolveIdDefault() {
		MultivaluedMap<String, String> pathParameters = new MultivaluedMapImpl();
		pathParameters.add("id", "123");
		InteractionContext ctx = new InteractionContext(mock(HttpHeaders.class), pathParameters, new MultivaluedMapImpl(), new ResourceState("entity", "initial_state", new ArrayList<Action>(), "/resource"), mock(Metadata.class));
		assertEquals("123", ctx.getId());
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Test
	public void testResolveIdNoDefaultNull() {
		MultivaluedMap<String, String> pathParameters = new MultivaluedMapImpl();
		pathParameters.add("test", "123");
		InteractionContext ctx = new InteractionContext(mock(HttpHeaders.class), pathParameters, new MultivaluedMapImpl(), new ResourceState("entity", "initial_state", new ArrayList<Action>(), "/resource"), mock(Metadata.class));
		assertNull(ctx.getId());
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Test
	public void testResolveIdParameter() {
		MultivaluedMap<String, String> pathParameters = new MultivaluedMapImpl();
		pathParameters.add("TheTestParameterKey", "123");
		ResourceState state = new ResourceState("entity", "initial_state", new ArrayList<Action>(), "/resource", "TheTestParameterKey");
		InteractionContext ctx = new InteractionContext(mock(HttpHeaders.class), pathParameters, new MultivaluedMapImpl(), state, mock(Metadata.class));
		assertEquals("123", ctx.getId());
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Test
	public void testResolveSimpleIdFromMetadata() {
		// create metadata with an entity that declares a field 'myId' as its id
		Metadata metadata = new Metadata("SimpleModel");
		EntityMetadata vocs = new EntityMetadata("entity");
		Vocabulary vocName = new Vocabulary();
		vocName.setTerm(new TermComplexType(false));
		vocName.setTerm(new TermIdField(true));
		vocs.setPropertyVocabulary("myId", vocName);
		metadata.setEntityMetadata(vocs);
		
		MultivaluedMap<String, String> pathParameters = new MultivaluedMapImpl();
		pathParameters.add("myId", "123");
		InteractionContext ctx = new InteractionContext(mock(HttpHeaders.class), pathParameters, 
				new MultivaluedMapImpl(), 
				new ResourceState("entity", "initial_state", new ArrayList<Action>(), "/resource"),
				metadata);
		assertEquals("123", ctx.getId());
	}

	@Test
	public void testAttributes() {
		InteractionContext ctx = new InteractionContext(mock(HttpHeaders.class), new MultivaluedMapImpl<String, String>(), new MultivaluedMapImpl<String, String>(), new ResourceState("entity", "initial_state", new ArrayList<Action>(), "/resource"), mock(Metadata.class));
		ctx.setAttribute("abc", "test");
		
		assertEquals(ctx.getAttribute("abc"), "test");
	}
}
