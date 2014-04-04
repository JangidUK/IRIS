package com.temenos.interaction.sdk.rimdsl;

/*
 * #%L
 * interaction-sdk
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


import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Map;

import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader;
import org.junit.Assert;
import org.junit.Test;

import com.google.common.io.CharStreams;
import com.temenos.interaction.core.entity.Metadata;
import com.temenos.interaction.core.entity.MetadataParser;
import com.temenos.interaction.sdk.JPAResponderGen;
import com.temenos.interaction.sdk.command.Commands;
import com.temenos.interaction.sdk.command.Parameter;
import com.temenos.interaction.sdk.interaction.state.IMEntityState;
import com.temenos.interaction.sdk.interaction.state.IMPseudoState;
import com.temenos.interaction.sdk.interaction.state.IMState;
import com.temenos.interaction.sdk.interaction.IMResourceStateMachine;
import com.temenos.interaction.sdk.interaction.InteractionModel;

/**
 * Unit test for {@link RimDslGenerator}.
 */
public class TestRimDslGenerator {
	private final static String RIM_LINE_SEP = System.getProperty("line.separator");

	public final static String METADATA_AIRLINE_XML_FILE = "AirlinesMetadata.xml";
	public final static String RIM_DSL_AIRLINE_SIMPLE_FILE = "AirlinesSimple.rim";
	public final static String RIM_DSL_AIRLINE_FILE = "Airlines.rim";
	public final static String RIM_DSL_AIRLINE_NON_STRICT_ODATA_FILE = "AirlinesNonStrictOData.rim";

	public final static String METADATA_BANKING_XML_FILE = "Banking.xml";
	public final static String RIM_DSL_BANKING_FILE = "Banking.rim";
	
	public final static Parameter COMMAND_SERVICE_DOCUMENT = new Parameter("ServiceDocument", false, "");
	public final static Parameter COMMAND_EDM_DATA_SERVICES = new Parameter("edmDataServices", true, "");
	public final static Parameter COMMAND_METADATA = new Parameter("Metadata", false, "");
	public final static Parameter COMMAND_METADATA_SOURCE_ODATAPRODUCER = new Parameter("producer", true, "odataProducer");
	public final static Parameter COMMAND_METADATA_SOURCE_MODEL = new Parameter("edmMetadata", true, "edmMetadata");

	@Test
	public void testGenerateRimDslAirlinesSimple() {
		//Define the basic interaction model based on the available metadata
		Metadata metadata = parseMetadata(METADATA_AIRLINE_XML_FILE);
		InteractionModel interactionModel = new InteractionModel(metadata);

		//Add transitions
		IMResourceStateMachine rsmFlightSchedule = interactionModel.findResourceStateMachine("FlightSchedule");
		IMResourceStateMachine rsmAirport = interactionModel.findResourceStateMachine("Airport");
		IMResourceStateMachine rsmFlight = interactionModel.findResourceStateMachine("Flight");
		IMResourceStateMachine rsmPassenger = interactionModel.findResourceStateMachine("Passenger");
		
		rsmFlightSchedule.addTransitionToEntityState("flightschedule", rsmAirport, "departureAirport", "departureAirportCode", null);
		rsmFlightSchedule.addTransitionToEntityState("flightschedule", rsmAirport, "arrivalAirport", "arrivalAirportCode", null);

		rsmFlightSchedule.addPseudoStateTransition("FlightSchedules", "created", "FlightSchedules", "POST", null, "CreateEntity", null, true);
		rsmFlight.addPseudoStateTransition("Flights", "created", "Flights", "POST", null, "CreateEntity", null, true);
		rsmAirport.addPseudoStateTransition("Airports", "created", "Airports", "POST", null, "CreateEntity", null, true);
		rsmPassenger.addPseudoStateTransition("Passengers", "created", "Passengers", "POST", null, "CreateEntity", null, true);
		
		//Run the generator
		RimDslGenerator generator = new RimDslGenerator(createVelocityEngine());
		String dsl = generator.generateRimDsl(interactionModel, JPAResponderGen.getDefaultCommands(), true);
		
		//Check results
		assertTrue(dsl != null && !dsl.equals(""));
		assertEquals(readTextFile(RIM_DSL_AIRLINE_SIMPLE_FILE), dsl);
	}
	
