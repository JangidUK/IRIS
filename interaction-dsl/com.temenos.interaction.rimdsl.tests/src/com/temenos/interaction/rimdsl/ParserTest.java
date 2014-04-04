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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.URL;
import java.util.List;

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.xtext.junit4.InjectWith;
import org.eclipse.xtext.junit4.XtextRunner;
import org.eclipse.xtext.junit4.util.ParseHelper;
import org.eclipse.xtext.resource.IEObjectDescription;
import org.eclipse.xtext.scoping.IScope;
import org.eclipse.xtext.scoping.IScopeProvider;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.google.common.base.Charsets;
import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.google.common.io.Resources;
import com.google.inject.Inject;
import com.temenos.interaction.rimdsl.rim.DomainDeclaration;
import com.temenos.interaction.rimdsl.rim.DomainModel;
import com.temenos.interaction.rimdsl.rim.NotFoundFunction;
import com.temenos.interaction.rimdsl.rim.OKFunction;
import com.temenos.interaction.rimdsl.rim.Relation;
import com.temenos.interaction.rimdsl.rim.RelationConstant;
import com.temenos.interaction.rimdsl.rim.RelationRef;
import com.temenos.interaction.rimdsl.rim.ResourceInteractionModel;
import com.temenos.interaction.rimdsl.rim.State;
import com.temenos.interaction.rimdsl.rim.Transition;
import com.temenos.interaction.rimdsl.rim.UriLink;

@InjectWith(RIMDslInjectorProvider.class)
@RunWith(XtextRunner.class)
public class ParserTest {

	@Inject
	ParseHelper<DomainModel> parser;
	@Inject
	private IScopeProvider scopeProvider;
	
	@Test 
	public void parseModel() throws Exception {
		DomainModel domainModel = parser.parse(loadTestRIM());
		ResourceInteractionModel model = (ResourceInteractionModel) domainModel.getRims().get(0);
		assertEquals(4, model.getStates().size());
	    assertEquals("demo_initial", model.getStates().get(0).getName());
	    assertEquals("demo_tickets", model.getStates().get(1).getName());
	    assertEquals("demo_aticket", model.getStates().get(2).getName());
	    assertEquals("demo_deleted", model.getStates().get(3).getName());
	}
	
	private String loadTestRIM() throws IOException {
		URL url = Resources.getResource("ParserTest.rim");
		String rim = Resources.toString(url, Charsets.UTF_8);
		return rim;
	}

	@Test 
	public void parseDomainModel() throws Exception {
		DomainModel domainModel = parser.parse(loadTestDomainRIM());
		EList<Resource.Diagnostic> errors = domainModel.eResource().getErrors();
		assertEquals(0, errors.size());
	}
	
	private String loadTestDomainRIM() throws IOException {
		URL url = Resources.getResource("TestDomain.rim");
		String rim = Resources.toString(url, Charsets.UTF_8);
		return rim;
	}


	private final static String LINE_SEP = System.getProperty("line.separator");
	
	private final static String SIMPLE_STATES_RIM = "" +
	"rim Simple {" + LINE_SEP +	
	"	command GetEntity" + LINE_SEP +
	"	command UpdateEntity" + LINE_SEP +
			
	"initial resource A {" + LINE_SEP +
	"	type: collection" + LINE_SEP +
	"	entity: ENTITY" + LINE_SEP +
	"	view: GetEntity" + LINE_SEP +
	"}" + LINE_SEP +

	"resource B {" + LINE_SEP +
	"	type: item" + LINE_SEP +
	"	entity: ENTITY" + LINE_SEP +
	"	actions [ UpdateEntity ]" + LINE_SEP +
	"}" + LINE_SEP +
	"}" + LINE_SEP +  // end rim
	"";

	@Test
	public void testParseSimpleStates() throws Exception {
		DomainModel domainModel = parser.parse(SIMPLE_STATES_RIM);
		ResourceInteractionModel model = (ResourceInteractionModel) domainModel.getRims().get(0);
		EList<Resource.Diagnostic> errors = model.eResource().getErrors();
		assertEquals(0, errors.size());
		
		// there should be exactly two states
		assertEquals(2, model.getStates().size());
	    assertEquals("A", model.getStates().get(0).getName());
	    assertEquals("B", model.getStates().get(1).getName());

	    // there should be no transitions between these states
	    State Astate = model.getStates().get(0);
	    assertEquals(0, Astate.getTransitions().size());
	    State Bstate = model.getStates().get(1);
	    assertEquals(0, Bstate.getTransitions().size());
	}

