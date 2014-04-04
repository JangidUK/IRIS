package com.temenos.interaction.rimdsl;

/*
 * #%L
 * com.temenos.interaction.rimdsl.tests
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

import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.xtext.generator.IGenerator;
import org.eclipse.xtext.junit4.InjectWith;
import org.eclipse.xtext.junit4.XtextRunner;
import org.eclipse.xtext.resource.SaveOptions;
import org.eclipse.xtext.resource.XtextResourceSet;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.temenos.interaction.rimdsl.rim.CommandFactory;
import com.temenos.interaction.rimdsl.rim.DomainModel;
import com.temenos.interaction.rimdsl.rim.Entity;
import com.temenos.interaction.rimdsl.rim.EventFactory;
import com.temenos.interaction.rimdsl.rim.ImplRef;
import com.temenos.interaction.rimdsl.rim.ResourceInteractionModel;
import com.temenos.interaction.rimdsl.rim.ResourceType;
import com.temenos.interaction.rimdsl.rim.RimFactory;
import com.temenos.interaction.rimdsl.rim.State;
import com.temenos.interaction.rimdsl.rim.Title;
import com.temenos.interaction.rimdsl.rim.TransitionForEach;
import com.temenos.interaction.rimdsl.rim.TransitionRef;
import com.temenos.interaction.rimdsl.rim.TransitionSpec;
import com.temenos.interaction.rimdsl.rim.UriLink;
import com.temenos.interaction.rimdsl.rim.UriLinkage;

@InjectWith(RIMDslInjectorProvider.class)
@RunWith(XtextRunner.class)
public class SerializerTest {

	private final static String LINE_SEP = System.getProperty("line.separator");

	@Inject 
	IGenerator generator;
	
	@Inject
	private Provider<XtextResourceSet> provider;
	 
	private XtextResourceSet resourceSet;
	
	@Before
	public void setup() throws Exception {
	    // creates the resource set to use for this test
	    resourceSet = provider.get();
	}
	
	@Test
	public void testSerializeSimple() throws Exception {
		DomainModel domainModel = RimFactory.eINSTANCE.createDomainModel();
		
		// rim Test {}
		ResourceInteractionModel rim = RimFactory.eINSTANCE.createResourceInteractionModel();
		rim.setName("Test");
		domainModel.getRims().add(rim);

		String output = saveRimFromDomain(domainModel);
		assertEquals("rim Test {"+LINE_SEP+"}", output);
	}

	private final static String RIM_WITH_TRANSITION_TITLE = "" +
			"rim Test {" + LINE_SEP +
			"	event GET { method: GET }" + LINE_SEP +
			"	command GETEntities" + LINE_SEP +
			"	command GETEntity" + LINE_SEP +
			"" + LINE_SEP +
			"	resource A {" + LINE_SEP +
			"		type: collection" + LINE_SEP +
			"		entity: Entity" + LINE_SEP +
			"		view: GETEntities" + LINE_SEP +
			"		GET *-> B {" + LINE_SEP +
			"			title: \"Aaron\\'s transition to B\"" + LINE_SEP +
			"		}" + LINE_SEP +
			"	}" + LINE_SEP +
			"" + LINE_SEP +
			"	resource B {" + LINE_SEP +
			"		type: item" + LINE_SEP +
			"		entity: Entity" + LINE_SEP +
			"		view: GETEntity" + LINE_SEP +
			"	}" + LINE_SEP +
			"" + LINE_SEP +
			"}" +
			""
	;

	@Test
	public void testSerializeTransitionWithTitle() throws Exception {
		DomainModel domainModel = RimFactory.eINSTANCE.createDomainModel();
		
		// rim Test {}
		ResourceInteractionModel rim = RimFactory.eINSTANCE.createResourceInteractionModel();
		rim.setName("Test");
		domainModel.getRims().add(rim);

		// events
		EventFactory eventFactory = new EventFactory(rim.getEvents());
		// commands
		CommandFactory commandFactory = new CommandFactory(rim.getCommands());
		
		// resource A {}
		State A = RimFactory.eINSTANCE.createState();
		A.setName("A");
		A.setEntity(createEntity("Entity"));
		A.setType(createResourceType(true));
		ImplRef AImpl = RimFactory.eINSTANCE.createImplRef();
		AImpl.setView(commandFactory.createResourceCommand("GETEntities", null));
		A.setImpl(AImpl);
		rim.getStates().add(A);
		
		// resource B {}
		State B = RimFactory.eINSTANCE.createState();
		B.setName("B");
		B.setEntity(createEntity("Entity"));
		B.setType(createResourceType(false));
		ImplRef BImpl = RimFactory.eINSTANCE.createImplRef();
		BImpl.setView(commandFactory.createResourceCommand("GETEntity", null));
		B.setImpl(BImpl);
		rim.getStates().add(B);
		
		// transition from A *-> B
		A.getTransitions().add(createTransitionForEach(eventFactory, B, "Aaron's transition to B", null));
		
		String output = saveRimFromDomain(domainModel);
		assertEquals(RIM_WITH_TRANSITION_TITLE, output);
	}

	private final static String RIM_WITH_TRANSITION_PARAMETERS = "" +
			"rim Test {" + LINE_SEP +
			"	event GET { method: GET }" + LINE_SEP +
			"	command GETEntities" + LINE_SEP +
			"	command GETEntity" + LINE_SEP +
			"" + LINE_SEP +
			"	resource A {" + LINE_SEP +
			"		type: collection" + LINE_SEP +
			"		entity: Entity" + LINE_SEP +
			"		view: GETEntities" + LINE_SEP +
			"		GET *-> B {" + LINE_SEP +
			"			parameters [ filter = \"CustomerCode eq '{Cno}'\" ]" + LINE_SEP +
			"		}" + LINE_SEP +
			"	}" + LINE_SEP +
			"" + LINE_SEP +
			"	resource B {" + LINE_SEP +
			"		type: item" + LINE_SEP +
			"		entity: Entity" + LINE_SEP +
			"		view: GETEntity" + LINE_SEP +
			"	}" + LINE_SEP +
			"" + LINE_SEP +
			"}" +
			""
	;
	
	@Test
	public void testSerializeWithTransitionParameters() throws Exception {
		DomainModel domainModel = RimFactory.eINSTANCE.createDomainModel();
		
		// rim Test {}
		ResourceInteractionModel rim = RimFactory.eINSTANCE.createResourceInteractionModel();
		rim.setName("Test");
		domainModel.getRims().add(rim);

		// events
		EventFactory eventFactory = new EventFactory(rim.getEvents());
		// commands
		CommandFactory commandFactory = new CommandFactory(rim.getCommands());
		
		// resource A {}
		State A = RimFactory.eINSTANCE.createState();
		A.setName("A");
		A.setEntity(createEntity("Entity"));
		A.setType(createResourceType(true));
		ImplRef AImpl = RimFactory.eINSTANCE.createImplRef();
		AImpl.setView(commandFactory.createResourceCommand("GETEntities", null));
		A.setImpl(AImpl);
		rim.getStates().add(A);
		
		// resource B {}
		State B = RimFactory.eINSTANCE.createState();
		B.setName("B");
		B.setEntity(createEntity("Entity"));
		B.setType(createResourceType(false));
		ImplRef BImpl = RimFactory.eINSTANCE.createImplRef();
		BImpl.setView(commandFactory.createResourceCommand("GETEntity", null));
		B.setImpl(BImpl);
		rim.getStates().add(B);
		
		// transition from A *-> B
		Map<String,String> parameters = new HashMap<String,String>();
		parameters.put("filter", "CustomerCode eq '{Cno}'");
		A.getTransitions().add(createTransitionForEach(eventFactory, B, null, parameters));
		
		String output = saveRimFromDomain(domainModel);
		assertEquals(RIM_WITH_TRANSITION_PARAMETERS, output);
	}

	private String saveRimFromDomain(DomainModel rim) throws Exception {
		Resource r = resourceSet.createResource(URI.createURI("fake.rim"));
		r.getContents().add(rim);
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		r.save(baos, SaveOptions.defaultOptions().toOptionsMap());
		String content = baos.toString("UTF-8");
		return content;
	}

	private ResourceType createResourceType(boolean collection) {
		ResourceType resourceType = RimFactory.eINSTANCE.createResourceType();
		resourceType.setIsItem(!collection);
		resourceType.setIsCollection(collection);
		return resourceType;
	}

	private Entity createEntity(String entityName) {
		Entity entity = RimFactory.eINSTANCE.createEntity();
		entity.setName(entityName);
		return entity;
	}

	private TransitionRef createTransitionForEach(EventFactory factory, State target, String titleStr, Map<String,String> parameters) {
		TransitionForEach transition = RimFactory.eINSTANCE.createTransitionForEach();
		transition.setEvent(factory.createGET());
		transition.setState(target);
		
		TransitionSpec spec = RimFactory.eINSTANCE.createTransitionSpec();
		if (titleStr != null) {
			Title title = RimFactory.eINSTANCE.createTitle();
			title.setName(titleStr);
			spec.setTitle(title);
		}
		// add any parameters and conditions
		if (parameters != null) {
			for (String paramkey : parameters.keySet()) {
				UriLink uriParameter = RimFactory.eINSTANCE.createUriLink();
				uriParameter.setTemplateProperty(paramkey);
				UriLinkage entityProperty = RimFactory.eINSTANCE.createUriLinkage();
				entityProperty.setName(parameters.get(paramkey));
				uriParameter.setEntityProperty(entityProperty);
				spec.getUriLinks().add(uriParameter);
			}
		}
		transition.setSpec(spec);
		return transition;
	}

}