	@Test
	public void testGenerateRimDslAirlines() {
		InteractionModel interactionModel = createAirlineModelDSL(null);
		//Run the generator
		RimDslGenerator generator = new RimDslGenerator(createVelocityEngine());
		String dsl = generator.generateRimDsl(interactionModel, JPAResponderGen.getDefaultCommands(), true);

		//Check results
		assertTrue(dsl != null && !dsl.equals(""));
		assertEquals(readTextFile(RIM_DSL_AIRLINE_FILE), dsl);
	}

	@Test
	public void testGenerateRimDslMapAirlines() {
		InteractionModel interactionModel = createAirlineModelDSL(null);
		interactionModel.findResourceStateMachine("FlightSchedule").setRimName("Flight_Schedule");
		interactionModel.findResourceStateMachine("Passenger").setRimName("Passenger");

		//Run the generator
		RimDslGenerator generator = new RimDslGenerator(createVelocityEngine());
		// returns a map of specified resource state machine name and dsl
		Map<String,String> dslMap = generator.generateRimDslMap(interactionModel, JPAResponderGen.getDefaultCommands(), true);

		String rimDSL = null;
		// check results for ServiceDocument
		String root = interactionModel.getName();
		rimDSL = dslMap.get(root);
		assertTrue(rimDSL.contains("event GET"));
		assertTrue(rimDSL.contains("initial resource ServiceDocument"));
		assertTrue(rimDSL.contains("GET -> Flight_Schedule.FlightSchedules"));
		assertFalse(rimDSL.contains("resource FlightSchedules"));
		assertTrue(rimDSL.contains("GET -> Flights"));
		assertTrue(rimDSL.contains("GET -> Passenger.Passengers"));
		assertFalse(rimDSL.contains("resource Passengers"));
		assertTrue(rimDSL.contains("GET *-> flightschedule_departureAirport {"));
		
		// check results for FlightSchedules
		rimDSL = dslMap.get("Flight_Schedule");
		assertTrue(rimDSL.contains("rim Flight_Schedule {"));
		assertTrue(rimDSL.contains("event GET"));
		assertTrue(rimDSL.contains("resource FlightSchedules"));
		assertTrue(rimDSL.contains("GET *-> flightschedule {" + RIM_LINE_SEP
				+ "\t\tparameters [ id=\"{flightScheduleID}\" ]" + RIM_LINE_SEP
				+ "\t}"));
		assertTrue(rimDSL.contains("resource flightschedule_departureAirport"));
		assertTrue(rimDSL.contains("path: \"/FlightSchedules({id})/departureAirport\""));

		// check results for Passengers
		rimDSL = dslMap.get("Passenger");
		assertTrue(rimDSL.contains("rim Passenger {"));
		assertTrue(rimDSL.contains("resource Passengers"));
	}

	@Test
	public void testGenerateRimDslMapAirlinesOnError() {
		InteractionModel interactionModel = createAirlineModelDSL(null);
		// flight defines an onerror
		interactionModel.findResourceStateMachine("Flight").setRimName("Flight");

		//Run the generator
		RimDslGenerator generator = new RimDslGenerator(createVelocityEngine());
		// returns a map of specified resource state machine name and dsl
		Map<String,String> dslMap = generator.generateRimDslMap(interactionModel, JPAResponderGen.getDefaultCommands(), true);

		// check results for Flight
		String rimDSL = dslMap.get("Flight");
		assertTrue(rimDSL.contains("rim Flight {"));
		assertTrue(rimDSL.contains("onerror --> FlightResponder.ErrorMessages"));

	}

