package com.temenos.interaction.odataext.entity;

/*
 * #%L
 * interaction-odata4j-ext
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


import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.HttpMethod;

import org.odata4j.core.ODataVersion;
import org.odata4j.edm.EdmAssociation;
import org.odata4j.edm.EdmAssociationEnd;
import org.odata4j.edm.EdmComplexType;
import org.odata4j.edm.EdmDataServices;
import org.odata4j.edm.EdmDataServices.Builder;
import org.odata4j.edm.EdmEntityContainer;
import org.odata4j.edm.EdmEntitySet;
import org.odata4j.edm.EdmEntityType;
import org.odata4j.edm.EdmFunctionImport;
import org.odata4j.edm.EdmMultiplicity;
import org.odata4j.edm.EdmNavigationProperty;
import org.odata4j.edm.EdmProperty;
import org.odata4j.edm.EdmProperty.CollectionKind;
import org.odata4j.edm.EdmSchema;
import org.odata4j.edm.EdmSimpleType;
import org.odata4j.edm.EdmType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.temenos.interaction.core.entity.EntityMetadata;
import com.temenos.interaction.core.entity.Metadata;
import com.temenos.interaction.core.entity.vocabulary.terms.TermComplexGroup;
import com.temenos.interaction.core.entity.vocabulary.terms.TermComplexType;
import com.temenos.interaction.core.entity.vocabulary.terms.TermIdField;
import com.temenos.interaction.core.entity.vocabulary.terms.TermListType;
import com.temenos.interaction.core.entity.vocabulary.terms.TermValueType;
import com.temenos.interaction.core.hypermedia.CollectionResourceState;
import com.temenos.interaction.core.hypermedia.ResourceState;
import com.temenos.interaction.core.hypermedia.ResourceStateMachine;
import com.temenos.interaction.core.hypermedia.Transition;


/**
 * This class converts a Metadata structure to odata4j's EdmDataServices.
 */
public class MetadataOData4j {
	private final static Logger logger = LoggerFactory.getLogger(MetadataOData4j.class);

	private final static String MULTI_NAV_PROP_TO_ENTITY = "MULTI_NAV_PROP";

	private EdmDataServices edmDataServices;
	private Metadata metadata;
	private ResourceStateMachine hypermediaEngine;
	private ResourceState serviceDocument;

	/**
	 * Construct the odata metadata ({@link EdmDataServices}) by looking up a resource 
	 * called 'ServiceDocument' and add an EntitySet to the metadata for any collection
	 * resource with a transition from this 'ServiceDocument' resource.
	 * @param metadata metadata
	 */
	public MetadataOData4j(Metadata metadata, ResourceStateMachine hypermediaEngine) {
		serviceDocument = hypermediaEngine.getResourceStateByName("ServiceDocument");
		if (serviceDocument == null)
			throw new RuntimeException("No 'ServiceDocument' found.");
		assert(!(serviceDocument instanceof CollectionResourceState)) : "Initial state must be an individual resource state";
		this.metadata = metadata;
		this.hypermediaEngine = hypermediaEngine;
	}

	/**
	 * Returns odata4j metadata
	 * @return edmdataservices object
	 */
	public EdmDataServices getMetadata() {
		if (edmDataServices == null) 
			edmDataServices = createOData4jMetadata(metadata, hypermediaEngine, serviceDocument);
		return edmDataServices;
	}