	private final static String SIMPLE_STATES_REORDERED_RIM = "" +
	"rim Simple {" + LINE_SEP +	
	"	command GetEntity" + LINE_SEP +
	"	command UpdateEntity" + LINE_SEP +
			
	"initial resource A {" + LINE_SEP +
	"	GET -> B" + LINE_SEP +
	"	entity: ENTITY" + LINE_SEP +
	"	view: GetEntity" + LINE_SEP +
	"	type: collection" + LINE_SEP +
	"}" + LINE_SEP +

	"resource B {" + LINE_SEP +
	"	entity: ENTITY" + LINE_SEP +
	"	actions [ UpdateEntity ]" + LINE_SEP +
	"	type: item" + LINE_SEP +
	"}" + LINE_SEP +
	"}" + LINE_SEP +  // end rim
	"";

	@Test
	public void testParseSimpleStatesReordered() throws Exception {
		DomainModel domainModel = parser.parse(SIMPLE_STATES_REORDERED_RIM);
		ResourceInteractionModel model = (ResourceInteractionModel) domainModel.getRims().get(0);
		EList<Resource.Diagnostic> errors = model.eResource().getErrors();
		assertEquals(0, errors.size());
		
		// there should be exactly two states
		assertEquals(2, model.getStates().size());
	    assertEquals("A", model.getStates().get(0).getName());
	    assertEquals("B", model.getStates().get(1).getName());

	    // there should be no transitions between these states
	    State Astate = model.getStates().get(0);
	    assertEquals(1, Astate.getTransitions().size());
	    assertEquals("B", Astate.getTransitions().get(0).getState().getName());
	    State Bstate = model.getStates().get(1);
	    assertEquals(0, Bstate.getTransitions().size());
	}

	private final static String SINGLE_STATE_VIEW_COMMAND_ONLY_RIM = "" +
	"rim Test {" + LINE_SEP +
	"	command GetEntity" + LINE_SEP +
			
	"initial resource A {" + LINE_SEP +
	"	type: collection" + LINE_SEP +
	"	entity: ENTITY" + LINE_SEP +
	"	view: GetEntity" + LINE_SEP +
	"}\r\n" + LINE_SEP +
	"}" + LINE_SEP +  // end rim
	"";

	@Test
	public void testParseSingleStateViewCommandOnly() throws Exception {
		DomainModel domainModel = parser.parse(SINGLE_STATE_VIEW_COMMAND_ONLY_RIM);
		ResourceInteractionModel model = (ResourceInteractionModel) domainModel.getRims().get(0);
		assertEquals(0, model.eResource().getErrors().size());
		
		// there should be exactly one states
		assertEquals(1, model.getStates().size());
	    assertEquals("A", model.getStates().get(0).getName());

	    // there should be a view for this state
	    assertNotNull(model.getStates().get(0).getImpl().getView());
	}
	
	private final static String SINGLE_STATE_NO_COMMANDS_RIM = "" +
	"rim Test {" + LINE_SEP +	
	"initial resource A {" + LINE_SEP +
	"	type: collection" + LINE_SEP +
	" 	entity: ENTITY" + LINE_SEP +
	"}\r\n" + LINE_SEP +
	"}" + LINE_SEP +  // end rim
	"";

	@Ignore
	@Test
	public void testParseSingleStateNoCommands() throws Exception {
		DomainModel domainModel = parser.parse(SINGLE_STATE_NO_COMMANDS_RIM);
		ResourceInteractionModel model = (ResourceInteractionModel) domainModel.getRims().get(0);
		
		// there should be exactly one state
		assertEquals(1, model.getStates().size());
	    assertEquals("A", model.getStates().get(0).getName());

	    // there should be an error indicating a problem with the missing actions
		EList<Resource.Diagnostic> errors = model.eResource().getErrors();
		assertEquals(1, errors.size());
	}

	private final static String SIMPLE_TRANSITION_RIM = "" +
			"rim Test {" + LINE_SEP +
			"	command GetEntity" + LINE_SEP +
			"	command GetEntities" + LINE_SEP +
					
