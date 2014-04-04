package com.interaction.example.hateoas.banking;

/*
 * #%L
 * interaction-example-hateoas-banking
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
import static org.junit.Assert.fail;

import java.io.InputStreamReader;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.Response;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.test.framework.JerseyTest;
import com.temenos.interaction.media.hal.MediaType;
import com.theoryinpractise.halbuilder.api.Link;
import com.theoryinpractise.halbuilder.api.ReadableRepresentation;
import com.theoryinpractise.halbuilder.api.Representation;
import com.theoryinpractise.halbuilder.api.RepresentationFactory;
import com.theoryinpractise.halbuilder.standard.StandardRepresentationFactory;

public class HypermediaITCase extends JerseyTest {

	public HypermediaITCase() throws Exception {
		super();
	}
	
	@Before
	public void initTest() {
		// test with external server 
    	webResource = Client.create().resource(Configuration.TEST_ENDPOINT_URI); 
	}

	@After
	public void tearDown() {}


	@Test
	public void testGetEntryPointLinks() {
		ClientResponse response = webResource.path("/").accept(MediaType.APPLICATION_HAL_JSON).get(ClientResponse.class);
        assertEquals(Response.Status.Family.SUCCESSFUL, Response.Status.fromStatusCode(response.getStatus()).getFamily());

		RepresentationFactory representationFactory = new StandardRepresentationFactory();
		ReadableRepresentation resource = representationFactory.readRepresentation(new InputStreamReader(response.getEntityInputStream()));

		List<Link> links = resource.getLinks();
		assertEquals(5, links.size());
		for (Link link : links) {
			if (link.getRel().equals("self")) {
				assertEquals(Configuration.TEST_ENDPOINT_URI + "/", link.getHref());
			} else if (link.getName().equals("home.initial>GET>Preferences.preferences")) {
				assertEquals(Configuration.TEST_ENDPOINT_URI + "/preferences", link.getHref());
			} else if (link.getName().equals("home.initial>GET>FundsTransfer.fundstransfers")) {
				assertEquals(Configuration.TEST_ENDPOINT_URI + "/fundtransfers", link.getHref());
			} else if (link.getName().equals("home.initial>GET>Customer.customers")) {
				assertEquals(Configuration.TEST_ENDPOINT_URI + "/customers", link.getHref());
			} else if (link.getName().equals("home.initial>GET>home.ServiceDocument")) {
				assertEquals(Configuration.TEST_ENDPOINT_URI + "/Banking.svc", link.getHref());
			} else {
				fail("unexpected link [" + link.getName() + "]");
			}
		}
	}
	
	@Test
	public void testCollectionLinks() {
		ClientResponse response = webResource.path("/fundtransfers").accept(MediaType.APPLICATION_HAL_JSON).get(ClientResponse.class);
        assertEquals(Response.Status.Family.SUCCESSFUL, Response.Status.fromStatusCode(response.getStatus()).getFamily());

		RepresentationFactory representationFactory = new StandardRepresentationFactory();
		ReadableRepresentation resource = representationFactory.readRepresentation(new InputStreamReader(response.getEntityInputStream()));

		// the links from the collection
		List<Link> links = resource.getLinks();
		assertEquals(2, links.size());
		for (Link link : links) {
			if (link.getRel().equals("self")) {
				assertEquals(Configuration.TEST_ENDPOINT_URI + "/fundtransfers", link.getHref());
			} else if (link.getName().equals("FundsTransfer.fundstransfers>POST>FundsTransfer.new")) {
				assertEquals(Configuration.TEST_ENDPOINT_URI + "/fundtransfers/new", link.getHref());
			} else {
				fail("unexpected link [" + link.getName() + "]");
			}
		}
		
		// the links on each item
		Collection<Map.Entry<String, ReadableRepresentation>> subresources = resource.getResources();
		assertNotNull(subresources);
		for (Map.Entry<String, ReadableRepresentation> entry : subresources) {
			ReadableRepresentation item = entry.getValue();
			List<Link> itemLinks = item.getLinks();
			assertEquals(1, itemLinks.size());
			for (Link link : itemLinks) {
				if (link.getRel().contains("self")) {
					assertEquals(Configuration.TEST_ENDPOINT_URI + "/fundtransfers/" + item.getProperties().get("id"), link.getHref());
				} else {
					fail("unexpected link");
				}
			}
		}
	}

	/**
	 * Attempt a DELETE to the resource (a collection resource)
	 */
	@Test
	public void deleteFundTransferMethodNotAllowed() throws Exception {
		ClientResponse response = webResource.path("/fundtransfers").delete(ClientResponse.class);
        assertEquals(405, response.getStatus());

        assertEquals(3, response.getAllow().size());
        assertTrue(response.getAllow().contains("GET"));
        assertTrue(response.getAllow().contains("OPTIONS"));
        assertTrue(response.getAllow().contains("HEAD"));
	}

	/**
	 * Attempt a PUT to the resource (entity resource)
	 */
	@Test
	public void putFundTransferHalXML() throws Exception {
		double d = Math.random() * 10000000;
		String id = Integer.toString((int) d);
		String resourceUri = "/fundtransfers/" + id;
		String halRequest = buildHalResource(resourceUri, id).toString(RepresentationFactory.HAL_XML);

		ClientResponse response = webResource.path(resourceUri).accept(MediaType.APPLICATION_HAL_XML).type(MediaType.APPLICATION_HAL_XML).put(ClientResponse.class, halRequest);
        assertEquals(200, response.getStatus());

		response = webResource.path(resourceUri).accept(MediaType.APPLICATION_HAL_JSON).get(ClientResponse.class);
        assertEquals(Response.Status.Family.SUCCESSFUL, Response.Status.fromStatusCode(response.getStatus()).getFamily());
	}

	/**
	 * Attempt a PUT to the resource (entity resource)
	 */
	@Test
	public void putFundTransferHalJSON() throws Exception {
		double d = Math.random() * 10000000;
		String id = Integer.toString((int) d);
		String resourceUri = "/fundtransfers/" + id;
		String halRequest = buildHalResource(resourceUri, id).toString(RepresentationFactory.HAL_JSON);

		ClientResponse response = webResource.path(resourceUri).accept(MediaType.APPLICATION_HAL_JSON).type(MediaType.APPLICATION_HAL_JSON).put(ClientResponse.class, halRequest);
        assertEquals(200, response.getStatus());

		response = webResource.path("/fundtransfers/" + id).accept(MediaType.APPLICATION_HAL_JSON).get(ClientResponse.class);
        assertEquals(Response.Status.Family.SUCCESSFUL, Response.Status.fromStatusCode(response.getStatus()).getFamily());
	}
	
	private Representation buildHalResource(String resourceUri, String id) {
		RepresentationFactory representationFactory = new StandardRepresentationFactory();
		Representation r = representationFactory.newRepresentation(resourceUri);
		r.withProperty("id", id);
		r.withProperty("body", "Funds tranfer issued at 01/01/2012");
		return r;
	}

	/**
	 * Attempt a PUT to the collection resource (which only accepts post in this example)
	 */
	@Test
	public void putMethodNotAllowed() throws Exception {
		RepresentationFactory representationFactory = new StandardRepresentationFactory();
		ReadableRepresentation r = representationFactory.newRepresentation("/fundtransfers");
		String halRequest = r.toString(RepresentationFactory.HAL_XML);
		
		// attempt to put to the notes collection, rather than an individual
		ClientResponse response = webResource.path("/fundtransfers").type(MediaType.APPLICATION_HAL_XML).put(ClientResponse.class, halRequest);
        assertEquals(405, response.getStatus());

        assertEquals(3, response.getAllow().size());
        assertTrue(response.getAllow().contains("GET"));
        assertTrue(response.getAllow().contains("OPTIONS"));
        assertTrue(response.getAllow().contains("HEAD"));
	}

	/**
	 * Attempt to PUT an invalid resource representation.  The supplied self link is not correctly formed.
	 */
	@Test
	public void putInvalidResource() throws Exception {
		RepresentationFactory representationFactory = new StandardRepresentationFactory();
		ReadableRepresentation r = representationFactory.newRepresentation("~/xyz/123");
		String halRequest = r.toString(RepresentationFactory.HAL_XML);
		
		// attempt to put to the notes collection, rather than an individual
		ClientResponse response = webResource.path("/fundtransfers").type(MediaType.APPLICATION_HAL_XML).put(ClientResponse.class, halRequest);
        assertEquals(400, response.getStatus());
	}

}