	/**
	 * Create EDM metadata merged from multiple producers
	 * @param producers Set of odata producers
	 * @return Merged EDM metadata
	 */
	public EdmDataServices createOData4jMetadata(Metadata metadata, ResourceStateMachine hypermediaEngine, ResourceState serviceDocument) {
		String serviceName = metadata.getModelName();
		String namespace = serviceName + Metadata.MODEL_SUFFIX;
		Builder mdBuilder = EdmDataServices.newBuilder();
		List<EdmSchema.Builder> bSchemas = new ArrayList<EdmSchema.Builder>();
		EdmSchema.Builder bSchema = new EdmSchema.Builder();
		List<EdmEntityContainer.Builder> bEntityContainers = new ArrayList<EdmEntityContainer.Builder>();
		Map<String, EdmEntityType.Builder> bEntityTypeMap = new HashMap<String, EdmEntityType.Builder>();
		Map<String, EdmComplexType.Builder> bComplexTypeMap = new HashMap<String, EdmComplexType.Builder>();
		Map<String, EdmEntitySet.Builder> bEntitySetMap = new HashMap<String, EdmEntitySet.Builder>();
		Map<String, EdmFunctionImport.Builder> bFunctionImportMap = new HashMap<String, EdmFunctionImport.Builder>();
		List<EdmAssociation.Builder> bAssociations = new ArrayList<EdmAssociation.Builder>();

		for(EntityMetadata entityMetadata : metadata.getEntitiesMetadata().values()) {
			List<EdmProperty.Builder> bProperties = new ArrayList<EdmProperty.Builder>();
			List<String> keys = new ArrayList<String>();
			String complexTypePrefix = new StringBuilder(entityMetadata.getEntityName()).append("_").toString();
			for(String propertyName : entityMetadata.getPropertyVocabularyKeySet()) {
				//Entity properties, lets gather some information about the property
				String termComplex = entityMetadata.getTermValue(propertyName, TermComplexType.TERM_NAME);							// Is vocabulary a group (Complex Type)
				boolean termList = Boolean.parseBoolean(entityMetadata.getTermValue(propertyName, TermListType.TERM_NAME));	// Is vocabulary a List of (Complex Types)
				String termComplexGroup = entityMetadata.getTermValue(propertyName, TermComplexGroup.TERM_NAME);					// Is vocabulary belongs to a group (ComplexType) 
				boolean isNullable = entityMetadata.isPropertyNullable(propertyName);
				if (termComplex.equals("false")) {
					// This means we are dealing with plain property, either belongs to Entity or ComplexType (decide later, lets build it first)
					EdmType edmType = termValueToEdmType(entityMetadata.getTermValue(propertyName, TermValueType.TERM_NAME));
					EdmProperty.Builder ep = EdmProperty.newBuilder(entityMetadata.getSimplePropertyName(propertyName)).
							setType(edmType).
							setNullable(isNullable);
					if (termComplexGroup == null) {
						// Property belongs to an Entity Type, simply add it 
						bProperties.add(ep);
					} else {
						// Property belongs to a group (complex type), first make sure we have a group 
						// so add a group with Entity name space and group name
						addComplexType(namespace, complexTypePrefix + entityMetadata.getSimplePropertyName(termComplexGroup), bComplexTypeMap);
						// And then add the property into complex type
						addPropertyToComplexType(namespace, complexTypePrefix + entityMetadata.getSimplePropertyName(termComplexGroup), ep, bComplexTypeMap);
					}
				} else {
					// This means vocabulary is a group (complex type), so add it in a map
					String complexPropertyName = complexTypePrefix + entityMetadata.getSimplePropertyName(propertyName);
					addComplexType(namespace, complexPropertyName, bComplexTypeMap);
					if (termComplexGroup != null) {
						// This mean group (complex type) belongs to a group (complex type), so make sure add the parent group and add
						// nested group as group property
						addComplexType(namespace, complexTypePrefix + entityMetadata.getSimplePropertyName(termComplexGroup), bComplexTypeMap);
						addComplexTypeToComplexType(namespace, complexTypePrefix + entityMetadata.getSimplePropertyName(termComplexGroup), complexPropertyName, isNullable, termList, bComplexTypeMap);
					} else {
						// This means group (complex type) belongs to an Entity, so simply build and add as a Entity prop
						EdmProperty.Builder ep;
						if (termList) {
							ep = EdmProperty.newBuilder(complexPropertyName).
									setType(bComplexTypeMap.get(namespace + "." + complexPropertyName)).
									setCollectionKind(CollectionKind.List).								
									setNullable(isNullable);
						} else {
							ep = EdmProperty.newBuilder(complexPropertyName).
									setType(bComplexTypeMap.get(namespace + "." + complexPropertyName)).
									setNullable(isNullable);
						}
						bProperties.add(ep);
					}
				}	

				//Entity keys
				if(entityMetadata.getTermValue(propertyName, TermIdField.TERM_NAME).equals("true")) {
					if(termComplex.equals("true")) {
						keys.add(complexTypePrefix + entityMetadata.getSimplePropertyName(propertyName));
					}
					else {
						keys.add(propertyName);
					}
				}
			}

			// Add entity type
			if (keys.size() > 0) {
				EdmEntityType.Builder bEntityType = EdmEntityType.newBuilder().setNamespace(namespace).setAlias(entityMetadata.getEntityName()).setName(entityMetadata.getEntityName()).addKeys(keys).addProperties(bProperties);
				bEntityTypeMap.put(entityMetadata.getEntityName(), bEntityType);
			} else {
				logger.error("Unable to add EntityType for [" + entityMetadata.getEntityName() + "] - no ID column defined");
			}
		}

		// Add Navigation Properties
		for (EdmEntityType.Builder bEntityType : bEntityTypeMap.values()) {
			// build associations
			Map<String, EdmAssociation.Builder> bAssociationMap = buildAssociations(namespace, bEntityType, bEntityTypeMap, hypermediaEngine, serviceDocument);
			bAssociations.addAll(bAssociationMap.values());

			//add navigation properties
			Map<String,String> multiAssociation = new HashMap<String,String>();
			int multipleAssoc = 0;
			String entityName = bEntityType.getName();
			for (ResourceState s : hypermediaEngine.getStates()) {
				for (ResourceState ts : s.getAllTargets()) {
					Collection<Transition> entityTransitions = s.getTransitions(ts);
					if (entityTransitions != null) {
						for(Transition entityTransition : entityTransitions) {
							ResourceState sourceState = entityTransition.getSource();
							ResourceState targetState = entityTransition.getTarget();
							if (sourceState.getEntityName().equals(entityName) 
									&& !entityTransition.getTarget().isPseudoState()
									&& !entityTransition.getTarget().equals(serviceDocument)
									&& !(entityTransition.getSource() instanceof CollectionResourceState)) {
								//We can have more than one navigation property for the same association
								String navPropertyName = targetState.getName();
								//We can have transitions to a resource state from multiple source states
								if (entityTransition.getLabel() != null) {
									navPropertyName = entityTransition.getLabel();
								} else if (multiAssociation.get(navPropertyName) != null) {
									navPropertyName = navPropertyName + "_" + multipleAssoc++;
								}
								multiAssociation.put(navPropertyName, targetState.getName());
								
								EdmAssociation.Builder relationship = bAssociationMap.get(targetState.getName());
								bEntityType.addNavigationProperties(EdmNavigationProperty
										.newBuilder(navPropertyName)
										.setRelationship(relationship)
										.setFromTo(relationship.getEnd1(), relationship.getEnd2()));
							}
						}
					}
				}
			}
		}

		// Index EntitySets by Entity name
		Collection<ResourceState> allTargets = hypermediaEngine.getStates();
		for (ResourceState state : allTargets) {
			if (state instanceof CollectionResourceState) {
				EdmEntityType.Builder entityType = bEntityTypeMap.get(state.getEntityName());
				if (entityType == null) 
					throw new RuntimeException("Entity type not found for " + state.getEntityName());
				Transition fromInitialState = serviceDocument.getTransition(state);
				if (fromInitialState != null) {
					//Add entity set
					EdmEntitySet.Builder bEntitySet = EdmEntitySet.newBuilder().setName(state.getName()).setEntityType(entityType);
					bEntitySetMap.put(state.getEntityName(), bEntitySet);
				} else {
					logger.error("Not adding entity set ["+state.getName()+"] to metadata, no transition from initial state ["+serviceDocument.getName()+"]");
				}
			}
		}

		for (ResourceState state : hypermediaEngine.getStates()) {
			if (state instanceof CollectionResourceState) {
				Transition fromInitialState = serviceDocument.getTransition(state);
				if (fromInitialState == null) {
					EdmEntitySet.Builder bEntitySet = bEntitySetMap.get(state.getEntityName());
					// Add Function
					EdmFunctionImport.Builder bFunctionImport = EdmFunctionImport.newBuilder()
							.setName(state.getName())
							.setEntitySet(bEntitySet)
							.setHttpMethod(HttpMethod.GET)
							.setIsCollection(true)
							.setReturnType(bEntityTypeMap.get(state.getEntityName()));
					bFunctionImportMap.put(state.getName(), bFunctionImport);
				}
			}
		}

		EdmEntityContainer.Builder bEntityContainer = EdmEntityContainer.newBuilder()
				.setName(serviceName)
				.setIsDefault(true)
				.addEntitySets(new ArrayList<EdmEntitySet.Builder>(bEntitySetMap.values()))
				.addFunctionImports(new ArrayList<EdmFunctionImport.Builder>(bFunctionImportMap.values()));
		bEntityContainers.add(bEntityContainer);

		List<EdmEntityType.Builder> bEntityTypes = new ArrayList<EdmEntityType.Builder>();
		bEntityTypes.addAll(bEntityTypeMap.values());

		List<EdmComplexType.Builder> bComplexTypes = new ArrayList<EdmComplexType.Builder>();
		bComplexTypes.addAll(bComplexTypeMap.values());

		bSchema
		.setNamespace(namespace)
		.setAlias(serviceName)
		.addEntityTypes(bEntityTypes)
		.addComplexTypes(bComplexTypes)
		.addAssociations(bAssociations)
		.addEntityContainers(bEntityContainers);
		bSchemas.add(bSchema);

		mdBuilder.addSchemas(bSchemas);
		mdBuilder.setVersion(ODataVersion.V1);

		//Build the EDM metadata
		return mdBuilder.build();
	}

