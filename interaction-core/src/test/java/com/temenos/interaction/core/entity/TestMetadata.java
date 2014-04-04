package com.temenos.interaction.core.entity;

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


import java.util.Date;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import org.joda.time.DateTimeZone;
import org.joda.time.LocalTime;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.temenos.interaction.core.entity.vocabulary.TermFactory;
import com.temenos.interaction.core.entity.vocabulary.Vocabulary;
import com.temenos.interaction.core.entity.vocabulary.terms.TermComplexGroup;
import com.temenos.interaction.core.entity.vocabulary.terms.TermComplexType;
import com.temenos.interaction.core.entity.vocabulary.terms.TermIdField;
import com.temenos.interaction.core.entity.vocabulary.terms.TermMandatory;
import com.temenos.interaction.core.entity.vocabulary.terms.TermValueType;
import com.temenos.interaction.core.resource.ResourceMetadataManager;

public class TestMetadata {

	private static Metadata metadata;
	private static EntityMetadata vocs;
	
	
	@BeforeClass
	public static void setup()
	{
		//Define vocabulary for this entity
		metadata = new Metadata("Customers");
		vocs = new EntityMetadata("Customer");
				
		Vocabulary vocName = new Vocabulary();
		vocName.setTerm(new TermComplexType(false));
		vocName.setTerm(new TermIdField(true));
		vocs.setPropertyVocabulary("name", vocName);
		
		Vocabulary vocAddress = new Vocabulary();
		vocAddress.setTerm(new TermComplexType(true));
		vocs.setPropertyVocabulary("address", vocAddress);

		Stack<String> collectionNames = new Stack<String>();
		collectionNames.push("address");
		
		Vocabulary vocNumbert = new Vocabulary();
		vocNumbert.setTerm(new TermComplexGroup("address"));
		vocNumbert.setTerm(new TermValueType(TermValueType.INTEGER_NUMBER));
		vocs.setPropertyVocabulary("number", vocNumbert, collectionNames.elements());
		
		Vocabulary vocStreet = new Vocabulary();
		vocStreet.setTerm(new TermComplexGroup("address"));
		vocStreet.setTerm(new TermValueType(TermValueType.TEXT));
		vocs.setPropertyVocabulary("street", vocStreet, collectionNames.elements());
		
		Vocabulary vocTown = new Vocabulary();
		vocTown.setTerm(new TermComplexGroup("address"));
		vocTown.setTerm(new TermValueType(TermValueType.TEXT));
		vocs.setPropertyVocabulary("town", vocStreet, collectionNames.elements());
		
		Vocabulary vocPostCode = new Vocabulary();
		vocPostCode.setTerm(new TermComplexGroup("address"));
		vocPostCode.setTerm(new TermValueType(TermValueType.TEXT));
		vocs.setPropertyVocabulary("postCode", vocPostCode, collectionNames.elements());
		
		Vocabulary vocDob = new Vocabulary();
		vocDob.setTerm(new TermComplexType(false));
		vocDob.setTerm(new TermValueType(TermValueType.DATE));
		vocDob.setTerm(new TermMandatory(true));
		vocs.setPropertyVocabulary("dateOfBirth", vocDob);

		Vocabulary vocTime = new Vocabulary();
		vocTime.setTerm(new TermComplexType(false));
		vocTime.setTerm(new TermValueType(TermValueType.TIME));
		vocs.setPropertyVocabulary("time", vocTime);

		Vocabulary vocSector = new Vocabulary();
		vocSector.setTerm(new TermComplexType(false));
		vocs.setPropertyVocabulary("sector", vocSector);
		
		Vocabulary vocIndustry = new Vocabulary();
		vocIndustry.setTerm(new TermComplexType(false));
		vocs.setPropertyVocabulary("industry", vocIndustry);
		
		metadata.setEntityMetadata(vocs);
	}
	
	@Test
	public void testPropertyVocabularyKeySet()
	{	
		Set<String> propertyKeys = vocs.getPropertyVocabularyKeySet();
		Assert.assertEquals(10, propertyKeys.size());
		Assert.assertTrue(propertyKeys.contains("name"));
		Assert.assertTrue(propertyKeys.contains("address"));
		Assert.assertTrue(propertyKeys.contains("address.number"));
		Assert.assertTrue(propertyKeys.contains("address.street"));
		Assert.assertTrue(propertyKeys.contains("address.town"));
		Assert.assertTrue(propertyKeys.contains("address.postCode"));
		Assert.assertTrue(propertyKeys.contains("dateOfBirth"));
		Assert.assertTrue(propertyKeys.contains("sector"));
		Assert.assertTrue(propertyKeys.contains("industry"));
	}
	
	@Test
	public void testIsPropertyComplex()
	{		
		Assert.assertFalse(vocs.isPropertyComplex("name"));
		Assert.assertTrue(vocs.isPropertyComplex("address"));
		Assert.assertFalse(vocs.isPropertyComplex("address.number"));
		Assert.assertFalse(vocs.isPropertyComplex("address.street"));
		Assert.assertFalse(vocs.isPropertyComplex("address.town"));
		Assert.assertFalse(vocs.isPropertyComplex("address.postCode"));
		Assert.assertFalse(vocs.isPropertyComplex("dateOfBirth"));
		Assert.assertFalse(vocs.isPropertyComplex("sector"));
		Assert.assertFalse(vocs.isPropertyComplex("industry"));
	}
	