	@Test
	public void testGenerateRimDslDomain() {
		InteractionModel interactionModel = createAirlineModelDSL("airline");
		//Run the generator
		RimDslGenerator generator = new RimDslGenerator(createVelocityEngine());
		String dsl = generator.generateRimDsl(interactionModel, JPAResponderGen.getDefaultCommands(), true);
		
		//Check results
		assertTrue(dsl != null && !dsl.equals(""));
		assertTrue(dsl.contains("domain airline {"));
	}

	@Test
	public void testGenerateRimDslAirlinesNonStrictOData() {
		InteractionModel interactionModel = createAirlineModelDSL(null);
		//Run the generator
		RimDslGenerator generator = new RimDslGenerator(createVelocityEngine());
		String dsl = generator.generateRimDsl(interactionModel, JPAResponderGen.getDefaultCommands(), false);
		
		//Check results
		assertTrue(dsl != null && !dsl.equals(""));
		assertEquals(readTextFile(RIM_DSL_AIRLINE_NON_STRICT_ODATA_FILE), dsl);
	}

	@Test
	public void createBankingRimDsl() {
		//Define the basic interaction model based on the available metadata
		Metadata metadata = parseMetadata(METADATA_BANKING_XML_FILE);
		InteractionModel interactionModel = new InteractionModel(metadata);
		interactionModel.setExceptionState(new IMEntityState("InteractionException", "", Commands.GET_EXCEPTION));
		IMState rsErrors = new IMEntityState("Errors", "", Commands.GET_NOOP);
		interactionModel.addErrorHandlerState(rsErrors);
		
		Commands commands =  JPAResponderGen.getDefaultCommands();
		commands.addCommand("AuthoriseEntity", "com.temenos.interaction.commands.odata.UpdateEntityCommand", COMMAND_METADATA_SOURCE_ODATAPRODUCER);
		commands.addCommand("ReverseEntity", "com.temenos.interaction.commands.odata.UpdateEntityCommand", COMMAND_METADATA_SOURCE_ODATAPRODUCER);
		commands.addCommand("GETReversedEntities", "com.temenos.interaction.commands.odata.GETEntitiesCommand", COMMAND_METADATA_SOURCE_ODATAPRODUCER);
		commands.addCommand("GETReversedEntity", "com.temenos.interaction.commands.odata.GETEntityCommand", COMMAND_METADATA_SOURCE_ODATAPRODUCER);
		commands.addRimEvent("AUTHORISE", "PUT");
		commands.addRimEvent("REVERSE", "PUT");

		//Add state transitions
		IMResourceStateMachine rsm = interactionModel.findResourceStateMachine("Sector");
		rsm.addCollectionAndEntityState("IAuth", "unauthorised input records");
		rsm.addCollectionAndEntityState("Reversed", "reversed records", "GETReversedEntities", "GETReversedEntity");

		IMPseudoState pseudoState = rsm.addPseudoStateTransition("Sectors", "input", "Sectors", "POST", null, "CreateEntity", null, true);
		pseudoState.addAutoTransition(rsm.getResourceState("sector_IAuth"), "GET");
		pseudoState.addAutoTransition(rsm.getResourceState("sector"), "GET");
		pseudoState.setErrorHandlerState(rsErrors);
		

		pseudoState = rsm.addPseudoStateTransition("sector_IAuth", "authorise", "PUT", "authorise", "AuthoriseEntity", "edit", false);
		pseudoState.addAutoTransition(rsm.getResourceState("sector_IAuth"), "GET");
		pseudoState.addAutoTransition(rsm.getResourceState("sector"), "GET");
		
		pseudoState = rsm.addPseudoStateTransition("sector_IAuth", "delete", "DELETE", "delete", "DeleteEntity", "edit", false);
		pseudoState.addAutoTransition(rsm.getResourceState("sector"), "GET");

		pseudoState = rsm.addPseudoStateTransition("sector", "reverse", "sector_Reversed", "REVERSE", "reverse", "ReverseEntity", "edit", false);
		pseudoState.addAutoTransition(rsm.getResourceState("sector_Reversed"), "GET");
		
		//Run the generator
		RimDslGenerator generator = new RimDslGenerator(createVelocityEngine());
		String dsl = generator.generateRimDsl(interactionModel, commands, false);
		assertEquals(readTextFile(RIM_DSL_BANKING_FILE), dsl);
	}
	
