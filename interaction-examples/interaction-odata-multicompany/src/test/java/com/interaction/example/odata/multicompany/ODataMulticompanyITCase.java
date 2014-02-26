package com.interaction.example.odata.multicompany;

/*
 * #%L
 * interaction-example-odata-multicompany
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

import java.io.IOException;
import java.io.StringReader;
import java.util.List;

import org.apache.abdera.Abdera;
import org.apache.abdera.model.Document;
import org.apache.abdera.parser.Parser;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.GetMethod;
import org.core4j.Enumerable;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.odata4j.consumer.ODataConsumer;
import org.odata4j.core.OEntity;
import org.odata4j.core.OLink;
import org.odata4j.jersey.consumer.ODataJerseyConsumer;

/**
 * This test ensures that this projects OData entities have working 
 * OData associations (links).
 * 
 * @author aphethean
 */
public class ODataMulticompanyITCase {

	private final static String FLIGHT_ENTITYSET_NAME = "Flights";
	private final static String FLIGHT_SCHEDULE_ENTITYSET_NAME = "FlightSchedules";
	private final static String AIRPORT_ENTITYSET_NAME = "Airports";
	
	private String baseUri = null;
	private HttpClient client;

	public ODataMulticompanyITCase() throws Exception {
		super();
	}
	
	@Before
	public void setup() {
		baseUri = ConfigurationHelper.getTestEndpointUri(Configuration.TEST_ENDPOINT_URI);
		client = new HttpClient();
	}

	@After
	public void tearDown() {}

	/**
	 * GET item, check id of entity
	 */
	@Test
	public void getFlightCheckId() throws Exception {
		org.apache.abdera.model.Entry entry = null;
    	GetMethod method = new GetMethod(baseUri + "Flights(2)");
		try {
			client.executeMethod(method);
			assertEquals(200, method.getStatusCode());

			if (method.getStatusCode() == HttpStatus.SC_OK) {
				// read as string for debugging
				String response = method.getResponseBodyAsString();
				System.out.println("Response = " + response);

				Abdera abdera = new Abdera();
				Parser parser = abdera.getParser();
				Document<org.apache.abdera.model.Entry> doc = parser.parse(new StringReader(response));
				entry = doc.getRoot();
			}
		} catch (IOException e) {
			fail(e.getMessage());
		} finally {
			method.releaseConnection();
		}

		assertNotNull(entry);
		assertEquals("http://localhost:8080/example/interaction-odata-multicompany.svc/MockCompany001/Flights(2)", entry.getId().toString());
	}

	/**
	 * GET collection, check link to self and other entities for each item
	 */
	@Test
	public void getFlightsLinksToFlightSchedule() throws Exception {
		ODataConsumer consumer = ODataJerseyConsumer.newBuilder(baseUri).build();

		Enumerable<OEntity> flights = consumer.getEntities(FLIGHT_ENTITYSET_NAME).execute();
		assertEquals(4, flights.toSet().size());
		for (OEntity flight : flights.toSet()) {
			Long flightID = (Long) flight.getProperty("flightID").getValue();
			Long flightScheduleNum = (Long) flight.getProperty("flightScheduleNum").getValue();

			assertEquals(1, flight.getLinks().size());
			// there should be one link to one flight schedule for this flight
			assertTrue(containsLink(flight.getLinks(), FLIGHT_ENTITYSET_NAME + "(" + flightID + ")/flightSchedule") ||
					containsLink(flight.getLinks(), FLIGHT_SCHEDULE_ENTITYSET_NAME + "(" + flightScheduleNum + ")"));
		}
	}

	/**
	 * GET item, check link to another entity
	 */
	@Test
	public void getFlightLinksToFlightSchedule() throws Exception {
		ODataConsumer consumer = ODataJerseyConsumer.newBuilder(baseUri).build();

		OEntity flight = consumer.getEntity(FLIGHT_ENTITYSET_NAME, 2).execute();
		Long flightID = (Long) flight.getProperty("flightID").getValue();
		assertEquals(2, flightID.intValue());
		Long flightScheduleNum = (Long) flight.getProperty("flightScheduleNum").getValue();

		// there should be one link to one flight schedule for this flight
		assertEquals(1, flight.getLinks().size());
		assertTrue(containsLink(flight.getLinks(), FLIGHT_ENTITYSET_NAME + "(" + flightID + ")/flightSchedule") ||
				containsLink(flight.getLinks(), FLIGHT_SCHEDULE_ENTITYSET_NAME + "(" + flightScheduleNum + ")"));
	}

	/**
	 * GET collection, check link to self and other entities for each item
	 */
	@Test
	public void getFlightSchedulesLinksToFlightSchedule() throws Exception {
		ODataConsumer consumer = ODataJerseyConsumer.newBuilder(baseUri).build();

		Enumerable<OEntity> flightSchedules = consumer.getEntities(FLIGHT_SCHEDULE_ENTITYSET_NAME).execute();
		// if the test is run multiple times it will be greater than 7
		assertTrue(flightSchedules.toSet().size() >= 7);
		for (OEntity flightSchedule : flightSchedules.toSet()) {
			Long id = (Long) flightSchedule.getProperty("flightScheduleID").getValue();

			// there should be one link to self (OData4j swallows this)
			// there should be one link to flights
			// there should be one link to one departureAirport for this flight schedule
			// there should be one link to one departureAirport for this flight schedule
			assertTrue(containsLink(flightSchedule.getLinks(), "FlightSchedules(" + id + ")/flights", "http://schemas.microsoft.com/ado/2007/08/dataservices/related/Flights"));
			assertTrue(containsLink(flightSchedule.getLinks(), FLIGHT_SCHEDULE_ENTITYSET_NAME + "(" + id + ")/departureAirport", "http://schemas.microsoft.com/ado/2007/08/dataservices/related/Airport"));
			assertTrue(containsLink(flightSchedule.getLinks(), FLIGHT_SCHEDULE_ENTITYSET_NAME + "(" + id + ")/arrivalAirport", "http://schemas.microsoft.com/ado/2007/08/dataservices/related/Airport"));
			assertEquals(3, flightSchedule.getLinks().size());
		}
	}