	private Map<String, EdmAssociation.Builder> buildAssociations(String namespace, EdmEntityType.Builder entityType, Map<String, EdmEntityType.Builder> bEntityTypeMap, ResourceStateMachine hypermediaEngine, ResourceState serviceDocument) {
		// Obtain the relation between entities and write navigation properties
		Map<String, EdmAssociation.Builder> bAssociationMap = new HashMap<String, EdmAssociation.Builder>();
		//Map<Association name, Entity relation>
		Map<String, EdmAssociation.Builder> relations = new HashMap<String, EdmAssociation.Builder>();

		String entityName = entityType.getName();
		Collection<Transition> entityTransitions = hypermediaEngine.getTransitionsById().values();
		if (entityTransitions != null) {
			//Find out which target entities have more than one transition from this state
			Set<String> targetStateNames = new HashSet<String>();
			Map<String, String> multipleNavPropsToEntity = new HashMap<String, String>();		//Map<TargetEntityName, TargetStateName>
			for(Transition entityTransition : entityTransitions) {
				if (entityTransition.getSource().getEntityName().equals(entityName) 
						&& !entityTransition.getTarget().isPseudoState()
						&& !entityTransition.getTarget().equals(serviceDocument)) {
					String targetEntityName = entityTransition.getTarget().getEntityName();
					String targetStateName = entityTransition.getTarget().getName();
					String lastTargetStateName = multipleNavPropsToEntity.get(targetEntityName);
					if(lastTargetStateName == null) {
						multipleNavPropsToEntity.put(targetEntityName, targetStateName);
						targetStateNames.add(entityTransition.getTarget().getName());
					}
					else if(!targetStateName.equals(lastTargetStateName)) {		//Disregard transitions from multiple source states
						multipleNavPropsToEntity.put(targetEntityName, MULTI_NAV_PROP_TO_ENTITY);		//null indicates to generate multiple navigation properties 
					}
				}
			}

			//Create navigation properties from transitions
			Set<String> npNames = new HashSet<String>();
			for(Transition entityTransition : entityTransitions) {
				ResourceState sourceState = entityTransition.getSource();
				ResourceState targetState = entityTransition.getTarget();
				String npName = targetState.getName();
				if (sourceState.getEntityName().equals(entityName) 
						&& !entityTransition.getTarget().isPseudoState()
						&& !entityTransition.getTarget().equals(serviceDocument)
						&& !npNames.contains(npName)
						&& !(entityTransition.getSource() instanceof CollectionResourceState)) {		//We can have transitions to a resource state from multiple source states
					// Use the entity names to define the relation
					String relationName;
					if(multipleNavPropsToEntity.get(targetState.getEntityName()).equals(MULTI_NAV_PROP_TO_ENTITY)) {
						//More than one transition => use separate associations "sourceEntityName_navPropName"
						relationName = sourceState.getEntityName() + "_" + targetState.getName();
					}
					else {
						//Only one transition => use single association "sourceEntityName_targetEntityName"
						relationName = sourceState.getEntityName() + "_" + targetState.getEntityName();
						String invertedRelationName = targetState.getEntityName() + "_" + sourceState.getEntityName();
						if(relations.containsKey(invertedRelationName)) {
							relationName = invertedRelationName;					
						}
					}

					//Multiplicity
					EdmMultiplicity multiplicitySource = targetState instanceof CollectionResourceState ? EdmMultiplicity.ONE : EdmMultiplicity.MANY;
					EdmMultiplicity multiplicityTarget = targetState instanceof CollectionResourceState ? EdmMultiplicity.MANY : EdmMultiplicity.ONE;

					// Association
					EdmAssociationEnd.Builder sourceRole = EdmAssociationEnd.newBuilder()
							.setRole(relationName + "_Source")
							.setType(entityType)
							.setMultiplicity(multiplicitySource);
					EdmEntityType.Builder targetEntityType = bEntityTypeMap.get(targetState.getEntityName());
					assert(targetEntityType != null);
					EdmAssociationEnd.Builder targetRole = EdmAssociationEnd.newBuilder()
							.setRole(relationName + "_Target")
							.setType(targetEntityType)
							.setMultiplicity(multiplicityTarget);
					EdmAssociation.Builder bAssociation = EdmAssociation.newBuilder()
							.setNamespace(namespace)
							.setName(relationName)
							.setEnds(sourceRole, targetRole);

					bAssociationMap.put(targetState.getName(), bAssociation);
					if (!relations.containsKey(relationName)) {
						relations.put(relationName, bAssociation);
					}
				}
			}
		}
		return bAssociationMap;
	}

