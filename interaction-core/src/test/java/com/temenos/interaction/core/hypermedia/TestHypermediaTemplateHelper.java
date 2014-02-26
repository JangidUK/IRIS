package com.temenos.interaction.core.hypermedia;

/*
 * #%L
 * interaction-core
 * %%
 * Copyright (C) 2012 - 2014 Temenos Holdings N.V.
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

import org.junit.Test;

public class TestHypermediaTemplateHelper {

	@Test
	public void testGetBaseUri() {
		assertEquals("http://localhost:8080/responder/rest/", 
				HypermediaTemplateHelper.getTemplatedBaseUri("http://localhost:8080/responder/rest", "http://localhost:8080/responder/rest/"));
	}

	@Test
	public void testGetBaseUriSimple() {
		assertEquals("http://localhost:8080/responder/rest/", 
				HypermediaTemplateHelper.getTemplatedBaseUri("http://localhost:8080/responder/rest", "http://localhost:8080/responder/rest/test"));
	}

	@Test
	public void testGetTemplatedBaseUri() {
		assertEquals("http://localhost:8080/responder/rest/MockCompany001/", 
				HypermediaTemplateHelper.getTemplatedBaseUri("http://localhost:8080/responder/rest/{companyid}", "http://localhost:8080/responder/rest/MockCompany001/"));
	}

	@Test
	public void testGetTemplatedBaseUriSimple() {
		assertEquals("http://localhost:8080/responder/rest/MockCompany001/", 
				HypermediaTemplateHelper.getTemplatedBaseUri("http://localhost:8080/responder/rest/{companyid}", "http://localhost:8080/responder/rest/MockCompany001/test"));
	}

	@Test
	public void testGetTemplatedBaseUriNested() {
		assertEquals("http://localhost:8080/responder/rest/MockCompany001/", 
				HypermediaTemplateHelper.getTemplatedBaseUri("http://localhost:8080/responder/rest/{companyid}", "http://localhost:8080/responder/rest/MockCompany001/test/blah"));
	}

	@Test
	public void testGetTemplatedBaseUriQueryParameters() {
		assertEquals("http://localhost:8080/responder/rest/MockCompany001/", 
				HypermediaTemplateHelper.getTemplatedBaseUri("http://localhost:8080/responder/rest/{companyid}", "http://localhost:8080/responder/rest/MockCompany001/test?blah=123"));
	}

	@Test
	public void testGetTemplatedBaseUriLookBehind() {
		// 
		assertEquals("http://127.0.0.1:9081/hothouse-iris/Hothouse.svc/GB0010001/", 
				HypermediaTemplateHelper.getTemplatedBaseUri("http://127.0.0.1:9081/hothouse-iris/Hothouse.svc/{companyid}/", "http://127.0.0.1:9081/hothouse-iris/Hothouse.svc/GB0010001/root"));
	}
}