			"initial resource A {" + LINE_SEP +
			"	type: collection" + LINE_SEP +
			"	entity: ENTITY" + LINE_SEP +
			"	view: GetEntities" + LINE_SEP +
			"	GET -> B" + LINE_SEP +
			"}" + LINE_SEP +

			"resource B {" +
			"	type: item" + LINE_SEP +
			"	entity: ENTITY" + LINE_SEP +
			"	view: GetEntity" + LINE_SEP +
			"}" + LINE_SEP +
			"}" + LINE_SEP +  // end rim
			"";

	@Test
	public void testParseStatesWithTransition() throws Exception {
		DomainModel domainModel = parser.parse(SIMPLE_TRANSITION_RIM);
		ResourceInteractionModel model = (ResourceInteractionModel) domainModel.getRims().get(0);
		assertEquals(0, model.eResource().getErrors().size());
		
		// there should be exactly two states
		assertEquals(2, model.getStates().size());
		State aState = model.getStates().get(0);
	    assertEquals("A", aState.getName());

	    // there should one transition from state A to state B
		assertEquals(1, aState.getTransitions().size());
		assertEquals("B", aState.getTransitions().get(0).getState().getName());
	}

	private final static String TRANSITION_WITH_URI_CHARACTERS_PARAMETERS_RIM = "" +
			"rim Test {" + LINE_SEP +
			"	command GetEntity" + LINE_SEP +
			"	command GetEntities" + LINE_SEP +
					
			"initial resource A {" + LINE_SEP +
			"	type: collection" + LINE_SEP +
			"	entity: ENTITY" + LINE_SEP +
			"	view: GetEntities" + LINE_SEP +
			"	GET -> B {" + LINE_SEP +
			"		parameters [ filter=\"CustomerCode eq '{Id}'\" ]" + LINE_SEP +
			"	}" + LINE_SEP +
			"}" + LINE_SEP +

			"resource B {" +
			"	type: item" + LINE_SEP +
			"	entity: ENTITY" + LINE_SEP +
			"	view: GetEntity {" + LINE_SEP +
			"		properties [ filter=\"{filter}\" ]" + LINE_SEP +
			"	}" + LINE_SEP +
			"}" + LINE_SEP +
			"}" + LINE_SEP +  // end rim
			"";

	@Test
	public void testParseStatesWithTransitionUriCharactersParameters() throws Exception {
		DomainModel domainModel = parser.parse(TRANSITION_WITH_URI_CHARACTERS_PARAMETERS_RIM);
		ResourceInteractionModel model = (ResourceInteractionModel) domainModel.getRims().get(0);
		EList<Resource.Diagnostic> errors = model.eResource().getErrors();
		assertEquals(0, errors.size());
		
		// there should be exactly two states
		assertEquals(2, model.getStates().size());
		State aState = model.getStates().get(0);
	    assertEquals("A", aState.getName());
	    EList<UriLink> uriLinks = aState.getTransitions().get(0).getSpec().getUriLinks();
	    assertEquals(1, uriLinks.size());
	    assertEquals("filter", uriLinks.get(0).getTemplateProperty());
	    assertEquals("CustomerCode eq '{Id}'", uriLinks.get(0).getEntityProperty().getName());

	    // there should one transition from state A to state B
		assertEquals(1, aState.getTransitions().size());
		assertEquals("B", aState.getTransitions().get(0).getState().getName());
	}

	private final static String TRANSITION_WITH_EXPRESSION_RIM = "" +
			"rim Test {" + LINE_SEP +
			"	command GetEntity" + LINE_SEP +
			"	command GetEntities" + LINE_SEP +
			"	command PutEntity" + LINE_SEP +
					
			"initial resource A {" + LINE_SEP +
			"	type: collection" + LINE_SEP +
			"	entity: ENTITY" + LINE_SEP +
			"	view: GetEntities" + LINE_SEP +
			"	GET -> B { condition: OK(B) }" + LINE_SEP +
			"	GET -> B { condition: NOT_FOUND(B) }" + LINE_SEP +
			"	GET -> B { condition: OK(B) && NOT_FOUND(B) }" + LINE_SEP +
			"}" + LINE_SEP +