	@Test
	public void testGetComplexGroup()
	{		
		Assert.assertEquals("", vocs.getPropertyComplexGroup("name"));
		Assert.assertEquals("", vocs.getPropertyComplexGroup("address"));
		Assert.assertEquals("address", vocs.getPropertyComplexGroup("address.number"));
		Assert.assertEquals("address", vocs.getPropertyComplexGroup("address.street"));
		Assert.assertEquals("address", vocs.getPropertyComplexGroup("address.town"));
		Assert.assertEquals("address", vocs.getPropertyComplexGroup("address.postCode"));
		Assert.assertEquals("", vocs.getPropertyComplexGroup("dateOfBirth"));
		Assert.assertEquals("", vocs.getPropertyComplexGroup("sector"));
		Assert.assertEquals("", vocs.getPropertyComplexGroup("industry"));
	}
	
	@Test
	public void testIsPropertyText()
	{		
		Assert.assertTrue(vocs.isPropertyText("name"));
		Assert.assertTrue(vocs.isPropertyText("address"));
		Assert.assertFalse(vocs.isPropertyText("address.number"));
		Assert.assertTrue(vocs.isPropertyText("address.street"));
		Assert.assertTrue(vocs.isPropertyText("address.town"));
		Assert.assertTrue(vocs.isPropertyText("address.postCode"));
		Assert.assertFalse(vocs.isPropertyText("dateOfBirth"));
		Assert.assertTrue(vocs.isPropertyText("sector"));
		Assert.assertTrue(vocs.isPropertyText("industry"));
	}
	
	@Test
	public void testIsPropertyNumber()
	{		
		Assert.assertFalse(vocs.isPropertyNumber("name"));
		Assert.assertFalse(vocs.isPropertyNumber("address"));
		Assert.assertTrue(vocs.isPropertyNumber("address.number"));
		Assert.assertFalse(vocs.isPropertyNumber("address.street"));
		Assert.assertFalse(vocs.isPropertyNumber("address.town"));
		Assert.assertFalse(vocs.isPropertyNumber("address.postCode"));
		Assert.assertFalse(vocs.isPropertyNumber("dateOfBirth"));
		Assert.assertFalse(vocs.isPropertyNumber("sector"));
		Assert.assertFalse(vocs.isPropertyNumber("industry"));
	}
	
	@Test
	public void testIdFields() {
		Assert.assertEquals(1, vocs.getIdFields().size());
		Assert.assertEquals("name", vocs.getIdFields().get(0));
	}

	@Test
	public void testCreateEmptyEntityProperty() {
		Assert.assertEquals(null, vocs.createEmptyEntityProperty("address.number").getValue());
		Assert.assertEquals("", vocs.createEmptyEntityProperty("name").getValue());
		Assert.assertTrue(vocs.createEmptyEntityProperty("dateOfBirth").getValue() instanceof Date);
		Assert.assertEquals(null, vocs.createEmptyEntityProperty("industry").getValue());
	}
	
	@Test
	public void testTimePropertyAsString() {
		Assert.assertEquals("00:00:00.001", vocs.getPropertyValueAsString("time", new LocalTime(1, DateTimeZone.UTC)));
	}
	
	@Test
	public void testEntityList() {
		String entity1 = "CountryList";
		String entity2 = "CustomerInfo";
		TermFactory termFactory = new TermFactory();
		ResourceMetadataManager rmManager = new ResourceMetadataManager(termFactory);
		Metadata metadata = new Metadata(rmManager);
		EntityMetadata em1 = metadata.getEntityMetadata(entity1);
		EntityMetadata em2 = metadata.getEntityMetadata(entity2);
		
		Assert.assertEquals("CountryList", em1.getEntityName().toString());
		Assert.assertEquals("CustomerInfo", em2.getEntityName().toString());
	}
	
	@Test
	public void testAllEntity() {
		String entity1 = "CountryList";
		String entity2 = "CustomerInfo";
		TermFactory termFactory = new TermFactory();
		ResourceMetadataManager rmManager = new ResourceMetadataManager(termFactory);
		Metadata metadata = new Metadata(rmManager);
		Map<String, EntityMetadata> mapEntity = metadata.getEntitiesMetadata();
		
		Assert.assertTrue(mapEntity.isEmpty());
		
		EntityMetadata em1 = metadata.getEntityMetadata(entity1);
		EntityMetadata em2 = metadata.getEntityMetadata(entity2);
		
		Assert.assertEquals("CountryList", em1.getEntityName().toString());
		Assert.assertEquals("CustomerInfo", em2.getEntityName().toString());

		Assert.assertTrue(mapEntity.containsKey(entity1));
		Assert.assertTrue(mapEntity.containsKey(entity2));
	}

}
