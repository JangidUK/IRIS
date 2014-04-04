package com.temenos.interaction.media.odata.xml.atom;

/*
 * #%L
 * interaction-media-odata-xml
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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.URI;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.UriInfo;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.odata4j.core.OCollection;
import org.odata4j.core.OCollections;
import org.odata4j.core.OComplexObject;
import org.odata4j.core.OComplexObjects;
import org.odata4j.core.OEntities;
import org.odata4j.core.OEntity;
import org.odata4j.core.OEntityKey;
import org.odata4j.core.OObject;
import org.odata4j.core.OProperties;
import org.odata4j.core.OProperty;
import org.odata4j.edm.EdmCollectionType;
import org.odata4j.edm.EdmComplexType;
import org.odata4j.edm.EdmDataServices;
import org.odata4j.edm.EdmEntitySet;
import org.odata4j.edm.EdmEntityType;
import org.odata4j.edm.EdmProperty;
import org.odata4j.edm.EdmProperty.CollectionKind;
import org.odata4j.edm.EdmType;
import org.odata4j.format.FormatWriter;
import org.odata4j.format.FormatWriterFactory;

import com.temenos.interaction.commands.odata.OEntityTransformer;
import com.temenos.interaction.core.MultivaluedMapImpl;
import com.temenos.interaction.core.entity.Metadata;
import com.temenos.interaction.core.entity.MetadataParser;
import com.temenos.interaction.core.hypermedia.Action;
import com.temenos.interaction.core.hypermedia.CollectionResourceState;
import com.temenos.interaction.core.hypermedia.ResourceState;
import com.temenos.interaction.core.hypermedia.ResourceStateMachine;
import com.temenos.interaction.core.hypermedia.Transition;
import com.temenos.interaction.core.hypermedia.UriSpecification;
import com.temenos.interaction.core.resource.EntityResource;
import com.temenos.interaction.core.resource.RESTResource;
import com.temenos.interaction.odataext.entity.MetadataOData4j;

public class TestAtomXMLProviderWithBag {
	public final static String SERVICE_METADATA = "metadata.xml";
	public final static String NEW_TELLER_ENTRY = "NewTellerEntry.xml";
	public final static String POPULATED_TELLER_ENTRY = "PopulatedTellerEntry.xml";
	public final static String TELLER_ENTRY_REP_AS_COLLECTION = "TellerEntryRepAsCollection.xml";
	public final static String NEW_TELLER_ENTRY_KEY = "TT13081VLCS3";
	
	public final static String TELLER_ENTITY_NAME = "Teller_Cashinl";
	public final static String TELLER_ENTITY_SETNAME = "Teller_Cashinls";

	public final static String CUSTOMER_ENTITY_NAME = "Customer";
	public final static String CUSTOMER_ENTITY_SETNAME = "Customers";
	
	// Private Global Variables
	private static Metadata metadata = null;
	private static EdmDataServices edmMetadata = null;
	
	@BeforeClass
	public static void setup() {
		//Parse metadata.xml file
		MetadataParser testParser = new MetadataParser();
		InputStream testIs = TestAtomXMLProviderWithBag.class.getClassLoader().getResourceAsStream(SERVICE_METADATA);
		metadata = testParser.parse(testIs);
		edmMetadata = getEdmDataServices(metadata, TELLER_ENTITY_NAME, CUSTOMER_ENTITY_NAME);
	}
	
	@AfterClass
	public static void tearDown() {
		metadata = null;
		edmMetadata = null;
	}
	
	private AtomXMLProvider getAtomXMLProvider(Metadata metadata, ResourceStateMachine rsm) {
		return new AtomXMLProvider(new	MetadataOData4j(metadata, rsm), metadata, rsm, new OEntityTransformer());
	}
	
	@Test
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void testReadPathWithCollection() throws Exception {
		ResourceStateMachine rsm = getResourceStateMachine(TELLER_ENTITY_NAME);
		GenericEntity<EntityResource<OEntity>> ge = new GenericEntity<EntityResource<OEntity>>(new EntityResource<OEntity>(null)) {};
		
		//AtomXMLProvider ap = new AtomXMLProvider(edmMetadata, metadata, rsm, new OEntityTransformer());
		AtomXMLProvider ap = getAtomXMLProvider(metadata, rsm);
		UriInfo mockUriInfo = mock(UriInfo.class);
		when(mockUriInfo.getBaseUri()).thenReturn(new URI("http://www.temenos.com/rest.svc/"));
		when(mockUriInfo.getPath()).thenReturn("/" + TELLER_ENTITY_SETNAME);
		MultivaluedMap<String, String> mockPathParameters = new MultivaluedMapImpl<String>();
		mockPathParameters.add("id", "2");
		when(mockUriInfo.getPathParameters()).thenReturn(mockPathParameters);
		ap.setUriInfo(mockUriInfo);
		Request requestContext = mock(Request.class);
		when(requestContext.getMethod()).thenReturn("GET");
		ap.setRequestContext(requestContext);

		EntityResource<OEntity> result = ap.readFrom(RESTResource.class, ge.getType(), null, MediaType.APPLICATION_ATOM_XML_TYPE, null, readInputStream(NEW_TELLER_ENTRY));
		assertNotNull(result);
		
		OEntity entity = result.getEntity();
		assertNotNull (entity);
		
		// Now verify the properties populated from collection
		OProperty<?> denomProp = entity.getProperty("Teller_Cashinl_DrDenomMvGroup");
		EdmType denomPropType = denomProp.getType();
		assertFalse(denomPropType.isSimple());
		assertEquals("List(hothouse-modelsModel.Teller_Cashinl_DrDenomMvGroup)", denomPropType.getFullyQualifiedTypeName());
		EdmCollectionType denomPropCollType = (EdmCollectionType) denomPropType;
		assertEquals("hothouse-modelsModel.Teller_Cashinl_DrDenomMvGroup", denomPropCollType.getItemType().getFullyQualifiedTypeName());
		assertEquals(CollectionKind.List, denomPropCollType.getCollectionKind());
		OCollection<OComplexObject> denomOCollprops = (OCollection) denomProp.getValue();
		assertEquals(1, denomOCollprops.size());
		Iterator<OComplexObject> denomOCollIt = denomOCollprops.iterator();
		
		// Verify the internal properties
		OComplexObject denomColl1Entry1 = denomOCollIt.next();
		List<OProperty<?>> denomColl1Props = denomColl1Entry1.getProperties();
		assertTrue(denomColl1Props.get(0).getType().isSimple());
		assertEquals("DrDenom", denomColl1Props.get(0).getName());
		assertEquals("", denomColl1Props.get(0).getValue());
		assertTrue(denomColl1Props.get(1).getType().isSimple());
		assertEquals("DrUnit", denomColl1Props.get(1).getName());
		assertEquals("", denomColl1Props.get(1).getValue());
		
		OProperty<?> waiveCharge = entity.getProperty("WaiveCharges");
		assertTrue(waiveCharge.getType().isSimple());
		assertEquals("YES", waiveCharge.getValue());
	}
	
	
	@Test
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void testEntryRepAsBag() throws Exception {
		ResourceStateMachine rsm = getResourceStateMachine(TELLER_ENTITY_NAME);
		GenericEntity<EntityResource<OEntity>> ge = new GenericEntity<EntityResource<OEntity>>(new EntityResource<OEntity>(null)) {};

		//AtomXMLProvider ap = new AtomXMLProvider(edmMetadata, metadata, rsm, new OEntityTransformer());
		AtomXMLProvider ap = getAtomXMLProvider(metadata, rsm);
		UriInfo mockUriInfo = mock(UriInfo.class);
		when(mockUriInfo.getBaseUri()).thenReturn(new URI("http://www.temenos.com/rest.svc/"));
		when(mockUriInfo.getPath()).thenReturn("/" + TELLER_ENTITY_SETNAME);
		MultivaluedMap<String, String> mockPathParameters = new MultivaluedMapImpl<String>();
		mockPathParameters.add("id", "2");
		when(mockUriInfo.getPathParameters()).thenReturn(mockPathParameters);
		ap.setUriInfo(mockUriInfo);
		Request requestContext = mock(Request.class);
		when(requestContext.getMethod()).thenReturn("GET");
		ap.setRequestContext(requestContext);
		
		EntityResource<OEntity> result = ap.readFrom(RESTResource.class, ge.getType(), null, MediaType.APPLICATION_ATOM_XML_TYPE, null, readInputStream(POPULATED_TELLER_ENTRY));
		assertNotNull(result);
		
		OEntity entity = result.getEntity();
		assertNotNull (entity);
		
		// Now verify the properties populated from collection
		OProperty<?> denomProp = entity.getProperty("Teller_Cashinl_DrDenomMvGroup");
		EdmType denomPropType = denomProp.getType();
		assertFalse(denomPropType.isSimple());
		assertEquals("List(hothouse-modelsModel.Teller_Cashinl_DrDenomMvGroup)", denomPropType.getFullyQualifiedTypeName());
		EdmCollectionType denomPropCollType = (EdmCollectionType) denomPropType;
		assertEquals("hothouse-modelsModel.Teller_Cashinl_DrDenomMvGroup", denomPropCollType.getItemType().getFullyQualifiedTypeName());
		assertEquals(CollectionKind.List, denomPropCollType.getCollectionKind());
		OCollection<OComplexObject> denomOCollprops = (OCollection) denomProp.getValue();
		assertEquals(2, denomOCollprops.size());
		Iterator<OComplexObject> denomOCollIt = denomOCollprops.iterator();
		
			// Verify the internal properties
			OComplexObject denomColl1Entry1 = denomOCollIt.next();
			List<OProperty<?>> denomColl1Props = denomColl1Entry1.getProperties();
			assertTrue(denomColl1Props.get(0).getType().isSimple());
			assertEquals("DrDenom", denomColl1Props.get(0).getName());
			assertEquals("USD100", denomColl1Props.get(0).getValue());
			assertTrue(denomColl1Props.get(1).getType().isSimple());
			assertEquals("DrUnit", denomColl1Props.get(1).getName());
			assertEquals("9", denomColl1Props.get(1).getValue());
			
	
			OComplexObject denomColl1Entry2 = denomOCollIt.next();
			List<OProperty<?>> denomColl2Props = denomColl1Entry2.getProperties();
			assertTrue(denomColl2Props.get(0).getType().isSimple());
			assertEquals("DrDenom", denomColl2Props.get(0).getName());
			assertEquals("USD10", denomColl2Props.get(0).getValue());
			assertTrue(denomColl2Props.get(1).getType().isSimple());
			assertEquals("DrUnit", denomColl2Props.get(1).getName());
			assertEquals("10", denomColl2Props.get(1).getValue());
	
		OProperty<?> waiveCharge = entity.getProperty("WaiveCharges");
		assertTrue(waiveCharge.getType().isSimple());
		assertEquals("YES", waiveCharge.getValue());
		
		OProperty<?> narrative = entity.getProperty("Teller_Cashinl_Narrative2MvGroup");
		EdmType narrtivePropType = narrative.getType();
		assertFalse(narrtivePropType.isSimple());
		assertEquals("List(hothouse-modelsModel.Teller_Cashinl_Narrative2MvGroup)", narrtivePropType.getFullyQualifiedTypeName());
		EdmCollectionType narrativePropCollType = (EdmCollectionType) narrtivePropType;
		assertEquals("hothouse-modelsModel.Teller_Cashinl_Narrative2MvGroup", narrativePropCollType.getItemType().getFullyQualifiedTypeName());
		assertEquals(CollectionKind.List, narrativePropCollType.getCollectionKind());
		OCollection<OComplexObject> narrativeOCollprops = (OCollection) narrative.getValue();
		assertEquals(2, narrativeOCollprops.size());
		Iterator<OComplexObject> narrativeOCollIt = narrativeOCollprops.iterator();
		
			// Verify the internal properties
			OComplexObject narrativeColl1Entry1 = narrativeOCollIt.next();
			List<OProperty<?>> narrativeColl1Props = narrativeColl1Entry1.getProperties();
			assertTrue(narrativeColl1Props.get(0).getType().isSimple());
			assertEquals("Narrative2", narrativeColl1Props.get(0).getName());
			assertEquals("Narrative2 Value 1", narrativeColl1Props.get(0).getValue());
			
			OComplexObject narrativeColl1Entry2 = narrativeOCollIt.next();
			List<OProperty<?>> narrativeColl2Props = narrativeColl1Entry2.getProperties();
			assertTrue(narrativeColl2Props.get(0).getType().isSimple());
			assertEquals("Narrative2", narrativeColl2Props.get(0).getName());
			assertEquals("Narrative2 Value 2", narrativeColl2Props.get(0).getValue());
		
		OProperty<?> account2 = entity.getProperty("Account2");
		assertTrue(account2.getType().isSimple());
		assertEquals("60259", account2.getValue());
	}
	
	@Test
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void testEntryRepAsCollection() throws Exception {
		ResourceStateMachine rsm = getResourceStateMachine(TELLER_ENTITY_NAME);
		GenericEntity<EntityResource<OEntity>> ge = new GenericEntity<EntityResource<OEntity>>(new EntityResource<OEntity>(null)) {};
		//AtomXMLProvider ap = new AtomXMLProvider(edmMetadata, metadata, rsm, new OEntityTransformer());
		AtomXMLProvider ap = getAtomXMLProvider(metadata, rsm);
		
		UriInfo mockUriInfo = mock(UriInfo.class);
		when(mockUriInfo.getBaseUri()).thenReturn(new URI("http://www.temenos.com/rest.svc/"));
		when(mockUriInfo.getPath()).thenReturn("/" + TELLER_ENTITY_SETNAME);
		MultivaluedMap<String, String> mockPathParameters = new MultivaluedMapImpl<String>();
		mockPathParameters.add("id", "2");
		when(mockUriInfo.getPathParameters()).thenReturn(mockPathParameters);
		ap.setUriInfo(mockUriInfo);
		Request requestContext = mock(Request.class);
		when(requestContext.getMethod()).thenReturn("GET");
		ap.setRequestContext(requestContext);
		
		EntityResource<OEntity> result = ap.readFrom(RESTResource.class, ge.getType(), null, MediaType.APPLICATION_ATOM_XML_TYPE, null, readInputStream(TELLER_ENTRY_REP_AS_COLLECTION));
		assertNotNull(result);
		
		OEntity entity = result.getEntity();
		assertNotNull (entity);
		
		// Now verify the properties populated from collection
		OProperty<?> denomProp = entity.getProperty("Teller_Cashinl_DrDenomMvGroup");
		EdmType denomPropType = denomProp.getType();
		assertFalse(denomPropType.isSimple());
		assertEquals("List(hothouse-modelsModel.Teller_Cashinl_DrDenomMvGroup)", denomPropType.getFullyQualifiedTypeName());
		EdmCollectionType denomPropCollType = (EdmCollectionType) denomPropType;
		assertEquals("hothouse-modelsModel.Teller_Cashinl_DrDenomMvGroup", denomPropCollType.getItemType().getFullyQualifiedTypeName());
		assertEquals(CollectionKind.List, denomPropCollType.getCollectionKind());
		OCollection<OComplexObject> denomOCollprops = (OCollection) denomProp.getValue();
		assertEquals(2, denomOCollprops.size());
		Iterator<OComplexObject> denomOCollIt = denomOCollprops.iterator();
		
			// Verify the internal properties
			OComplexObject denomColl1Entry1 = denomOCollIt.next();
			List<OProperty<?>> denomColl1Props = denomColl1Entry1.getProperties();
			assertTrue(denomColl1Props.get(0).getType().isSimple());
			assertEquals("DrDenom", denomColl1Props.get(0).getName());
			assertEquals("USD100", denomColl1Props.get(0).getValue());
			assertTrue(denomColl1Props.get(1).getType().isSimple());
			assertEquals("DrUnit", denomColl1Props.get(1).getName());
			assertEquals("9", denomColl1Props.get(1).getValue());
			
	
			OComplexObject denomColl1Entry2 = denomOCollIt.next();
			List<OProperty<?>> denomColl2Props = denomColl1Entry2.getProperties();
			assertTrue(denomColl2Props.get(0).getType().isSimple());
			assertEquals("DrDenom", denomColl2Props.get(0).getName());
			assertEquals("USD10", denomColl2Props.get(0).getValue());
			assertTrue(denomColl2Props.get(1).getType().isSimple());
			assertEquals("DrUnit", denomColl2Props.get(1).getName());
			assertEquals("10", denomColl2Props.get(1).getValue());
	
		OProperty<?> waiveCharge = entity.getProperty("WaiveCharges");
		assertTrue(waiveCharge.getType().isSimple());
		assertEquals("YES", waiveCharge.getValue());
		
		OProperty<?> narrative = entity.getProperty("Teller_Cashinl_Narrative2MvGroup");
		EdmType narrtivePropType = narrative.getType();
		assertFalse(narrtivePropType.isSimple());
		assertEquals("List(hothouse-modelsModel.Teller_Cashinl_Narrative2MvGroup)", narrtivePropType.getFullyQualifiedTypeName());
		EdmCollectionType narrativePropCollType = (EdmCollectionType) narrtivePropType;
		assertEquals("hothouse-modelsModel.Teller_Cashinl_Narrative2MvGroup", narrativePropCollType.getItemType().getFullyQualifiedTypeName());
		assertEquals(CollectionKind.List, narrativePropCollType.getCollectionKind());
		OCollection<OComplexObject> narrativeOCollprops = (OCollection) narrative.getValue();
		assertEquals(2, narrativeOCollprops.size());
		Iterator<OComplexObject> narrativeOCollIt = narrativeOCollprops.iterator();
		
			// Verify the internal properties
			OComplexObject narrativeColl1Entry1 = narrativeOCollIt.next();
			List<OProperty<?>> narrativeColl1Props = narrativeColl1Entry1.getProperties();
			assertTrue(narrativeColl1Props.get(0).getType().isSimple());
			assertEquals("Narrative2", narrativeColl1Props.get(0).getName());
			assertEquals("Narrative2 Value 1", narrativeColl1Props.get(0).getValue());
			
			OComplexObject narrativeColl1Entry2 = narrativeOCollIt.next();
			List<OProperty<?>> narrativeColl2Props = narrativeColl1Entry2.getProperties();
			assertTrue(narrativeColl2Props.get(0).getType().isSimple());
			assertEquals("Narrative2", narrativeColl2Props.get(0).getName());
			assertEquals("Narrative2 Value 2", narrativeColl2Props.get(0).getValue());
		
		OProperty<?> account2 = entity.getProperty("Account2");
		assertTrue(account2.getType().isSimple());
		assertEquals("60259", account2.getValue());
	}
	
	@Test
	public void testCustomerNestedEntry() throws Exception {
		// Prepare Customer OEntity with nested collections
		OEntity origEntity = getCustomerEntity();
		org.odata4j.format.Entry newEntry = createRequestEntry(edmMetadata.findEdmEntitySet(CUSTOMER_ENTITY_SETNAME), origEntity);
		
		// Lets just convert into an XML Representation
		FormatWriter<org.odata4j.format.Entry> writer = FormatWriterFactory.getFormatWriter(org.odata4j.format.Entry.class, null, null, null);
        StringWriter sw = new StringWriter();
        writer.write(null, sw, newEntry);
        String origEntryStr = sw.toString();
        System.out.println("Original Entry:" + origEntryStr);
		
        // Now verify the properties populated from OEntity
		ResourceState initial = new ResourceState("ServiceDocument", "ServiceDocument", new ArrayList<Action>(), "/");
		initial.addTransition(new Transition.Builder().method(HttpMethod.GET)
				.target(new ResourceState(CUSTOMER_ENTITY_NAME, CUSTOMER_ENTITY_SETNAME, new ArrayList<Action>(), "/" + CUSTOMER_ENTITY_SETNAME))
				.build());
		ResourceStateMachine rsm = new ResourceStateMachine(initial);

		GenericEntity<EntityResource<OEntity>> ge = new GenericEntity<EntityResource<OEntity>>(new EntityResource<OEntity>(null)) {};
		AtomXMLProvider ap = new AtomXMLProvider(edmMetadata, metadata, rsm, new OEntityTransformer());
		
		UriInfo mockUriInfo = mock(UriInfo.class);
		when(mockUriInfo.getBaseUri()).thenReturn(new URI("http://www.temenos.com/rest.svc/"));
		when(mockUriInfo.getPath()).thenReturn("/" + CUSTOMER_ENTITY_SETNAME);
		MultivaluedMap<String, String> mockPathParameters = new MultivaluedMapImpl<String>();
		mockPathParameters.add("id", "2");
		when(mockUriInfo.getPathParameters()).thenReturn(mockPathParameters);
		ap.setUriInfo(mockUriInfo);
		Request requestContext = mock(Request.class);
		when(requestContext.getMethod()).thenReturn("GET");
		ap.setRequestContext(requestContext);
		
		EntityResource<OEntity> result = ap.readFrom(RESTResource.class, ge.getType(), null, MediaType.APPLICATION_ATOM_XML_TYPE, null, new ByteArrayInputStream(origEntryStr.getBytes()));
		assertNotNull(result);
		
		OEntity retEntity = result.getEntity();
		assertNotNull (retEntity);
        
		assertEquals(origEntity.getProperties().size(), retEntity.getProperties().size());
	}
	
	/*
	 * Load the xml as Input stream
	 */
	private InputStream readInputStream(String fileName) {
		InputStream is = this.getClass().getClassLoader().getResourceAsStream(fileName);
		return is;
	}
	
	/*
	 * Create Entry using OEntity
	 */
	protected org.odata4j.format.Entry createRequestEntry(EdmEntitySet entitySet, final OEntity oentity) {
		return new org.odata4j.format.Entry() {

			@Override
			public String getUri() {
				return null;
			}

			@Override
			public OEntity getEntity() {
				return oentity;
			}
		};
	}
	
	/*
	 *  Generic method to generate EdmDataService for resources for tests
	 */
	private static EdmDataServices getEdmDataServices(Metadata metadata, String... collOfResource) {
		return (new MetadataOData4j(metadata, getResourceStateMachine(collOfResource)).getMetadata());
	} 
	
	private static ResourceStateMachine getResourceStateMachine(String... collOfResource) {
		ResourceState initial = new ResourceState("Initial", "ServiceDocument", new ArrayList<Action>(), "/", null, new UriSpecification("Initial", "/"));
		for (String resourceName : collOfResource) {
			CollectionResourceState resourceType = new CollectionResourceState(resourceName, resourceName, new ArrayList<Action>(), "/" + resourceName +"s", null, null);
			initial.addTransition(new Transition.Builder().method(HttpMethod.GET).target(resourceType).build());
		}
		ResourceStateMachine hypermediaEngine = new ResourceStateMachine(initial);
		return hypermediaEngine;
	}
	
	/*
	 * Prepare Customer OEntity with nested collections
	 */
	private OEntity getCustomerEntity() {
		EdmEntitySet entitySet = edmMetadata.findEdmEntitySet(CUSTOMER_ENTITY_NAME);
		EdmType type = entitySet.getType();
		EdmEntityType entityType = (EdmEntityType) type;
		
		List<OProperty<?>> properties = new ArrayList<OProperty<?>>();
		
		// Simple Properties
		properties.add(OProperties.string("name", "CustomerID"));
		properties.add(OProperties.datetime("dateOfBirth", new Date()));
		properties.add(OProperties.string("sector", "Agriculture"));
		properties.add(OProperties.string("industry", "Banking"));
		properties.add(OProperties.boolean_("loyal", true));
		properties.add(OProperties.int64("loyal_rating", new Long(10)));
		
		// Lets build up Address Complex Property now
		EdmProperty addProp = entityType.findProperty("Customer_address");
		assertFalse(addProp.getType().isSimple());
		EdmComplexType addType = (EdmComplexType) addProp.getType();
		OCollection.Builder<OObject> addCollBuilder = OCollections.newBuilder( addType );
		
		// Address 1
		List<OProperty<?>> add1Props = new ArrayList<OProperty<?>>();
		add1Props.add(OProperties.int64("number", new Long(2)));
		
			// Nested Street Complex Type
			EdmProperty streetProp = addType.findProperty("Customer_street");
			assertFalse(streetProp.getType().isSimple());
			EdmComplexType streetType = (EdmComplexType) streetProp.getType();
			OCollection.Builder<OObject> strCollBuilder = OCollections.newBuilder( streetType );
			// Street 1
			List<OProperty<?>> str1Props = new ArrayList<OProperty<?>>();
			str1Props.add(OProperties.string("streetType", "Peoples Building"));
			strCollBuilder.add((OObject) OComplexObjects.create(streetType, str1Props));
			// Street 2
			List<OProperty<?>> str2Props = new ArrayList<OProperty<?>>();
			str2Props.add(OProperties.string("streetType", "Maylands Avenue"));
			strCollBuilder.add((OObject) OComplexObjects.create(streetType, str2Props));
		
			OCollection<? extends OObject> add1StreetBag = strCollBuilder.build();
			OProperty<?> add1StreetOp = OProperties.collection( 
										streetProp.getName(), 
										new EdmCollectionType( EdmProperty.CollectionKind.List, streetType ), 
										add1StreetBag);
			
		add1Props.add(add1StreetOp);
		add1Props.add(OProperties.string("town", "Hemel Hempstead"));
		add1Props.add(OProperties.string("postCode", "HP2 4NW"));
		addCollBuilder.add((OObject) OComplexObjects.create(addType, add1Props));
		
		// Address 2
		List<OProperty<?>> add2Props = new ArrayList<OProperty<?>>();
		add2Props.add(OProperties.int64("number", new Long(29)));
		
			// Nested Street Complex Type
			OCollection.Builder<OObject> str2CollBuilder = OCollections.newBuilder( streetType );
			// Street 1
			List<OProperty<?>> str21Props = new ArrayList<OProperty<?>>();
			str21Props.add(OProperties.string("streetType", "Belsize Road"));
			str2CollBuilder.add((OObject) OComplexObjects.create(streetType, str21Props));
			
			OCollection<? extends OObject> add2StreetBag = str2CollBuilder.build();
			OProperty<?> add2StreetOp = OProperties.collection( 
										streetProp.getName(), 
										new EdmCollectionType( EdmProperty.CollectionKind.List, streetType ), 
										add2StreetBag);
		add2Props.add(add2StreetOp);
		add2Props.add(OProperties.string("town", "Hemel Hempstead"));
		add2Props.add(OProperties.string("postCode", "HP3 8DJ"));
		addCollBuilder.add((OObject) OComplexObjects.create(addType, add2Props));
				
		OCollection<? extends OObject> addBag = addCollBuilder.build();
		OProperty<?> addOp = OProperties.collection( 
									addProp.getName(), 
									new EdmCollectionType( EdmProperty.CollectionKind.List, addType), 
									addBag);
		properties.add(addOp);
		
		OEntity entity = OEntities.create(entitySet, OEntityKey.create("CustomerID"), properties, null);
		return entity;
	}
}