			"resource B {" +
			"	type: item" + LINE_SEP +
			"	entity: ENTITY" + LINE_SEP +
			"	view: GetEntity" + LINE_SEP +
			"}" + LINE_SEP +
			"}" + LINE_SEP +  // end rim
			"";

	@Test
	public void testParseStatesWithTransitionEvalExpression() throws Exception {
		DomainModel domainModel = parser.parse(TRANSITION_WITH_EXPRESSION_RIM);
		ResourceInteractionModel model = (ResourceInteractionModel) domainModel.getRims().get(0);
		EList<Resource.Diagnostic> errors = model.eResource().getErrors();
		assertEquals(0, errors.size());
		
		// there should be exactly two states
		assertEquals(2, model.getStates().size());
		State aState = model.getStates().get(0);
	    assertEquals("A", aState.getName());

	    // there should two transitions from state A to state B
		assertEquals(3, aState.getTransitions().size());
		// assert expressions on OK transition
		assertEquals("B", aState.getTransitions().get(0).getState().getName());
		assertEquals(1, aState.getTransitions().get(0).getSpec().getEval().getExpressions().size());
		assertTrue(aState.getTransitions().get(0).getSpec().getEval().getExpressions().get(0) instanceof OKFunction);
		assertEquals("B", ((OKFunction) aState.getTransitions().get(0).getSpec().getEval().getExpressions().get(0)).getState().getName());

		// assert expressions on NOT_FOUND transition
		assertEquals("B", aState.getTransitions().get(1).getState().getName());
		assertEquals(1, aState.getTransitions().get(1).getSpec().getEval().getExpressions().size());
		assertTrue(aState.getTransitions().get(1).getSpec().getEval().getExpressions().get(0) instanceof NotFoundFunction);
		assertEquals("B", ((NotFoundFunction) aState.getTransitions().get(1).getSpec().getEval().getExpressions().get(0)).getState().getName());

		// assert expressions with && on transition
		assertEquals("B", aState.getTransitions().get(2).getState().getName());
		Transition twe = (Transition) aState.getTransitions().get(2);
		assertEquals(2, twe.getSpec().getEval().getExpressions().size());
		assertTrue(twe.getSpec().getEval().getExpressions().get(0) instanceof OKFunction);
		assertEquals("B", ((OKFunction) twe.getSpec().getEval().getExpressions().get(0)).getState().getName());
		assertTrue(twe.getSpec().getEval().getExpressions().get(1) instanceof NotFoundFunction);
		assertEquals("B", ((NotFoundFunction) twe.getSpec().getEval().getExpressions().get(1)).getState().getName());

	}

	private final static String EMBEDDED_TRANSITION_RIM = "" +
			"rim Test {" + LINE_SEP +
			"	command GetEntity" + LINE_SEP +
			"	command GetEntities" + LINE_SEP +
					
			"initial resource A {" + LINE_SEP +
			"	type: collection" + LINE_SEP +
			"	entity: ENTITY" + LINE_SEP +
			"	view: GetEntities" + LINE_SEP +
			"	GET +-> B" + LINE_SEP +
			"}" + LINE_SEP +

			"resource B {" +
			"	type: item" + LINE_SEP +
			"	entity: ENTITY" + LINE_SEP +
			"	view: GetEntity" + LINE_SEP +
			"}" + LINE_SEP +
			"}" + LINE_SEP +  // end rim
			"";

	@Test
	public void testParseStatesWithEmbeddedTransition() throws Exception {
		DomainModel domainModel = parser.parse(EMBEDDED_TRANSITION_RIM);
		ResourceInteractionModel model = (ResourceInteractionModel) domainModel.getRims().get(0);
		assertEquals(0, model.eResource().getErrors().size());
		
		// there should be exactly two states
		assertEquals(2, model.getStates().size());
		State aState = model.getStates().get(0);
	    assertEquals("A", aState.getName());

	    // there should one transition from state A to state B
		assertEquals(1, aState.getTransitions().size());
		assertEquals("B", aState.getTransitions().get(0).getState().getName());
	}
	
	private final static String EXCEPTION_RESOURCE_RIM = "" +
			"rim Test {" + LINE_SEP +
			"	command Noop" + LINE_SEP +
			
