package com.temenos.interaction.sdk;

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


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import com.temenos.interaction.core.entity.EntityMetadata;
import com.temenos.interaction.core.entity.Metadata;
import com.temenos.interaction.core.entity.MetadataParser;
import com.temenos.interaction.core.entity.vocabulary.Vocabulary;
import com.temenos.interaction.core.entity.vocabulary.terms.TermIdField;
import com.temenos.interaction.core.entity.vocabulary.terms.TermValueType;
import com.temenos.interaction.sdk.adapter.edmx.EDMXAdapter;
import com.temenos.interaction.sdk.interaction.IMResourceStateMachine;
import com.temenos.interaction.sdk.interaction.InteractionModel;

/**
 * Unit test for {@link JPAResponderGen}.
 */
public class TestJPAResponderGen {
	public final static String METADATA_AIRLINE_XML_FILE = "AirlinesMetadata.xml";
	public final static String EDMX_AIRLINE_FILE = "airlines.edmx";
	private final static String RIM_LINE_SEP = System.getProperty("line.separator");
	
	//Mock out methods that write contents to files
	class MockGenerator extends JPAResponderGen {
		List<String> generatedClasses = new ArrayList<String>();
		String generatedPersistenceXML;
		String generatedSpringXML;
		String generatedSpringResourceManagerXML;
		String generateResponderDML;
		String generatedRimDsl;
		String generatedMetadata;
		
		@Override
		protected boolean writeClass(String path, String classFileName, String generatedClass) {
			this.generatedClasses.add(generatedClass);
			return true;
		}
		
		@Override
		protected boolean writeJPAConfiguration(File sourceDir, String generatedPersistenceXML) {
			this.generatedPersistenceXML = generatedPersistenceXML;
			return true;
		}

		@Override
		protected boolean writeSpringConfiguration(File sourceDir, String filename, String generatedSpringXML) {
			if(filename.equals(JPAResponderGen.SPRING_CONFIG_FILE)) {
				this.generatedSpringXML = generatedSpringXML;
			}
			else if(filename.equals(JPAResponderGen.SPRING_RESOURCEMANAGER_FILE)) {
				this.generatedSpringResourceManagerXML = generatedSpringXML;
			}
			return true;
		}
		
		@Override
		protected boolean writeResponderDML(File sourceDir, String generateResponderDML) {
			this.generateResponderDML = generateResponderDML;
			return true;
		}
		
		@Override
		protected boolean writeRimDsl(File sourceDir, String rimDslFilename, String generatedRimDsl) {
			this.generatedRimDsl = generatedRimDsl;
			return true;
		}
		
		@Override
		protected boolean writeMetadata(File sourceDir, String generatedMetadata) {
			this.generatedMetadata = generatedMetadata;
			return true;
		}
	};

	@Test
	public void testEntityInfo() {
		JPAResponderGen rg = new JPAResponderGen();
		
		EntityMetadata mdEntity = new EntityMetadata("Flight");
		Vocabulary voc = new Vocabulary();
		voc.setTerm(new TermValueType(TermValueType.INTEGER_NUMBER));
		voc.setTerm(new TermIdField(true));
		mdEntity.setPropertyVocabulary("ID", voc);
		EntityInfo p = rg.createEntityInfoFromEntityMetadata("AirlineModel", mdEntity);
		
		assertEquals("Flight", p.getClazz());
		assertEquals("AirlineModel", p.getPackage());
		assertEquals("AirlineModel.Flight", p.getFQTypeName());
	}

	@Test
	public void testJPAEntityKeyInfo() {
		JPAResponderGen rg = new JPAResponderGen();
		
		EntityMetadata mdEntity = new EntityMetadata("Flight");
		Vocabulary voc = new Vocabulary();
		voc.setTerm(new TermValueType(TermValueType.INTEGER_NUMBER));
		voc.setTerm(new TermIdField(true));
		mdEntity.setPropertyVocabulary("flightID", voc);
		EntityInfo p = rg.createEntityInfoFromEntityMetadata("AirlineModel", mdEntity);
		
		assertEquals("flightID", p.getKeyInfo().getName());
		assertEquals("Long", p.getKeyInfo().getType());
		assertEquals(0, p.getFieldInfos().size());
	}
	
