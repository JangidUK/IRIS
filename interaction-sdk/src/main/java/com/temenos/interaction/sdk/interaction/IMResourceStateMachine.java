package com.temenos.interaction.sdk.interaction;

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


import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.HttpMethod;

import com.temenos.interaction.sdk.command.Commands;
import com.temenos.interaction.sdk.interaction.state.IMCollectionState;
import com.temenos.interaction.sdk.interaction.state.IMEntityState;
import com.temenos.interaction.sdk.interaction.state.IMNavigationState;
import com.temenos.interaction.sdk.interaction.state.IMPseudoState;
import com.temenos.interaction.sdk.interaction.state.IMState;
import com.temenos.interaction.sdk.interaction.transition.IMTransition;

/**
 * This class holds information about a resource state machine.
 * An RSM holds information about interactions between different
 * states on an entity.
 */
public class IMResourceStateMachine {

	private String rimName;
	private String entityName;											//Entity name
	private IMState collectionState;							//Collection state
	private IMState entityState;								//Entity state
	private String mappedEntityProperty;								//Entity property to which the URI template parameter maps to
	private String pathParametersTemplate;								//Path parameters defined in URI template

	private Map<String, IMState> resourceStates = new HashMap<String, IMState>();	//Resource states 
	
	public IMResourceStateMachine(String entityName, String collectionStateName, String entityStateName, String mappedEntityProperty, String pathParametersTemplate) {
		this(entityName, collectionStateName, entityStateName, HttpMethod.GET, mappedEntityProperty, pathParametersTemplate);
	}

	public IMResourceStateMachine(String entityName, String collectionStateName, String entityStateName, String methodGetEntity, String mappedEntityProperty, String pathParametersTemplate) {
		this(entityName, collectionStateName, entityStateName, methodGetEntity, mappedEntityProperty, pathParametersTemplate, null);
	}
	
	public IMResourceStateMachine(String entityName, String collectionStateName, String entityStateName, String methodGetEntity, String mappedEntityProperty, String pathParametersTemplate, String collectionRels) {
		this.entityName = entityName;
		this.entityState = new IMEntityState(entityStateName, "/" + collectionStateName + "(" + pathParametersTemplate + ")", Commands.GET_ENTITY);
		resourceStates.put(entityStateName, entityState);
		this.collectionState = new IMCollectionState(collectionStateName, "/" + collectionStateName + "()", Commands.GET_ENTITIES, collectionRels, (IMEntityState) entityState);
		resourceStates.put(collectionStateName, collectionState);
		this.mappedEntityProperty = mappedEntityProperty;
		this.pathParametersTemplate = pathParametersTemplate;

		//Add a transition from the collection state to the entity state
		addStateTransition(collectionState.getName(), entityState.getName(), methodGetEntity, null, null, null, null, null);
	}
	
	public String getRimName() {
		return rimName;
	}

	public void setRimName(String rimName) {
		this.rimName = rimName;
	}

	public String getEntityName() {
		return entityName;
	}
	
	public IMState getCollectionState() {
		return collectionState;
	}
	
	public IMState getEntityState() {
		return entityState;
	}
	
	public String getMappedEntityProperty() {
		return mappedEntityProperty;
	}
	
	public String getPathParametersTemplate() {
		return pathParametersTemplate;
	}

	/**
	 * Add a transition to a collection state
	 * @param sourceStateName source state name
	 * @param targetResourceStateMachine Target resource state machine
	 * @param targetStateName target state name
	 * @param filter filter expression on collection
	 * @param title Transition label
	 */
	public void addTransitionToCollectionState(String sourceStateName, IMResourceStateMachine targetResourceStateMachine, String targetStateName, String filter, String linkProperty, String title) {
		this.addResourceTransition(sourceStateName, targetResourceStateMachine, targetStateName, HttpMethod.GET, title, targetStateName, true, linkProperty, filter);
	}

	/**
	 * Add a transition to an entity state
	 * @param sourceStateName source state name
	 * @param targetResourceStateMachine target resource state machine
	 * @param targetStateName target state name
	 * @param linkProperty linkage property
	 * @param title transition label
	 */
	public void addTransitionToEntityState(String sourceStateName, IMResourceStateMachine targetResourceStateMachine, String targetStateName, String linkProperty, String title) {
		this.addResourceTransition(sourceStateName, targetResourceStateMachine, targetStateName, HttpMethod.GET, title, targetStateName, false, linkProperty, null);
	}