	/**
	 * GET item, check link to another entity
	 */
	@Test
	public void getFlightScheduleLinksToAirports() throws Exception {
		ODataConsumer consumer = ODataJerseyConsumer.newBuilder(baseUri).build();

		OEntity flightSchedule = consumer.getEntity(FLIGHT_SCHEDULE_ENTITYSET_NAME, 2).execute();
		Long id = (Long) flightSchedule.getProperty("flightScheduleID").getValue();
		assertEquals(2, id.intValue());

		// there should be one link to self (OData4j swallows this)
		// there should be one link to flights
		// there should be one link to one departureAirport for this flight schedule
		// there should be one link to one arrivalAirport for this flight schedule
		assertTrue(containsLink(flightSchedule.getLinks(), FLIGHT_SCHEDULE_ENTITYSET_NAME + "(" + id + ")/departureAirport", "http://schemas.microsoft.com/ado/2007/08/dataservices/related/Airport"));
		assertTrue(containsLink(flightSchedule.getLinks(), FLIGHT_SCHEDULE_ENTITYSET_NAME + "(" + id + ")/arrivalAirport", "http://schemas.microsoft.com/ado/2007/08/dataservices/related/Airport"));
		assertEquals(3, flightSchedule.getLinks().size());
	}

	/**
	 * GET nav properties for an item
	 */
	@Test
	public void getFlightScheduleNavProperties() throws Exception {
		ODataConsumer consumer = ODataJerseyConsumer.newBuilder(baseUri).build();

		// now follow links to departure airport and arrival airport
		//do this only for strict odata
		OEntity departureAirport = consumer
				.getEntity(FLIGHT_SCHEDULE_ENTITYSET_NAME, 2055)
				.nav("departureAirport")
				.execute();
		assertEquals("LTN", departureAirport.getProperty("code").getValue());
		OEntity arrivalAirport = consumer
				.getEntity(FLIGHT_SCHEDULE_ENTITYSET_NAME, 2055)
				.nav("arrivalAirport")
				.execute();
		assertEquals("GVA", arrivalAirport.getProperty("code").getValue());
	}
	
	/**
	 * GET collection, check link to self and other entities for each item
	 */
	@Test
	public void getAirportsLinksToAirport() throws Exception {
		ODataConsumer consumer = ODataJerseyConsumer.newBuilder(baseUri).build();

		Enumerable<OEntity> airports = consumer.getEntities(AIRPORT_ENTITYSET_NAME).execute();
		assertEquals(6, airports.toSet().size());
		for (OEntity airport : airports.toSet()) {
			String code = airport.getProperty("code").getValue().toString();

			assertEquals(2, airport.getLinks().size());
			// there should be one link to departures
			assertTrue(containsLink(airport.getLinks(), AIRPORT_ENTITYSET_NAME + "('" + code + "')/departures"));
			assertTrue(containsLink(airport.getLinks(), AIRPORT_ENTITYSET_NAME + "('" + code + "')/arrivals"));
		}
	}

	/**
	 * GET item, check link to another entity
	 */
	@Test
	public void getAirportLinksToArrivalsDepartures() throws Exception {
		ODataConsumer consumer = ODataJerseyConsumer.newBuilder(baseUri).build();

		OEntity airport = consumer.getEntity(AIRPORT_ENTITYSET_NAME, "LTN").execute();
		String code = airport.getProperty("code").getValue().toString();
		assertEquals("LTN", code);

		assertEquals(2, airport.getLinks().size());
		assertTrue(containsLink(airport.getLinks(), AIRPORT_ENTITYSET_NAME + "('" + code + "')/departures"));
		assertTrue(containsLink(airport.getLinks(), AIRPORT_ENTITYSET_NAME + "('" + code + "')/arrivals"));
	}

	/**
	 * GET nav properties for an item
	 */
	@Test
	public void getAirportNavProperties() throws Exception {
		ODataConsumer consumer = ODataJerseyConsumer.newBuilder(baseUri).build();

		// now follow links to departure airport and arrival airport
		OEntity departuresFlightSchedule = consumer
				.getEntity(AIRPORT_ENTITYSET_NAME, "LTN")
				.nav("departures")
				.execute();
		assertEquals(2051L, departuresFlightSchedule.getProperty("flightScheduleID").getValue());
		OEntity arrivalsFlightSchedule = consumer
				.getEntity(AIRPORT_ENTITYSET_NAME, "LTN")
				.nav("arrivals")
				.execute();
		assertEquals(2052L, arrivalsFlightSchedule.getProperty("flightScheduleID").getValue());
	}
	
	private boolean containsLink(List<OLink> links, String link) {
		return containsLink(links, link, null);
	}
	
	private boolean containsLink(List<OLink> links, String link, String relation) {
		assert(links != null);
		for (OLink l : links) {
			if (l.getHref().equals(link) && (relation == null || l.getRelation().equals(relation))) {
				return true;
			}
		}
		//Link not found => print debug info
		System.out.println("Links with rel [" + relation + "] and href [" + link + "] does not exist:");
		for(OLink l : links) {
			System.out.println("   Link: rel [" + l.getRelation() + "], href [" + l.getHref() + "]");
		}
		return false;
	}
}