	@Test
	public void testJPAEntityFieldInfo() {
		JPAResponderGen rg = new JPAResponderGen();
		
		EntityMetadata mdEntity = new EntityMetadata("Flight");
		Vocabulary voc = new Vocabulary();
		voc.setTerm(new TermValueType(TermValueType.INTEGER_NUMBER));
		voc.setTerm(new TermIdField(true));
		mdEntity.setPropertyVocabulary("ID", voc);
		voc = new Vocabulary();
		voc.setTerm(new TermValueType(TermValueType.INTEGER_NUMBER));
		mdEntity.setPropertyVocabulary("flightID", voc);
		mdEntity.setPropertyVocabulary("number", new Vocabulary());
		mdEntity.setPropertyVocabulary("runway", new Vocabulary());
		voc = new Vocabulary();
		voc.setTerm(new TermValueType(TermValueType.TIMESTAMP));
		mdEntity.setPropertyVocabulary("departureDT", voc);
		voc = new Vocabulary();
		voc.setTerm(new TermValueType(TermValueType.DATE));
		mdEntity.setPropertyVocabulary("departureDate", voc);
		voc = new Vocabulary();
		voc.setTerm(new TermValueType(TermValueType.TIME));
		mdEntity.setPropertyVocabulary("departureTime", voc);
		EntityInfo p = rg.createEntityInfoFromEntityMetadata("AirlineModel", mdEntity);
		
		assertEquals(6, p.getFieldInfos().size());
		assertTrue(p.getFieldInfos().contains(new FieldInfo("flightID", "Long", null)));
		assertTrue(p.getFieldInfos().contains(new FieldInfo("number", "String", null)));
		assertTrue(p.getFieldInfos().contains(new FieldInfo("runway", "String", null)));
		assertTrue(p.getFieldInfos().contains(new FieldInfo("departureDT", "java.util.Date", null)));
		assertTrue(p.getFieldInfos().contains(new FieldInfo("departureDate", "java.util.Date", null)));
		assertTrue(p.getFieldInfos().contains(new FieldInfo("departureTime", "java.util.Date", null)));
	}
	
	@Test
	public void testJPAEntityFieldInfoAnnotations() {
		JPAResponderGen rg = new JPAResponderGen();
		
		EntityMetadata mdEntity = new EntityMetadata("Flight");
		Vocabulary voc = new Vocabulary();
		voc.setTerm(new TermValueType(TermValueType.INTEGER_NUMBER));
		voc.setTerm(new TermIdField(true));
		mdEntity.setPropertyVocabulary("ID", voc);
		voc = new Vocabulary();
		voc.setTerm(new TermValueType(TermValueType.TIMESTAMP));
		mdEntity.setPropertyVocabulary("departureDT", voc);
		voc = new Vocabulary();
		voc.setTerm(new TermValueType(TermValueType.DATE));
		mdEntity.setPropertyVocabulary("departureDate", voc);
		voc = new Vocabulary();
		voc.setTerm(new TermValueType(TermValueType.TIME));
		mdEntity.setPropertyVocabulary("departureTime", voc);
		EntityInfo p = rg.createEntityInfoFromEntityMetadata("AirlineModel", mdEntity);

		// Annotations
		FieldInfo dateOnlyFI = p.getFieldInfos().get(0);
		assertEquals(1, dateOnlyFI.getAnnotations().size());
		assertEquals("@Temporal(TemporalType.DATE)", dateOnlyFI.getAnnotations().get(0));
		
		FieldInfo timeFI = p.getFieldInfos().get(1);
		assertEquals(1, timeFI.getAnnotations().size());
		assertEquals("@Temporal(TemporalType.TIME)", timeFI.getAnnotations().get(0));

		FieldInfo dateFI = p.getFieldInfos().get(2);
		assertEquals(1, dateFI.getAnnotations().size());
		assertEquals("@Temporal(TemporalType.TIMESTAMP)", dateFI.getAnnotations().get(0));
	}
	