	/**
	 * Convert a Metadata vocabulary TermValueType to EdmType 
	 * @param type TermValueType
	 * @return EdmType
	 */
	public static EdmType termValueToEdmType(String type) {
		EdmType edmType;
		if(type.equals(TermValueType.NUMBER)) {
			edmType = EdmSimpleType.DOUBLE;
		}
		else if(type.equals(TermValueType.INTEGER_NUMBER)) {
			edmType = EdmSimpleType.INT64;
		}
		else if(type.equals(TermValueType.TIMESTAMP) ||
				type.equals(TermValueType.DATE)) {
			edmType = EdmSimpleType.DATETIME;
		}
		else if(type.equals(TermValueType.TIME)) {
			edmType = EdmSimpleType.TIME;
		}
		else if(type.equals(TermValueType.BOOLEAN)) {
			edmType = EdmSimpleType.BOOLEAN;
		}
		else {
			edmType = EdmSimpleType.STRING;
		}
		return edmType;
	}

	/**
	 * Build the complex type if and only if same name group is not found in the map
	 * @param complexTypeName
	 * @param bComplexTypeMap
	 */
	private void addComplexType(String nameSpace, String complexTypeName, 
			Map<String, EdmComplexType.Builder> bComplexTypeMap) {
		String complexTypeFullName = new StringBuilder(nameSpace)
		.append(".").append(complexTypeName)
		.toString();
		if (bComplexTypeMap.get(complexTypeFullName) == null ) {
			EdmComplexType.Builder cb = 
					EdmComplexType.newBuilder().setNamespace(nameSpace)
					.setName(complexTypeName);
			bComplexTypeMap.put(complexTypeFullName, cb);
		}
	}