	public InteractionModel createAirlineModelDSL(String domain) {
		//Define the basic interaction model based on the available metadata
		Metadata metadata = parseMetadata(METADATA_AIRLINE_XML_FILE);
		InteractionModel interactionModel = new InteractionModel(metadata);
		interactionModel.setDomain(domain);
		interactionModel.setExceptionState(new IMEntityState("InteractionException", "", Commands.GET_EXCEPTION));
		IMState rsResponseErrors = new IMEntityState("ErrorMessages", "", Commands.GET_NOOP);
		interactionModel.addErrorHandlerState(rsResponseErrors);

		//Add transitions
		IMResourceStateMachine rsmFlightSchedule = interactionModel.findResourceStateMachine("FlightSchedule");
		IMResourceStateMachine rsmAirport = interactionModel.findResourceStateMachine("Airport");
		IMResourceStateMachine rsmFlight = interactionModel.findResourceStateMachine("Flight");
		IMResourceStateMachine rsmPassenger = interactionModel.findResourceStateMachine("Passenger");
		
		rsmFlight.addTransitionToEntityState("flight", rsmFlightSchedule, "flightschedule", "flightScheduleNum", "flightschedule");
		rsmFlightSchedule.addTransitionToEntityState("flightschedule", rsmAirport, "departureAirport", "departureAirportCode", "departureAirport");
		rsmFlightSchedule.addTransitionToEntityState("flightschedule", rsmAirport, "arrivalAirport", "arrivalAirportCode", "arrivalAirport");
		rsmPassenger.addTransitionToEntityState("passenger", rsmFlight, "flight", "flightID", "flight");

		rsmAirport.addTransitionToCollectionState("airport", rsmFlightSchedule, "departures", "departureAirportCode eq '{code}'", "departureAirportCode", "departures");
		rsmAirport.addTransitionToCollectionState("airport", rsmFlightSchedule, "arrivals", "arrivalAirportCode eq '{code}'", "arrivalAirportCode", "arrivals");

		rsmFlightSchedule.addPseudoStateTransition("FlightSchedules", "created", "FlightSchedules", "POST", null, "CreateEntity", null, true);
		rsmFlight.addPseudoStateTransition("Flights", "created", "Flights", "POST", null, "CreateEntity", null, true);
		rsmAirport.addPseudoStateTransition("Airports", "created", "Airports", "POST", null, "CreateEntity", null, true);
		rsmPassenger.addPseudoStateTransition("Passengers", "created", "Passengers", "POST", null, "CreateEntity", null, true);
		
		//Add error handler
		IMState rsflightCreated = rsmFlight.getPseudoState("Flights", "created");
		rsflightCreated.setErrorHandlerState(rsResponseErrors);
		
		return interactionModel;
	}
	
	/*
	 * Create a velocity engine which loads velocity 
	 * templates from the classpath.
	 */
	private VelocityEngine createVelocityEngine() {
		VelocityEngine ve = new VelocityEngine();
		ve.setProperty(VelocityEngine.RESOURCE_LOADER, "classpath");
		ve.setProperty("classpath." + VelocityEngine.RESOURCE_LOADER + ".class", ClasspathResourceLoader.class.getName());
		ve.init();
		return ve;
	}

	/*
	 * Parse a metadata file
	 */
	private Metadata parseMetadata(String metadataFile) {
		MetadataParser parser = new MetadataParser();
		InputStream is = parser.getClass().getClassLoader().getResourceAsStream(metadataFile);
		Metadata metadata = parser.parse(is);
		Assert.assertNotNull(metadata);
		return metadata;
	}