	/* Add a transition to a collection or entity state
	 * @param sourceStateName
	 * @param targetResourceStateMachine
	 * @param targetStateName
	 * @param method
	 * @param title
	 * @param path
	 * @param isToCollectionState
	 * @param linkProperty
	 * @param filter
	 */
	protected void addResourceTransition(String sourceStateName, IMResourceStateMachine targetResourceStateMachine, String targetStateName, String method, String title, String path, boolean isToCollectionState, String linkProperty, String filter) {
		//Create resource states if required
		if(!resourceStates.containsKey(sourceStateName)) {
			resourceStates.put(sourceStateName, new IMEntityState(sourceStateName, path));
		}
		if(!resourceStates.containsKey(targetStateName)) {
			resourceStates.put(targetStateName, new IMNavigationState(targetStateName, path, targetResourceStateMachine, linkProperty, isToCollectionState));
		}
		
		//Add transition
		IMState sourceState = getResourceState(sourceStateName);
		IMState targetState = getResourceState(targetStateName);
		if(isToCollectionState) {
			sourceState.addTransitionToCollectionState(title, targetResourceStateMachine, targetState, linkProperty, method, filter);
		}
		else {
			sourceState.addTransitionToEntityState(title, targetResourceStateMachine, targetState, method, linkProperty);
		}
	}

	/**
	 * Add a new resource state and it's associated collection state. 
	 * @param stateId State identifier
	 * @param title Label representing this resource
	 */
	public void addCollectionAndEntityState(String stateId, String title) {
		addCollectionAndEntityState(stateId, title, null, null);
	}

	/**
	 * Add a new resource state and it's associated collection state. 
	 * @param stateId State identifier
	 * @param title Label representing this resource
	 */
	public void addCollectionAndEntityState(String stateId, String title, String collectionView, String entityView) {
		addCollectionAndEntityState(stateId, title, HttpMethod.GET, collectionView, HttpMethod.GET, entityView);
	}
	
	/**
	 * Add a new resource state and it's associated collection state. 
	 * @param stateId State identifier
	 * @param title Label representing this resource
	 */
	public void addCollectionAndEntityState(String stateId, String title, String collectionMethod, String collectionView, String entityMethod, String entityView) {
		addCollectionAndEntityState(stateId, title, collectionMethod, collectionView, entityMethod, entityView, null);
	}
	
	/**
	 * Add a new resource state and it's associated collection state. 
	 * @param stateId State identifier
	 * @param title Label representing this resource
	 */
	public void addCollectionAndEntityState(String stateId, String title, String collectionMethod, String collectionView, String entityMethod, String entityView, String collectionRels) {
		if(collectionView == null) {
			collectionView = Commands.GET_ENTITIES;
		}
		if(entityView == null) {
			entityView = Commands.GET_ENTITY;
		}
		
		//Add collection state
		addStateTransition(collectionState.getName(), collectionState.getName() + "_" + stateId, collectionMethod, stateId, title, collectionView, null, collectionRels);

		//Add entity state
		addStateTransition(collectionState.getName() + "_" + stateId, entityState.getName() + "_" + stateId, entityMethod, stateId, null, entityView, null, null);
	}
	
	/**
	 * Add a transition to a resource state
	 * @param title				Transition label
	 * @param sourceStateName	Source state
	 * @param targetStateName	Target state
	 * @param method			HTTP command
	 * @param action			Action to execute
	 * @param relations			Relations
	 */
	public void addStateTransition(String sourceStateName, String targetStateName, String method, String stateId, String title, String view, String action, String relations) {
		boolean boundToCollection = sourceStateName.equals(collectionState.getName()) && !targetStateName.equals(entityState.getName());	//rsm's collection state can only have transition to other collection states or to the rsm's entity state
		this.addStateTransition(sourceStateName, targetStateName, null, method, stateId, title, view, action, relations, false, boundToCollection);
	}
	