	@Test(expected = AssertionError.class)
	public void testGenJPAEntityNull() {
		JPAResponderGen rg = new JPAResponderGen();
		assertNull(rg.generateJPAEntityClass(null));
	}

	@Test
	public void testGenJPAEntity() {
		JPAResponderGen rg = new JPAResponderGen();
		
		FieldInfo keyInfo = new FieldInfo("flightID", "Long", null);
		List<FieldInfo> properties = new ArrayList<FieldInfo>();
		properties.add(new FieldInfo("number", "String", null));
		properties.add(new FieldInfo("fitHostiesName", "String", null));
		properties.add(new FieldInfo("runway", "String", null));
		properties.add(new FieldInfo("passengers", "Integer", null));
		
		List<String> annotations = new ArrayList<String>();
		annotations.add("@Temporal(TemporalType.TIMESTAMP)");
		properties.add(new FieldInfo("departureDT", "java.util.Date", annotations));
		
		properties.add(new FieldInfo("dinnerServed", "java.sql.Timestamp", null));
		
		EntityInfo jpaEntity = new EntityInfo("Flight", "AirlineModel.model", keyInfo, properties);
		String generatedClass = rg.generateJPAEntityClass(jpaEntity);
		
		assertTrue(generatedClass.contains("package AirlineModel.model;"));
		assertTrue(generatedClass.contains("import javax.persistence.Entity;"));
		assertTrue(generatedClass.contains("@Entity"));
		assertTrue(generatedClass.contains("public class Flight {"));
		assertTrue(generatedClass.contains("@Id"));
		assertTrue(generatedClass.contains("@Basic(optional = false)"));
		assertTrue(generatedClass.contains("private Long flightID;"));
		assertTrue(generatedClass.contains("private String number;"));
		assertTrue(generatedClass.contains("private String fitHostiesName;"));
		assertTrue(generatedClass.contains("private String runway;"));
		assertTrue(generatedClass.contains("private Integer passengers;"));

		// date handling needs some special support
		assertTrue(generatedClass.contains("@Temporal(TemporalType.TIMESTAMP)"));
		assertTrue(generatedClass.contains("private java.util.Date departureDT;"));

		assertTrue(generatedClass.contains("private java.sql.Timestamp dinnerServed;"));
		assertTrue(generatedClass.contains("public Flight() {"));
	}

	@Test
	public void testGenJPAEntitySimpleJoin() {
		JPAResponderGen rg = new JPAResponderGen();
		
		FieldInfo keyInfo = new FieldInfo("flightID", "Long", null);

		List<FieldInfo> properties = new ArrayList<FieldInfo>();
		properties.add(new FieldInfo("number", "String", null));
		List<String> annotations = new ArrayList<String>();
		annotations.add("@Temporal(TemporalType.TIMESTAMP)");
		properties.add(new FieldInfo("departureDT", "java.util.Date", annotations));

		List<JoinInfo> joins = new ArrayList<JoinInfo>();
		List<String> joinAnnotations = new ArrayList<String>();
		joinAnnotations.add("@ManyToMany(mappedBy=\"Flight\")");
		joins.add(new JoinInfo("passengers", "Person", joinAnnotations));
		
		EntityInfo jpaEntity = new EntityInfo("Flight", "AirlineModel.model", keyInfo, properties, joins, true);
		String generatedClass = rg.generateJPAEntityClass(jpaEntity);
		
		assertTrue(generatedClass.contains("package AirlineModel.model;"));
		assertTrue(generatedClass.contains("import javax.persistence.Entity;"));
		assertTrue(generatedClass.contains("@Entity"));
		assertTrue(generatedClass.contains("public class Flight {"));
		assertTrue(generatedClass.contains("@Id"));
		assertTrue(generatedClass.contains("@Basic(optional = false)"));
		assertTrue(generatedClass.contains("private Long flightID;"));
		assertTrue(generatedClass.contains("private String number;"));

		assertTrue(generatedClass.contains("@Temporal(TemporalType.TIMESTAMP)"));
		assertTrue(generatedClass.contains("private java.util.Date departureDT;"));

		assertTrue(generatedClass.contains("@ManyToMany(mappedBy=\"Flight\")"));
		assertTrue(generatedClass.contains("private Collection<Person> passengers;"));
		
		assertTrue(generatedClass.contains("public Flight() {"));
	}