	/*
	 * Read a text file
	 */
	private String readTextFile(String textFile) {
		InputStream is = this.getClass().getClassLoader().getResourceAsStream(textFile);
		BufferedReader br = new BufferedReader(new InputStreamReader(is));
		StringBuilder sb = new StringBuilder();
		String read;
		try {
			while((read = br.readLine()) != null) {
			    sb.append(read).append(System.getProperty("line.separator"));
			}
		}
		catch(IOException ioe) {
			fail(ioe.getMessage());
		}
		return sb.toString();		
	}
	
	@Test
	public void testGetRIM() {
		//Parse the test metadata
		MetadataParser parser = new MetadataParser();
		InputStream is = parser.getClass().getClassLoader().getResourceAsStream(METADATA_AIRLINE_XML_FILE);
		Metadata metadata = parser.parse(is);
		Assert.assertNotNull(metadata);

		//Define the interaction model
		InteractionModel interactionModel = new InteractionModel(metadata);

		IMResourceStateMachine rsmFlightSchedule = interactionModel.findResourceStateMachine("FlightSchedule");
		IMResourceStateMachine rsmAirport = interactionModel.findResourceStateMachine("Airport");
		IMResourceStateMachine rsmFlight = interactionModel.findResourceStateMachine("Flight");
		IMResourceStateMachine rsmPassenger = interactionModel.findResourceStateMachine("Passenger");
		rsmFlight.addTransitionToEntityState("flight", rsmFlightSchedule, "flightschedule", "flightScheduleNum", null);
		rsmFlightSchedule.addTransitionToEntityState("flightschedule", rsmAirport, "departureAirport", "departureAirportCode", null);
		rsmFlightSchedule.addTransitionToEntityState("flightschedule", rsmAirport, "arrivalAirport", "arrivalAirportCode", null);
		rsmAirport.addTransitionToCollectionState("airport", rsmFlightSchedule, "departures", "departureAirportCode eq '{code}'", "departureAirportCode", null);
		rsmAirport.addTransitionToCollectionState("airport", rsmFlightSchedule, "arrivals", "arrivalAirportCode eq '{code}'", "arrivalAirportCode", null);
		rsmPassenger.addTransitionToEntityState("passenger", rsmFlight, "flight", "flightID", null);
		
		//Run the generator
		RimDslGenerator generator = new RimDslGenerator(createVelocityEngine());
		Commands commands = JPAResponderGen.getDefaultCommands();
		String rimDSL = null;
		try {
			InputStream isRimDsl = generator.getRIM(interactionModel, commands, true);
			assertNotNull(isRimDsl);

			rimDSL = CharStreams.toString(new InputStreamReader(isRimDsl, "UTF-8"));
		}
		catch(Exception age) {
			fail(age.getMessage());
		}

		//Check the rim dsl
		assertTrue(rimDSL.contains("initial resource ServiceDocument"));
		assertTrue(rimDSL.contains("GET -> FlightSchedules"));
		assertTrue(rimDSL.contains("resource FlightSchedules"));
		assertTrue(rimDSL.contains("GET *-> flightschedule {" + RIM_LINE_SEP
				+ "\t\tparameters [ id=\"{flightScheduleID}\" ]" + RIM_LINE_SEP
				+ "\t}"));
		assertTrue(rimDSL.contains("GET *-> flightschedule_departureAirport {" + RIM_LINE_SEP
				+ "\t\tparameters [ id=\"{flightScheduleID}\" ]" + RIM_LINE_SEP
				+ "\t}"));
		assertTrue(rimDSL.contains("resource flightschedule_departureAirport"));
		assertTrue(rimDSL.contains("path: \"/FlightSchedules({id})/departureAirport\""));
		assertTrue(rimDSL.contains("GET -> Passengers"));
		assertTrue(rimDSL.contains("resource Passengers"));
	}

}