	/**
	 * Add a transition to a pseudo state.
	 * A pseudo state is an intermediate state to handle an action
	 * @param title				Transition label
	 * @param sourceStateName	Source state 
	 * @param pseudoStateId		Pseudo state identifier 
	 * @param method			HTTP command
	 * @param view				View to execute
	 * @param action			Action to execute
	 * @param relations			Relations
	 * @param boundToCollection	true if this transition should be bound to a collection state
	 */
	public IMPseudoState addPseudoStateTransition(String sourceStateName, String pseudoStateId, String method, String title, String action, String relations, boolean boundToCollection) {
		return this.addPseudoStateTransition(sourceStateName, pseudoStateId, null, method, title, action, relations, boundToCollection);
	}

	/**
	 * Add a transition to a pseudo state.
	 * A pseudo state is an intermediate state to handle an action. The transitive target state
	 * is used set the resource path of this pseudo state. If a logical transitive target state
	 * is not available, it will make up the path by appending the pseudo state id. 
	 * @param title				Transition label
	 * @param sourceStateName	Source state 
	 * @param pseudoStateId		Pseudo state identifier 
	 * @param transitiveTargetStateName	Transitive target state or null if there is no logical target state 
	 * @param method			HTTP command
	 * @param view				View to execute
	 * @param action			Action to execute
	 * @param relations			Relations
	 * @param boundToCollection	true if this transition should be bound to a collection state
	 */
	public IMPseudoState addPseudoStateTransition(String sourceStateName, String pseudoStateId, String transitiveTargetStateName, String method, String title, String action, String relations, boolean boundToCollection) {
		this.addStateTransition(sourceStateName, transitiveTargetStateName, pseudoStateId, method, null, title, null, action, relations, false, boundToCollection);
		return (IMPseudoState) getResourceState(sourceStateName + "_" + pseudoStateId);
	}
	
	/*
	 * Add a transition triggering a state change in the underlying resource manager
	 * @param title				Transition label
	 * @param sourceStateName	Source state
	 * @param targetStateName	Target state
	 * @param pseudoStateId		Pseudo state identifier 
	 * @param method			HTTP command
	 * @param stateId			State Id or null if this is the same state as the initial collection/entity state
	 * @param action			Action to execute
	 * @param relations			Relations
	 * @param boundToCollection	true if the target state should be bound to a collection state
	 */
	protected void addStateTransition(String sourceStateName, String targetStateName, String pseudoStateId, String method, String stateId, String title, String view, String action, String relations, boolean auto, boolean boundToCollection) {
		String path;
		if(pseudoStateId != null && !pseudoStateId.equals("")) {
			//This is a pseudo state
			if(targetStateName != null) {
				//This pseudo state has a logical transitive state defined
				IMState transitiveTargetState = getResourceState(targetStateName);
				if(transitiveTargetState == null) {
					throw new RuntimeException("Failed to find resource state [" + targetStateName + "].");
				}
				path = transitiveTargetState.getPath();
			}
			else {
				//Make up a path with the pseudo state id
				IMState state = getResourceState(sourceStateName);							//Source states must be created before pseudo states
				if(state == null) {
					throw new RuntimeException("Failed to find resource state [" + sourceStateName + "] for pseudo state [" + pseudoStateId + "].");
				}
				path = state.getPath() + "/" + pseudoStateId;
			}
			targetStateName = sourceStateName + "_" + pseudoStateId;
		}
		else {
			//This is a resource state
			path = sourceStateName.equals(collectionState.getName()) ? collectionState.getPath() : entityState.getPath();
			if(!(targetStateName.equals(sourceStateName) && (targetStateName.equals(entityState.getName()) || targetStateName.equals(collectionState.getName())))) {
				path = stateId != null ? getPathWithStateId(path, stateId) : path + "/" + method.toLowerCase();
			}
			//Create source state if necessary
			if(!resourceStates.containsKey(sourceStateName)) {
				resourceStates.put(sourceStateName, new IMEntityState(sourceStateName, path, view));
			}
		}
		
		//Create target state if required
		if(!resourceStates.containsKey(targetStateName)) {
			IMState targetState;
			if(pseudoStateId != null) {
				IMState sourceState = getResourceState(sourceStateName);
				if(sourceState == null) {
					throw new RuntimeException("Failed to find resource state [" + targetStateName + "] for pseudo state [" + pseudoStateId + "].");
				}
				view = sourceState.getView();
				targetState = new IMPseudoState(targetStateName, path, view, pseudoStateId, relations, action);				
			}
			else if(isCollectionState(targetStateName)) {
				//Create collection state (and entity state if required)
				String entityStateName = entityState.getName() + (stateId != null ? "_" + stateId : "");
				if(!resourceStates.containsKey(entityStateName)) {
					resourceStates.put(entityStateName, new IMEntityState(entityStateName, stateId != null ? getPathWithStateId(entityState.getPath(), stateId) : entityState.getPath() + "/" + method.toLowerCase(), view));
				}
				targetState = new IMCollectionState(targetStateName, path, view, relations, (IMEntityState) getResourceState(entityStateName));				
			}
			else if(action != null){
				targetState = new IMEntityState(targetStateName, path, relations, action);				
			}
			else {
				targetState = new IMEntityState(targetStateName, path, view);				
			}
			resourceStates.put(targetStateName, targetState);
		}
		
		//Add transition
		IMState sourceState = getResourceState(sourceStateName);
		IMState targetState = getResourceState(targetStateName);
		if(view != null && targetState.getView() != view) {
			targetState.setView(view);		//Set the view if required
		}
		if(targetState instanceof IMPseudoState) {
			sourceState.addTransitionToPseudoState(title, targetState, method, boundToCollection);
		}
		else {
			sourceState.addTransition(title, targetState, method, boundToCollection);
		}
	}
	