	@Test
	public void testGenJPAConfiguration() {
		JPAResponderGen rg = new JPAResponderGen();
		
		List<EntityInfo> entitiesInfo = new ArrayList<EntityInfo>();
		entitiesInfo.add(new EntityInfo("Flight", "AirlineModel", null, null));
		entitiesInfo.add(new EntityInfo("Airport", "AirlineModel", null, null));
		entitiesInfo.add(new EntityInfo("FlightSchedule", "AirlineModel", null, null));
		String generatedPersistenceConfig = rg.generateJPAConfiguration(entitiesInfo);
		
		assertTrue(generatedPersistenceConfig.contains("<!-- Generated JPA configuration for IRIS MockResponder -->"));
		assertTrue(generatedPersistenceConfig.contains("<class>AirlineModel.Flight</class>"));
		assertTrue(generatedPersistenceConfig.contains("<class>AirlineModel.Airport</class>"));
		assertTrue(generatedPersistenceConfig.contains("<class>AirlineModel.FlightSchedule</class>"));
	}

	@Test
	public void testGenResponderDML() {
		JPAResponderGen rg = new JPAResponderGen();
		
		List<EntityInfo> entitiesInfo = new ArrayList<EntityInfo>();
		
		List<FieldInfo> properties = new ArrayList<FieldInfo>();
		properties.add(new FieldInfo("number", "Long", null));
		properties.add(new FieldInfo("fitHostiesName", "String", null));
		properties.add(new FieldInfo("runway", "String", null));
		entitiesInfo.add(new EntityInfo("Flight", "AirlineModel", null, properties));
		entitiesInfo.add(new EntityInfo("Airport", "AirlineModel", null, null));
		entitiesInfo.add(new EntityInfo("FlightSchedule", "AirlineModel", null, null));
		String generatedResponderDML = rg.generateResponderDML(entitiesInfo);
		
		assertTrue(generatedResponderDML.contains("INSERT INTO `Flight`(`number` , `fitHostiesName` , `runway`) VALUES('1' , 'abc' , 'abc');"));
		assertTrue(generatedResponderDML.contains("INSERT INTO `Airport`() VALUES();"));
		assertTrue(generatedResponderDML.contains("INSERT INTO `FlightSchedule`() VALUES();"));
	}

	@Test
	public void testGenResponderDMLWithNavProperties() {
		JPAResponderGen rg = new JPAResponderGen();
		
		List<EntityInfo> entitiesInfo = new ArrayList<EntityInfo>();
		
		List<FieldInfo> properties = new ArrayList<FieldInfo>();
		List<String> annotations = new ArrayList<String>();
		annotations.add("@ManyToOne(optional = false)");
		properties.add(new FieldInfo("number", "Long", null));
		properties.add(new FieldInfo("fitHostiesName", "String", annotations));
		properties.add(new FieldInfo("runway", "String", null));
		entitiesInfo.add(new EntityInfo("Flight", "AirlineModel", null, properties));
		entitiesInfo.add(new EntityInfo("Airport", "AirlineModel", null, null));
		entitiesInfo.add(new EntityInfo("FlightSchedule", "AirlineModel", null, null));
		String generatedResponderDML = rg.generateResponderDML(entitiesInfo);
		
		assertTrue(generatedResponderDML.contains("INSERT INTO `Flight`(`number` , `runway`) VALUES('1' , 'abc');"));
		assertTrue(generatedResponderDML.contains("INSERT INTO `Airport`() VALUES();"));
		assertTrue(generatedResponderDML.contains("INSERT INTO `FlightSchedule`() VALUES();"));
	}
	