			"exception resource EXCEPTION {" + LINE_SEP +
			"	type: collection" + LINE_SEP +
			"	entity: ENTITY" + LINE_SEP +
			"   view: Noop" + LINE_SEP +
			"}\r\n" + LINE_SEP +
			"}" + LINE_SEP +  // end rim
			"";

	@Test
	public void testParseExceptionResource() throws Exception {
		DomainModel domainModel = parser.parse(EXCEPTION_RESOURCE_RIM);
		ResourceInteractionModel model = (ResourceInteractionModel) domainModel.getRims().get(0);
		assertEquals(0, model.eResource().getErrors().size());
		
		// there should be exactly one state
		assertEquals(1, model.getStates().size());
	    assertEquals("EXCEPTION", model.getStates().get(0).getName());

	    // state should be an exception state
	    State state = model.getStates().get(0);
	    assertTrue(state.isIsException());
	}

	private final static String RESOURCE_RELATIONS_RIM = "" +
			"rim Test {" + LINE_SEP +
			"	command Noop" + LINE_SEP +
			"	command Update" + LINE_SEP +
			
			"initial resource accTransactions {" + LINE_SEP +
			"	type: collection" + LINE_SEP +
			"	entity: ENTITY" + LINE_SEP +
			"   view: Noop" + LINE_SEP +
			"   relations [ \"archives\", \"http://www.temenos.com/statement-entries\" ]" + LINE_SEP +
			"   PUT -> accTransaction" + LINE_SEP +
			"}\r\n" + LINE_SEP +
			"resource accTransaction {" + LINE_SEP +
			"	type: item" + LINE_SEP +
			"	entity: ENTITY" + LINE_SEP +
			"   actions [ Update ]" + LINE_SEP +
			"   relations [ \"edit\" ]" + LINE_SEP +
			"}\r\n" + LINE_SEP +
			"}" + LINE_SEP +  // end rim
			"";

	@Test
	public void testParseResourceRelations() throws Exception {
		DomainModel domainModel = parser.parse(RESOURCE_RELATIONS_RIM);
		ResourceInteractionModel model = (ResourceInteractionModel) domainModel.getRims().get(0);
		EList<Resource.Diagnostic> errors = model.eResource().getErrors();
		assertEquals(0, errors.size());
		
		// there should be two states
		assertEquals(2, model.getStates().size());
		State r1 = model.getStates().get(0);
	    assertEquals("accTransactions", r1.getName());
		assertEquals(2, r1.getRelations().size());
		assertEquals("archives", ((RelationConstant)r1.getRelations().get(0)).getName());
		assertEquals("http://www.temenos.com/statement-entries", ((RelationConstant)r1.getRelations().get(1)).getName());
		State r2 = model.getStates().get(1);
	    assertEquals("accTransaction", r2.getName());
		assertEquals(1, r2.getRelations().size());
		assertEquals("edit", ((RelationConstant)r2.getRelations().get(0)).getName());
	}

	private final static String GLOBAL_RESOURCE_RELATIONS_RIM = "" +
			"rim Test {" + LINE_SEP +
			"	command Noop" + LINE_SEP +
			"	command Update" + LINE_SEP +
			
			"	relation archiveRel {" + LINE_SEP +
			"		fqn: \"archive\"" + LINE_SEP +
			"	}" + LINE_SEP +

			"	relation editRel {" + LINE_SEP +
			"		fqn: \"edit\"" + LINE_SEP +
			"		description: \"See 'edit' in http://www.iana.org/assignments/link-relations/link-relations.xhtml\"" + LINE_SEP +
			"	}" + LINE_SEP +

			"initial resource accTransactions {" + LINE_SEP +
			"	type: collection" + LINE_SEP +
			"	entity: ENTITY" + LINE_SEP +
			"   view: Noop" + LINE_SEP +
			"   relations [ archiveRel, \"http://www.temenos.com/statement-entries\" ]" + LINE_SEP +
			"   PUT -> accTransaction" + LINE_SEP +
			"}\r\n" + LINE_SEP +
			"resource accTransaction {" + LINE_SEP +
			"	type: item" + LINE_SEP +
			"	entity: ENTITY" + LINE_SEP +
			"   actions [ Update ]" + LINE_SEP +
			"   relations [ editRel ]" + LINE_SEP +
			"}\r\n" + LINE_SEP +
			"}" + LINE_SEP +  // end rim
			"";