	/*
	 * Get specified path with the state id
	 */
	protected String getPathWithStateId(String path, String stateId) {
		StringBuffer sbPath = new StringBuffer(path);
		int iParentheses = path.indexOf('(');
		if(iParentheses >= 0) {
			sbPath.insert(iParentheses, stateId);
		}
		else {
			path += "/" + stateId;
		}
		return sbPath.toString();
	}
	
	private boolean isCollectionState(String resourceStateName) {
		IMState state = resourceStates.get(resourceStateName);
		return state == null && resourceStateName.startsWith(collectionState.getName()) || 
				state != null && state instanceof IMCollectionState;
	}

	/**
	 * Return the outgoing transitions on the specified resource state
	 * @param resourceStateName resource state name
	 * @return
	 */
	public List<IMTransition> getTransitions(String resourceStateName) {
		if(resourceStates.containsKey(resourceStateName)) {
			return resourceStates.get(resourceStateName).getTransitions();
		}
		return null;
	}

	/**
	 * Obtain a list of transitions bound to the collection state resource
	 * @return
	 */
	public List<IMTransition> getCollectionStateTransitions() {
		List<IMTransition> transitions = new ArrayList<IMTransition>();
		transitions.addAll(collectionState.getTransitions());
		transitions.addAll(entityState.getTransitions());		//Collection resources should also have those of the entity state
		return transitions;
	}

	/**
	 * Obtain a list of transitions bound to the entity state resource 
	 * @return
	 */
	public List<IMTransition> getEntityStateTransitions() {
		return entityState.getTransitions();
	}
	
	/**
	 * Obtain a sorted list of resource states
	 * @return resource states
	 */
	public Collection<IMState> getResourceStates() {
		List<IMState> states = new ArrayList<IMState>(resourceStates.values());
		if(states.size() > 0) {
			//Return sorted list of resource states
			Collections.sort(states, new Comparator<IMState>() {
				@Override
				public int compare(final IMState s1, final IMState s2) {
					//Ensure collection and entity states appear first
					if(s1.equals(collectionState) && s2.equals(entityState)) {
						return -1;
					}
					else if(s1.equals(entityState) && s2.equals(collectionState)) {
						return 1;
					}
					else if(s1.equals(collectionState) || s1.equals(entityState)) {
						return -1;
					}
					else if(s2.equals(collectionState) || s2.equals(entityState)) {
						return 1;
					}
					else {
						return s1.getName().compareToIgnoreCase(s2.getName());
					}
				}
			} );
		}		
		return states;
	}
	
	/**
	 * Return a resource state
	 * @param stateName resource state name
	 * @return state
	 */
	public IMState getResourceState(String stateName) {
		return resourceStates.get(stateName);
	}

	/**
	 * Return a pseudo state
	 * @param sourceStateName State from which this pseudo state is accesible
	 * @param pseudoStateId Pseudo state id
	 * @return state
	 */
	public IMPseudoState getPseudoState(String sourceStateName, String pseudoStateId) {
		return (IMPseudoState) getResourceState(sourceStateName + "_" + pseudoStateId);
	}
}