	@Test
	public void testFormClassFilename() {
		assertEquals("/tmp/blah/com/some/package/SomeClass.java", JPAResponderGen.formClassFilename("/tmp/blah/com/some/package", new EntityInfo("SomeClass", "com.some.package", null, null)));
	}
	
	@Test
	public void testGeneratedArtifactsFromConceptualModels() {
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
		MockGenerator generator = new MockGenerator();
		boolean status = generator.generateArtifacts(metadata, interactionModel, new File("target/FlightResponder/classes"), new File("target/FlightResponder/classes"), true);
		
		//Check results
		assertTrue(status);
		
		assertTrue(generator.generatedClasses.size() == 4);
		assertTrue(generator.generatedClasses.get(0).contains("public class Flight"));
		assertTrue(generator.generatedClasses.get(1).contains("public class Airport"));
		assertTrue(generator.generatedClasses.get(2).contains("public class FlightSchedule"));
		assertTrue(generator.generatedClasses.get(3).contains("public class Passenger"));

		assertTrue(generator.generateResponderDML.contains("INSERT INTO `Flight`("));
		assertTrue(generator.generateResponderDML.contains("INSERT INTO `Airport`("));
		assertTrue(generator.generateResponderDML.contains("INSERT INTO `FlightSchedule`("));
		assertTrue(generator.generateResponderDML.contains("INSERT INTO `Passenger`("));

		assertTrue(generator.generatedPersistenceXML.contains("<class>FlightResponderModel.Flight</class>"));
		assertTrue(generator.generatedPersistenceXML.contains("<class>FlightResponderModel.Airport</class>"));
		assertTrue(generator.generatedPersistenceXML.contains("<class>FlightResponderModel.FlightSchedule</class>"));
		assertTrue(generator.generatedPersistenceXML.contains("<class>FlightResponderModel.Passenger</class>"));

		assertTrue(generator.generatedSpringXML.contains("<bean id=\"behaviour\" class=\"FlightResponderModel.FlightResponderBehaviour\" />"));

		assertTrue(generator.generatedSpringResourceManagerXML.contains("<constructor-arg name=\"namespace\" value=\"FlightResponder\" />"));		
		
		//Test rim dsl
		assertTrue(generator.generatedRimDsl.contains("initial resource ServiceDocument"));
		assertTrue(generator.generatedRimDsl.contains("GET -> FlightSchedules"));
		assertTrue(generator.generatedRimDsl.contains("resource FlightSchedules"));
		assertTrue(generator.generatedRimDsl.contains("GET *-> flightschedule {" + RIM_LINE_SEP
				+ "\t\tparameters [ id=\"{flightScheduleID}\" ]" + RIM_LINE_SEP
				+ "\t}"));
		assertTrue(generator.generatedRimDsl.contains("GET *-> flightschedule_departureAirport {" + RIM_LINE_SEP
				+ "\t\tparameters [ id=\"{flightScheduleID}\" ]" + RIM_LINE_SEP
				+ "\t}"));
		assertTrue(generator.generatedRimDsl.contains("resource flightschedule_departureAirport"));
		assertTrue(generator.generatedRimDsl.contains("path: \"/FlightSchedules({id})/departureAirport\""));
		assertTrue(generator.generatedRimDsl.contains("GET -> Passengers"));
		assertTrue(generator.generatedRimDsl.contains("resource Passengers"));
	}