	/**
	 * Adding the property to complex type if and only if not already added as a property of the complex type
	 * @param serviceNameSpace
	 * @param complexTypeName
	 * @param edmPropertyBuilder
	 * @param bComplexTypeMap
	 */
	private void addPropertyToComplexType(String nameSpace, String complexTypeName, EdmProperty.Builder edmPropertyBuilder, Map<String, EdmComplexType.Builder> bComplexTypeMap) {
		String complexTypeFullName = new StringBuilder(nameSpace)
		.append(".").append(complexTypeName)
		.toString();
		if (bComplexTypeMap.get(complexTypeFullName).findProperty(edmPropertyBuilder.getName()) == null) {
			List<EdmProperty.Builder> bl = new ArrayList<EdmProperty.Builder>();
			bl.add(edmPropertyBuilder);
			bComplexTypeMap.get(complexTypeFullName).addProperties(bl);
		}
	}

	/**
	 * Adding the ComplexType to Complex Type if and only is not already added as a property of the complex type
	 * @param serviceNameSpace
	 * @param complexTypeName
	 * @param edmPropertyBuilder
	 * @param bComplexTypeMap
	 */
	private void addComplexTypeToComplexType(String nameSpace, String complexTypeName, String nestedComplexType, boolean isNullable, boolean isList, Map<String, EdmComplexType.Builder> bComplexTypeMap) {
		String complexTypeFullName = new StringBuilder(nameSpace).append(".").append(complexTypeName).toString();
		String nestComplexTypeFullName = new StringBuilder(nameSpace).append(".").append(nestedComplexType).toString();
		if (bComplexTypeMap.get(complexTypeFullName).findProperty(nestedComplexType) == null) {
			List<EdmProperty.Builder> bl = new ArrayList<EdmProperty.Builder>();
			EdmProperty.Builder ep;
			if (isList) {
				ep = EdmProperty.newBuilder(nestedComplexType)
						.setType(bComplexTypeMap.get(nestComplexTypeFullName))
						.setCollectionKind(CollectionKind.List)
						.setNullable(isNullable);
			} else {
				ep = EdmProperty.newBuilder(nestedComplexType)
						.setType(bComplexTypeMap.get(nestComplexTypeFullName))
						.setNullable(isNullable);
			}
			bl.add(ep);
			bComplexTypeMap.get(complexTypeFullName).addProperties(bl);
		}
	}

}