	@Test
	public void testParseGlobalResourceRelations() throws Exception {
		DomainModel domainModel = parser.parse(GLOBAL_RESOURCE_RELATIONS_RIM);
		ResourceInteractionModel model = (ResourceInteractionModel) domainModel.getRims().get(0);
		EList<Resource.Diagnostic> errors = model.eResource().getErrors();
		assertEquals(0, errors.size());
		
		// there should be two states
		assertEquals(2, model.getStates().size());
		State r1 = model.getStates().get(0);
	    assertEquals("accTransactions", r1.getName());
		assertEquals(2, r1.getRelations().size());
		assertEquals("archive", ((Relation) ((RelationRef)r1.getRelations().get(0)).getRelation()).getFqn());
		assertEquals("http://www.temenos.com/statement-entries", ((RelationConstant)r1.getRelations().get(1)).getName());
		State r2 = model.getStates().get(1);
	    assertEquals("accTransaction", r2.getName());
		assertEquals(1, r2.getRelations().size());
		assertEquals("edit", ((Relation) ((RelationRef)r2.getRelations().get(0)).getRelation()).getFqn());
	}

	private final static String RESOURCE_ON_ERROR = "" +
			"rim Test {" + LINE_SEP +
			"	command GetEntity" + LINE_SEP +
			"	command NoopGET" + LINE_SEP +
					
			"initial resource A {" + LINE_SEP +
			"	type: collection" + LINE_SEP +
			"	entity: ENTITY" + LINE_SEP +
			"	view: GetEntity" + LINE_SEP +
			"	onerror --> AE" + LINE_SEP +
			"}" + LINE_SEP +

			"resource AE {" + LINE_SEP +
			"	type: item" + LINE_SEP +
			"	entity: ERROR" + LINE_SEP +
			"	view: NoopGET" + LINE_SEP +
			"}" + LINE_SEP +
			"}" + LINE_SEP +  // end rim
			"";

	@Test
	public void testParseOnErrorResource() throws Exception {
		DomainModel domainModel = parser.parse(RESOURCE_ON_ERROR);
		ResourceInteractionModel model = (ResourceInteractionModel) domainModel.getRims().get(0);
		assertEquals(0, model.eResource().getErrors().size());
		
		// there should be two states
		assertEquals(2, model.getStates().size());
		State r1 = model.getStates().get(0);
	    assertEquals("A", r1.getName());
		State r2 = model.getStates().get(1);
	    assertEquals("AE", r2.getName());
	    
	    //AE is the error handler for A
	    assertEquals(r2.getName(), r1.getErrorState().getName());
	}
	
	private final static String TRANSITION_WITHOUT_USE_RIMS = "" +
			"domain TestDomain {" + LINE_SEP +	
			"    rim ONE {" + LINE_SEP +
			"	     command NoopGET" + LINE_SEP +
			"        initial resource A {" + LINE_SEP +
			"			type: collection" + LINE_SEP +
			"			entity: ENTITY" + LINE_SEP +
			"			view: NoopGET" + LINE_SEP +
			"			GET -> TestDomain.TWO.B" + LINE_SEP +
			"        }" + LINE_SEP +
			"    }" + LINE_SEP +  // end rim
			"    rim TWO {" + LINE_SEP +
			"        command NoopGET" + LINE_SEP +
			"        initial resource B {" + LINE_SEP +
			"			type: collection" + LINE_SEP +
			"			entity: ENTITY" + LINE_SEP +
			"			view: NoopGET" + LINE_SEP +
			"			GET -> ONE.A" + LINE_SEP +
			"			GET -> A" + LINE_SEP +
			"        }" + LINE_SEP +
			"    }" + LINE_SEP +  // end rim
			"}" + LINE_SEP +  // end domain
			"" + LINE_SEP;