	@Test
	public void testGeneratedArtifactsFromEdmx() {
		//Parse the test metadata
		InputStream isEdmx = getClass().getResourceAsStream("/" + EDMX_AIRLINE_FILE);

		//Run the generator
		MockGenerator generator = new MockGenerator();
		boolean status = generator.generateArtifacts(new EDMXAdapter(isEdmx), new File("target/FlightResponder/classes"), new File("target/FlightResponder/classes"), true);
		
		//Check results
		assertTrue(status);
		
		assertTrue(generator.generatedClasses.size() == 4);
		assertTrue(generator.generatedClasses.get(0).contains("public class FlightSchedule"));
		assertTrue(generator.generatedClasses.get(1).contains("public class Airport"));
		assertTrue(generator.generatedClasses.get(2).contains("public class Flight"));
		assertTrue(generator.generatedClasses.get(3).contains("public class Passenger"));

		assertTrue(generator.generateResponderDML.contains("INSERT INTO `Flight`("));
		assertTrue(generator.generateResponderDML.contains("INSERT INTO `Airport`("));
		assertTrue(generator.generateResponderDML.contains("INSERT INTO `FlightSchedule`("));
		assertTrue(generator.generateResponderDML.contains("INSERT INTO `Passenger`("));

		assertTrue(generator.generatedPersistenceXML.contains("<class>FlightResponderModel.Flight</class>"));
		assertTrue(generator.generatedPersistenceXML.contains("<class>FlightResponderModel.Airport</class>"));
		assertTrue(generator.generatedPersistenceXML.contains("<class>FlightResponderModel.FlightSchedule</class>"));
		assertTrue(generator.generatedPersistenceXML.contains("<class>FlightResponderModel.Passenger</class>"));

		assertTrue(generator.generatedSpringXML.contains("<bean id=\"behaviour\" class=\"FlightResponderModel.FlightResponderBehaviour\" />"));

		assertTrue(generator.generatedSpringResourceManagerXML.contains("<constructor-arg name=\"namespace\" value=\"FlightResponder\" />"));		
		
		//Test rim dsl
		assertTrue(generator.generatedRimDsl.contains("initial resource ServiceDocument"));
		assertTrue(generator.generatedRimDsl.contains("GET -> FlightSchedules"));
		assertTrue(generator.generatedRimDsl.contains("resource FlightSchedules"));
		assertTrue(generator.generatedRimDsl.contains("GET *-> flightschedule {" + RIM_LINE_SEP
				+ "\t\tparameters [ id=\"{flightScheduleID}\" ]" + RIM_LINE_SEP
				+ "\t}"));
		assertTrue(generator.generatedRimDsl.contains("GET *-> flightschedule_departureAirport {" + RIM_LINE_SEP
				+ "\t\ttitle: \"departureAirport\"" + RIM_LINE_SEP
				+ "\t\tparameters [ id=\"{flightScheduleID}\" ]" + RIM_LINE_SEP
				+ "\t}"));
		assertTrue(generator.generatedRimDsl.contains("resource flightschedule_departureAirport"));
		assertTrue(generator.generatedRimDsl.contains("path: \"/FlightSchedules({id})/departureAirport\""));
		assertTrue(generator.generatedRimDsl.contains("GET -> airport_arrivals {" + RIM_LINE_SEP
				+ "\t\ttitle: \"arrivals\"" + RIM_LINE_SEP
				+ "\t\tparameters [ id=\"{code}\" ]" + RIM_LINE_SEP
				+ "\t}"));
		assertTrue(generator.generatedRimDsl.contains("GET -> airport_departures {" + RIM_LINE_SEP
				+ "\t\ttitle: \"departures\"" + RIM_LINE_SEP
				+ "\t\tparameters [ id=\"{code}\" ]" + RIM_LINE_SEP
				+ "\t}"));
		assertTrue(generator.generatedRimDsl.contains("GET -> Passengers"));
		assertTrue(generator.generatedRimDsl.contains("resource Passengers"));
	}

