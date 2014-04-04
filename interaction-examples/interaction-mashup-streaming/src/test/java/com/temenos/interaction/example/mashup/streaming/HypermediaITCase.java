package com.temenos.interaction.example.mashup.streaming;

/*
 * #%L
 * interaction-example-mashup-streaming
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
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;

import javax.ws.rs.core.Response;

import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.UniformInterfaceException;
import com.sun.jersey.test.framework.JerseyTest;
import com.temenos.interaction.media.hal.MediaType;
import com.theoryinpractise.halbuilder.api.Link;
import com.theoryinpractise.halbuilder.api.ReadableRepresentation;
import com.theoryinpractise.halbuilder.api.RepresentationFactory;
import com.theoryinpractise.halbuilder.standard.StandardRepresentationFactory;

/**
 * This test ensures that we can navigate from one application state
 * to another using hypermedia (links).
 * 
 * @author aphethean
 */
public class HypermediaITCase extends JerseyTest {

	public HypermediaITCase() throws Exception {
		super();
	}
	
	@Before
	public void initTest() {
		// -DTEST_ENDPOINT_URI={someurl} to test with external server 
    	webResource = Client.create().resource(ConfigurationHelper.getTestEndpointUri(Configuration.TEST_ENDPOINT_URI)); 
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
		assertEquals(2, links.size());
		for (Link link : links) {
			if (link.getRel().equals("self")) {
				assertEquals(Configuration.TEST_ENDPOINT_URI + "/", link.getHref());
			} else if (link.getName().equals("HOME.home>GET>Profile.profile")) {
				assertEquals(Configuration.TEST_ENDPOINT_URI + "/profile", link.getHref());
			} else {
				fail("unexpected link [" + link.getName() + "]");
			}
		}
	}
	
	@Test
	public void testFollowRelsToProfileImage() throws IOException, UniformInterfaceException, URISyntaxException {
		RepresentationFactory representationFactory = new StandardRepresentationFactory();

		ClientResponse response = webResource.path("/").accept(MediaType.APPLICATION_HAL_JSON).get(ClientResponse.class);
        assertEquals(Response.Status.Family.SUCCESSFUL, Response.Status.fromStatusCode(response.getStatus()).getFamily());
		ReadableRepresentation homeResource = representationFactory.readRepresentation(new InputStreamReader(response.getEntityInputStream()));
		Link profileLink = homeResource.getLinkByRel("http://relations.rimdsl.org/profile");
		assertNotNull(profileLink);
		response.close();

		ClientResponse profileResponse = webResource.uri(new URI(profileLink.getHref())).accept(MediaType.APPLICATION_HAL_JSON).get(ClientResponse.class);
        assertEquals(Response.Status.Family.SUCCESSFUL, Response.Status.fromStatusCode(profileResponse.getStatus()).getFamily());
		ReadableRepresentation profileResource = representationFactory.readRepresentation(new InputStreamReader(profileResponse.getEntityInputStream()));
		assertEquals("someone@somewhere.com", profileResource.getProperties().get("email"));
		Link profileImageLink = profileResource.getLinkByRel("http://relations.rimdsl.org/image");
		assertNotNull(profileImageLink);
		profileResponse.close();

		ClientResponse profileImageResponse = webResource.uri(new URI(profileImageLink.getHref())).get(ClientResponse.class);
        assertEquals(Response.Status.Family.SUCCESSFUL, Response.Status.fromStatusCode(profileImageResponse.getStatus()).getFamily());
        InputStream imageStream = profileImageResponse.getEntityInputStream();
        FileOutputStream fos = new FileOutputStream("./testimg.jpg");
		try {
			IOUtils.copy(imageStream, fos);
		} finally {
			IOUtils.closeQuietly(imageStream);
		}				
        fos.flush();
        fos.close();
		
        // check image received correctly
        assertEquals("image/jpeg", profileImageResponse.getHeaders().getFirst("Content-Type"));
        long originalFileSize;
        InputStream stream = null;
        try {
            URL url = this.getClass().getResource("/testimg.jpg");
            stream = url.openStream();
            originalFileSize = stream.available();
        } finally {
        	IOUtils.closeQuietly(stream);
        }
        File receivedImage = new File("./testimg.jpg");
        assertEquals(originalFileSize, receivedImage.length());
        profileImageResponse.close();
	}

}