	@Test
	public void testParseTransitionWithoutUse() throws Exception {
		DomainModel rootModel = parser.parse(TRANSITION_WITHOUT_USE_RIMS);

		// there should be one domain
		assertEquals(1, rootModel.getRims().size());
		// there should be two rims
		DomainDeclaration domainModel = (DomainDeclaration) rootModel.getRims().get(0);
		assertEquals(2, domainModel.getRims().size());

		// there should be one state in each rim and no errors
		ResourceInteractionModel model1 = (ResourceInteractionModel) domainModel.getRims().get(0);
		EList<Resource.Diagnostic> errors1 = model1.eResource().getErrors();
		assertEquals(0, errors1.size());
		assertEquals(1, model1.getStates().size());
	    assertEquals("A", model1.getStates().get(0).getName());

		ResourceInteractionModel model2 = (ResourceInteractionModel) domainModel.getRims().get(1);
		EList<Resource.Diagnostic> errors2 = model2.eResource().getErrors();
		assertEquals(0, errors2.size());
		assertEquals(1, model2.getStates().size());
	    assertEquals("B", model2.getStates().get(0).getName());
	    
	    // test scope for resource A
	    State A = model1.getStates().get(0);
	    EReference refA = A.eContainmentFeature();
	    IScope scopeA = scopeProvider.getScope(model1, refA);
	    assertEquals("A, ONE.A, TWO.B, TestDomain.ONE.A, TestDomain.TWO.B", formStringObjectInScope(scopeA));

	    // test scope for resource B
	    State B = model2.getStates().get(0);
	    EReference refB = B.eContainmentFeature();
	    IScope scopeB = scopeProvider.getScope(model2, refB);
	    assertEquals("B, ONE.A, TWO.B, TestDomain.ONE.A, TestDomain.TWO.B", formStringObjectInScope(scopeB));
	}
	
	private final static String TRANSITION_WITH_USE_RIMS = "" +
			"domain TestDomain {" + LINE_SEP +	
			"    use TestDomain.ONE.*" + LINE_SEP +
			"    rim ONE {" + LINE_SEP +
			"        command NoopGET" + LINE_SEP +
			"        initial resource A {" + LINE_SEP +
			"	         type: collection" + LINE_SEP +
			"	         entity: ENTITY" + LINE_SEP +
			"	         view: NoopGET" + LINE_SEP +
			"	         GET -> TestDomain.TWO.B" + LINE_SEP +
			"        }" + LINE_SEP +
			"    }" + LINE_SEP +  // end rim
			"    rim TWO {" + LINE_SEP +
			"        command NoopGET" + LINE_SEP +
			"        initial resource B {" + LINE_SEP +
			"	         type: collection" + LINE_SEP +
			"	         entity: ENTITY" + LINE_SEP +
			"	         view: NoopGET" + LINE_SEP +
			"	         GET -> ONE.A" + LINE_SEP +
			"	         GET -> A" + LINE_SEP +
			"	         onerror --> ONE.A" + LINE_SEP +
			"        }" + LINE_SEP +
			"    }" + LINE_SEP +  // end rim
			"}" + LINE_SEP +  // end domain
			"" + LINE_SEP;

	@Test
	public void testParseTransitionWithUse() throws Exception {
		DomainModel rootModel = parser.parse(TRANSITION_WITH_USE_RIMS);

		// there should be one domain
		assertEquals(1, rootModel.getRims().size());
		// there should be two rims
		DomainDeclaration domainModel = (DomainDeclaration) rootModel.getRims().get(0);
		assertEquals(3, domainModel.getRims().size());

		// there should be one state in each rim and no errors
		ResourceInteractionModel model1 = (ResourceInteractionModel) domainModel.getRims().get(1);
		EList<Resource.Diagnostic> errors1 = model1.eResource().getErrors();
		assertEquals(0, errors1.size());
		assertEquals(1, model1.getStates().size());
	    assertEquals("A", model1.getStates().get(0).getName());

		ResourceInteractionModel model2 = (ResourceInteractionModel) domainModel.getRims().get(2);
		EList<Resource.Diagnostic> errors2 = model2.eResource().getErrors();
		assertEquals(0, errors2.size());
		assertEquals(1, model2.getStates().size());
	    assertEquals("B", model2.getStates().get(0).getName());
	    
	    // test scope for resource A
	    State A = model1.getStates().get(0);
	    EReference refA = A.eContainmentFeature();
	    IScope scopeA = scopeProvider.getScope(model1, refA);
	    assertEquals("A, ONE.A, TWO.B, TestDomain.ONE.A, TestDomain.TWO.B", formStringObjectInScope(scopeA));

	    // test scope for resource B
	    State B = model2.getStates().get(0);
	    EReference refB = B.eContainmentFeature();
	    IScope scopeB = scopeProvider.getScope(model2, refB);
	    assertEquals("B, ONE.A, TWO.B, A, TestDomain.ONE.A, TestDomain.TWO.B", formStringObjectInScope(scopeB));
	}
	