	@Test
	public void testRIMWithoutReciprocalLinks() {
		//Parse the test metadata
		MetadataParser parser = new MetadataParser();
		InputStream is = parser.getClass().getClassLoader().getResourceAsStream(METADATA_AIRLINE_XML_FILE);
		Metadata metadata = parser.parse(is);
		Assert.assertNotNull(metadata);

		//Define the interaction model
		InteractionModel interactionModel = new InteractionModel(metadata);

		//Do not specify a reciprocal link
		IMResourceStateMachine rsmFlightSchedule = interactionModel.findResourceStateMachine("FlightSchedule");
		IMResourceStateMachine rsmAirport = interactionModel.findResourceStateMachine("Airport");
		IMResourceStateMachine rsmFlight = interactionModel.findResourceStateMachine("Flight");
		IMResourceStateMachine rsmPassenger = interactionModel.findResourceStateMachine("Passenger");
		rsmFlight.addTransitionToEntityState("flight", rsmFlightSchedule, "flightschedule", "flightScheduleNum", null);
		rsmFlightSchedule.addTransitionToEntityState("flightschedule", rsmAirport, "departureAirport", "departureAirportCode", null);
		rsmFlightSchedule.addTransitionToEntityState("flightschedule", rsmAirport, "arrivalAirport", "arrivalAirportCode", null);
		rsmAirport.addTransitionToCollectionState("airport", rsmFlightSchedule, "departures", "departureAirportCode eq '{code}'", "departureAirportCode", "departures");
		rsmAirport.addTransitionToCollectionState("airport", rsmFlightSchedule, "arrivals", "arrivalAirportCode eq '{code}'", "arrivalAirportCode", "arrivals");
		rsmPassenger.addTransitionToEntityState("passenger", rsmFlight, "flight", "flightID", "flight");
		
		//Run the generator
		MockGenerator generator = new MockGenerator();
		boolean status = generator.generateArtifacts(metadata, interactionModel, new File("target/FlightResponder/classes"), new File("target/FlightResponder/classes"), true);
		
		//Check results
		assertTrue(status);
		
		//Test rim dsl
		assertTrue(generator.generatedRimDsl.contains("GET *-> flightschedule_departureAirport {" + RIM_LINE_SEP
				+ "\t\tparameters [ id=\"{flightScheduleID}\" ]" + RIM_LINE_SEP
				+ "\t}"));
		assertTrue(generator.generatedRimDsl.contains("GET -> flightschedule_departureAirport {" + RIM_LINE_SEP
				+ "\t\tparameters [ id=\"{flightScheduleID}\" ]" + RIM_LINE_SEP
				+ "\t}"));
		assertTrue(generator.generatedRimDsl.contains("GET *-> flightschedule_arrivalAirport {" + RIM_LINE_SEP
				+ "\t\tparameters [ id=\"{flightScheduleID}\" ]" + RIM_LINE_SEP
				+ "\t}"));
		assertTrue(generator.generatedRimDsl.contains("GET -> flightschedule_arrivalAirport {" + RIM_LINE_SEP
				+ "\t\tparameters [ id=\"{flightScheduleID}\" ]" + RIM_LINE_SEP
				+ "\t}"));
		assertTrue(generator.generatedRimDsl.contains("GET -> airport_arrivals {" + RIM_LINE_SEP
				+ "\t\ttitle: \"arrivals\"" + RIM_LINE_SEP
				+ "\t\tparameters [ id=\"{code}\" ]" + RIM_LINE_SEP
				+ "\t}"));
		assertTrue(generator.generatedRimDsl.contains("GET -> airport_departures {" + RIM_LINE_SEP
				+ "\t\ttitle: \"departures\"" + RIM_LINE_SEP
				+ "\t\tparameters [ id=\"{code}\" ]" + RIM_LINE_SEP
				+ "\t}"));
		assertTrue(generator.generatedRimDsl.contains("GET -> passenger_flight {" + RIM_LINE_SEP
				+ "\t\ttitle: \"flight\"" + RIM_LINE_SEP
				+ "\t\tparameters [ id=\"{passengerNo}\" ]" + RIM_LINE_SEP
				+ "\t}"));
	}
	
	/*
	 * To support resource state transitions (as opposed to application state transitions)
	 * we use a pseudo state in our resource interaction model.  A pseudo state does not necessarily
	 * have an addressable uri, and always has actions.
	 */
	@Test
	public void testIMWithResourcePseudoStates() {
		//Parse the test metadata
		MetadataParser parser = new MetadataParser();
		InputStream is = parser.getClass().getClassLoader().getResourceAsStream(METADATA_AIRLINE_XML_FILE);
		Metadata metadata = parser.parse(is);
		Assert.assertNotNull(metadata);

		//Define the interaction model
		InteractionModel interactionModel = new InteractionModel(metadata);

		// Add CRUD pseudo states to an entity/entityset
		IMResourceStateMachine rsmAirport = interactionModel.findResourceStateMachine("Airport");
		rsmAirport.addPseudoStateTransition("Airports", "created", "Airports", "POST", null, "CreateEntity", null, true);
		rsmAirport.addPseudoStateTransition("airport", "updated", "airport", "PUT", null, "UpdateEntity", "edit", false);
		rsmAirport.addPseudoStateTransition("airport", "deleted", "DELETE", null, "DeleteEntity", "edit", false);
		
		//Run the generator
		MockGenerator generator = new MockGenerator();
		boolean status = generator.generateArtifacts(metadata, interactionModel, new File("target/FlightResponder/classes"), new File("target/FlightResponder/classes"), true);
		
		//Check results
		assertTrue(status);
		
		//Test rim dsl
		assertTrue(generator.generatedRimDsl.contains("POST -> Airports_created"));
		assertTrue(generator.generatedRimDsl.contains("PUT *-> airport_updated"));
		assertTrue(generator.generatedRimDsl.contains("PUT -> airport_updated"));
		assertTrue(generator.generatedRimDsl.contains("DELETE *-> airport_deleted"));
		assertTrue(generator.generatedRimDsl.contains("DELETE -> airport_deleted"));
		assertTrue(generator.generatedRimDsl.contains("resource Airports_created"));
		assertTrue(generator.generatedRimDsl.contains("resource Airports_created {" + RIM_LINE_SEP +
				"\ttype: item" + RIM_LINE_SEP +
				"\tentity: Airport" + RIM_LINE_SEP +
				"\tactions [ CreateEntity ]" + RIM_LINE_SEP +
				"\tpath: \"/Airports()\""));

		assertTrue(generator.generatedRimDsl.contains("resource airport_updated {" + RIM_LINE_SEP +
				"\ttype: item" + RIM_LINE_SEP +
				"\tentity: Airport" + RIM_LINE_SEP +
				"\tactions [ UpdateEntity ]" + RIM_LINE_SEP +
				"\trelations [ \"edit\" ]" + RIM_LINE_SEP +
				"\tpath: \"/Airports('{id}')\""));
		assertTrue(generator.generatedRimDsl.contains("resource airport_deleted {" + RIM_LINE_SEP +
				"\ttype: item" + RIM_LINE_SEP +
				"\tentity: Airport" + RIM_LINE_SEP +
				"\tactions [ DeleteEntity ]" + RIM_LINE_SEP +
				"\trelations [ \"edit\" ]" + RIM_LINE_SEP +
				"\tpath: \"/Airports('{id}')/deleted\""));
		assertTrue(generator.generatedRimDsl.contains("resource airport {" + RIM_LINE_SEP +
				"\ttype: item" + RIM_LINE_SEP +
				"\tentity: Airport" + RIM_LINE_SEP +
				"\tview: GETEntity" + RIM_LINE_SEP +
				"\tpath: \"/Airports('{id}')\"" + RIM_LINE_SEP +
				"\tPUT -> airport_updated {" + RIM_LINE_SEP
				+ "\t\tparameters [ id=\"{code}\" ]" + RIM_LINE_SEP
				+ "\t}" + RIM_LINE_SEP +
				"\tDELETE -> airport_deleted {" + RIM_LINE_SEP
				+ "\t\tparameters [ id=\"{code}\" ]" + RIM_LINE_SEP
				+ "\t}"));

	}
	
}