	private String formStringObjectInScope(IScope scope) {
	    List<String> actualList = Lists.newArrayList();
	    for (IEObjectDescription desc : scope.getAllElements()) {
	    	actualList.add(desc.getName().toString());
	    }
	    String actual = Joiner.on(", ").join(actualList);
	    return actual;
	}
	
	private final static String SIMPLE_PATHS_RIM = "" +
	"rim Simple {" + LINE_SEP +	
	"	command GetEntity" + LINE_SEP +
	"	command UpdateEntity" + LINE_SEP +
			
	"initial resource A {" + LINE_SEP +
	"	type: collection" + LINE_SEP +
	"	entity: ENTITY" + LINE_SEP +
	"	path: \"/A()\"" + LINE_SEP +
	"	view: GetEntity" + LINE_SEP +
	"}" + LINE_SEP +

	"resource B {" + LINE_SEP +
	"	type: item" + LINE_SEP +
	"	entity: ENTITY" + LINE_SEP +
	"	path: \"/B('{id}')\"" + LINE_SEP +
	"	actions [ UpdateEntity ]" + LINE_SEP +
	"}" + LINE_SEP +

	"initial resource AnotherResource {" + LINE_SEP +
	"	type: collection" + LINE_SEP +
	"	entity: ENTITY" + LINE_SEP +
	"	path: \"/AnotherResource()\"" + LINE_SEP +
	"	view: GetEntity" + LINE_SEP +
	"}" + LINE_SEP +

	"}" + LINE_SEP +  // end rim
	"";

	@Test
	public void testParseSimplePaths() throws Exception {
		DomainModel domainModel = parser.parse(SIMPLE_PATHS_RIM);
		ResourceInteractionModel model = (ResourceInteractionModel) domainModel.getRims().get(0);
		EList<Resource.Diagnostic> errors = model.eResource().getErrors();
		assertEquals(0, errors.size());
		
		// there should be exactly two states
		assertEquals(3, model.getStates().size());
	    assertEquals("A", model.getStates().get(0).getName());
	    assertEquals("B", model.getStates().get(1).getName());
	    assertEquals("AnotherResource", model.getStates().get(2).getName());

	    // there should be no transitions between these states
	    State Astate = model.getStates().get(0);
	    assertEquals("/A()", Astate.getPath().getName());
	    State Bstate = model.getStates().get(1);
	    assertEquals("/B('{id}')", Bstate.getPath().getName());
	    State ANOstate = model.getStates().get(2);
	    assertEquals("/AnotherResource()", ANOstate.getPath().getName());
	}

	private final static String BASE_PATHS_RIM = "" +
	"rim Base {" + LINE_SEP +	
	"	command GetEntity" + LINE_SEP +
	"	command UpdateEntity" + LINE_SEP +
	"	basepath: \"/{companyid}\"" + LINE_SEP +
			
	"initial resource A {" + LINE_SEP +
	"	type: collection" + LINE_SEP +
	"	entity: ENTITY" + LINE_SEP +
	"	path: \"/A()\"" + LINE_SEP +
	"	view: GetEntity" + LINE_SEP +
	"}" + LINE_SEP +

	"}" + LINE_SEP +  // end rim
	"";

	@Test
	public void testParseBasePaths() throws Exception {
		DomainModel domainModel = parser.parse(BASE_PATHS_RIM);
		ResourceInteractionModel model = (ResourceInteractionModel) domainModel.getRims().get(0);
		EList<Resource.Diagnostic> errors = model.eResource().getErrors();
		assertEquals(0, errors.size());
		
		// there should be exactly one states
		assertEquals(1, model.getStates().size());
	    assertEquals("A", model.getStates().get(0).getName());

	    // company basepath
	    assertEquals("/{companyid}", model.getBasepath().getName());
	    // resource path
	    State Astate = model.getStates().get(0);
	    assertEquals("/A()", Astate.getPath().getName());
	}

}
