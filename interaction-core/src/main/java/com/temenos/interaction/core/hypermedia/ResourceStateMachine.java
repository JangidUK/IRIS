package com.temenos.interaction.core.hypermedia;

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


import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriBuilderException;

import org.apache.wink.common.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.temenos.interaction.core.MultivaluedMapImpl;
import com.temenos.interaction.core.command.InteractionCommand;
import com.temenos.interaction.core.command.InteractionContext;
import com.temenos.interaction.core.command.InteractionException;
import com.temenos.interaction.core.command.NewCommandController;
import com.temenos.interaction.core.hypermedia.expression.Expression;
import com.temenos.interaction.core.resource.CollectionResource;
import com.temenos.interaction.core.resource.EntityResource;
import com.temenos.interaction.core.resource.MetaDataResource;
import com.temenos.interaction.core.resource.RESTResource;
import com.temenos.interaction.core.rim.HTTPHypermediaRIM;
import com.temenos.interaction.core.rim.ResourceRequestConfig;
import com.temenos.interaction.core.rim.ResourceRequestHandler;
import com.temenos.interaction.core.rim.ResourceRequestResult;
import com.temenos.interaction.core.web.RequestContext;
import com.temenos.interaction.core.workflow.AbortOnErrorWorkflowStrategyCommand;

/**
 * A state machine that is responsible for creating the links (hypermedia) to other
 * valid application states.
 * @author aphethean
 *
 */
public class ResourceStateMachine {
	private final Logger logger = LoggerFactory.getLogger(ResourceStateMachine.class);
	
	public final ResourceState initial;
	public final ResourceState exception;
	public final Transformer transformer;
	public NewCommandController commandController;
	
	// optimised access
	private Map<String,Transition> transitionsById = new HashMap<String,Transition>();
	private Map<String,Transition> transitionsByRel = new HashMap<String,Transition>();
	private List<ResourceState> allStates = new ArrayList<ResourceState>();
	private Map<String, Set<String>> interactionsByPath = new HashMap<String, Set<String>>();
	private Map<ResourceState, Set<String>> interactionsByState = new HashMap<ResourceState, Set<String>>();
	private Map<String, Set<ResourceState>> resourceStatesByPath = new HashMap<String, Set<ResourceState>>();
	private Map<String, ResourceState> resourceStatesByName = new HashMap<String, ResourceState>();
	
	public ResourceStateMachine(ResourceState initialState) {
		this(initialState, null, null);
	}

	public ResourceStateMachine(ResourceState initialState, ResourceState exceptionState) {
		this(initialState, exceptionState, null);
	}
	
	public NewCommandController getCommandController() {
		return commandController;
	}

	public void setCommandController(NewCommandController commandController) {
		this.commandController = commandController;
	}

	// TODO support Event
	public InteractionCommand determineAction(Event event, String resourcePath) {
		List<Action> actions = new ArrayList<Action>();
		Set<ResourceState> resourceStates = getResourceStatesByPath().get(resourcePath);
		for (ResourceState s : resourceStates) {
			actions.addAll(determineActions(event, s));
		}
		return buildWorkflow(actions);
	}
	
	public List<Action> determineActions(Event event, ResourceState state) {
		List<Action> actions = new ArrayList<Action>();
		Set<String> interactions = getInteractionByState().get(state);
		// TODO turn interactions into Events
		if (interactions.contains(event.getMethod())) {
			for (Action a : state.getActions()) {
				if (event.isSafe() && a.getType().equals(Action.TYPE.VIEW)) {
					// catch problem if overriding existing view actions 
//					assert(actions.size() == 0) : "Multiple view actions detected";
					if (actions.size() == 0)
						actions.add(a);
				} else if (event.isUnSafe() && a.getType().equals(Action.TYPE.ENTRY)) {
					actions.add(a);
				}
			}
		}
		return actions;
	}
	
	public InteractionCommand buildWorkflow(List<Action> actions) {
		if (actions.size() > 0) {
			AbortOnErrorWorkflowStrategyCommand workflow = new AbortOnErrorWorkflowStrategyCommand();
			for (Action action : actions) {
				assert(action != null);
				workflow.addCommand(getCommandController().fetchCommand(action.getName()));
			}
			return workflow;
		}
		return null;
	}
	
	public ResourceState determineState(Event event, String resourcePath) {
		ResourceState state = null;
		Set<ResourceState> resourceStates = getResourceStatesByPath().get(resourcePath);
		if (resourceStates != null) {
			for (ResourceState s : resourceStates) {
				Set<String> interactions = getInteractionByState().get(s);
				if (interactions.contains(event.getMethod())) {
					if(state == null || interactions.size() == 1 || !event.getMethod().equals("GET")) {		//Avoid overriding existing view actions
						state = s;
					}
				}
			}
		}
		
		return state;
	}

	/**
	 * 
	 * @invariant initial state not null
	 * @param initialState
	 * @param transformer
	 */
	public ResourceStateMachine(ResourceState initialState, Transformer transformer) {
		this(initialState, null, transformer);
	}
	
	
	public ResourceStateMachine(ResourceState initialState, ResourceState exceptionState, Transformer transformer) {
		if (initialState == null) throw new RuntimeException("Initial state must be supplied");
		logger.info("Constructing ResourceStateMachine with initial state ["+initialState+"]");
		assert(exceptionState == null || exceptionState.isException());
		this.initial = initialState;
		this.initial.setInitial(true);
		this.exception = exceptionState;
		this.transformer = transformer;
		build();
	}

	private void build() {
		collectStates(allStates, initial);
		collectTransitionsById(transitionsById);
		collectTransitionsByRel(transitionsByRel);
		collectInteractionsByPath(interactionsByPath);
		collectInteractionsByState(interactionsByState);
		collectResourceStatesByPath(resourceStatesByPath);
		collectResourceStatesByName(resourceStatesByName);
	}
	
	public ResourceState getInitial() {
		return initial;
	}

	public ResourceState getException() {
		return exception;
	}

	public Transformer getTransformer() {
		return transformer;
	}

	public Collection<ResourceState> getStates() {
		return allStates;
	}

	private void collectStates(Collection<ResourceState> result, ResourceState currentState) {
		if (result.contains(currentState)) return;
		result.add(currentState);
		for (ResourceState next : currentState.getAllTargets()) {
			if (!next.equals(initial)) {
				collectStates(result, next);
			}
		}
		
	}

	private void collectTransitionsById(Map<String,Transition> transitions) {
		for (ResourceState s : getStates()) {
			for (ResourceState target : s.getAllTargets()) {
				for(Transition transition : s.getTransitions(target)) {
					transitions.put(transition.getId(), transition);
				}
			}
		}
	}

	private void collectTransitionsByRel(Map<String,Transition> transitions) {
		for (ResourceState s : getStates()) {
			for (ResourceState target : s.getAllTargets()) {
				for(Transition transition : s.getTransitions(target)) {
					transitions.put(transition.getTarget().getRel(), transition);
				}
			}
		}
	}

	/**
	 * Return a map of all the paths, and interactions with those states
	 * mapped to that path
	 * @return
	 */
	public Map<String, Set<String>> getInteractionByPath() {
		return interactionsByPath;
	}
	
	private void collectInteractionsByPath(Map<String, Set<String>> result) {
		List<ResourceState> states = new ArrayList<ResourceState>();
		collectInteractionsByPath(result, states, initial);
	}
	
	private void collectInteractionsByPath(Map<String, Set<String>> result, Collection<ResourceState> states, ResourceState currentState) {
		if (currentState == null || states.contains(currentState)) return;
		states.add(currentState);
		// every state must have a 'GET' interaction
		Set<String> interactions = result.get(currentState.getPath());
		if (interactions == null)
			interactions = new HashSet<String>();
		interactions.add(HttpMethod.GET);
		result.put(currentState.getPath(), interactions);
		// add interactions by iterating through the transitions from this state
		for (ResourceState next : currentState.getAllTargets()) {
			List<Transition> transitions = currentState.getTransitions(next);
			for(Transition t : transitions) {
				TransitionCommandSpec command = t.getCommand();
				String path = command.getPath();
				
				interactions = result.get(path);
				if (interactions == null)
					interactions = new HashSet<String>();
				if (command.getMethod() != null && !command.isAutoTransition())
					interactions.add(command.getMethod());
				
				result.put(path, interactions);
				collectInteractionsByPath(result, states, next);
			}
		}
		
	}

	/**
	 * Return a map of all the ResourceState's, and interactions with those states.
	 * @return
	 */
	public Map<ResourceState, Set<String>> getInteractionByState() {
		return interactionsByState;
	}
	
	private void collectInteractionsByState(Map<ResourceState, Set<String>> result) {
		List<ResourceState> states = new ArrayList<ResourceState>();
		collectInteractionsByState(result, states, initial);
	}
	
	private void collectInteractionsByState(Map<ResourceState, Set<String>> result, Collection<ResourceState> states, ResourceState currentState) {
		if (currentState == null || states.contains(currentState)) return;
		states.add(currentState);
		// every state must have a 'GET' interaction
		Set<String> interactions = result.get(currentState);
		if (interactions == null)
			interactions = new HashSet<String>();
		if (!currentState.isPseudoState()) {
			interactions.add(HttpMethod.GET);
		}
		result.put(currentState, interactions);
		// add interactions by iterating through the transitions from this state
		for (ResourceState next : currentState.getAllTargets()) {
			List<Transition> transitions = currentState.getTransitions(next);
			for(Transition t : transitions) {
				TransitionCommandSpec command = t.getCommand();
				
				interactions = result.get(next);
				if (interactions == null)
					interactions = new HashSet<String>();
				if (command.getMethod() != null && !command.isAutoTransition())
					interactions.add(command.getMethod());
				
				result.put(next, interactions);
				collectInteractionsByState(result, states, next);
			}
		}
		
	}

	/**
	 * For a given resource state, get the valid interactions.
	 * @param state
	 * @return
	 */
	public Set<String> getInteractions(ResourceState state) {
		Set<String> interactions = null;
		if(state != null) {
			assert(getStates().contains(state));
			Map<String, Set<String>> interactionMap = getInteractionByPath();
			interactions = interactionMap.get(state.getPath());
		}
		return interactions;
	}
	
	/**
	 * For a given path, return the resource states.
	 * @param state
	 * @return
	 */
	public Set<ResourceState> getResourceStatesForPath(String path) {
		if (path == null) {
			path = initial.getPath();
		}
		return getResourceStatesByPath().get(path);
	}

	/**
	 * For a given path regular expression, return the resource states.
	 * @param state
	 * @return
	 */
	public Set<ResourceState> getResourceStatesForPathRegex(String pathRegex) {
		if (pathRegex == null) {
			pathRegex = initial.getPath();
		}
		return getResourceStatesForPathRegex(Pattern.compile(pathRegex));
	}

	/**
	 * @see {@link ResourceStateMachine#getResourceStatesForPathRegex(String)}
	 */
	public Set<ResourceState> getResourceStatesForPathRegex(Pattern pattern) {
		Set<ResourceState> matchingStates = new HashSet<ResourceState>();
		Set<String> paths = resourceStatesByPath.keySet();
		for (String path : paths) {
			Matcher m = pattern.matcher(path);
			if (m.matches()) {
				matchingStates.addAll(getResourceStatesForPath(path));
			}
		}
		return matchingStates;
	}

	/**
	 * Return a map of all the paths to the various ResourceState's
	 * @invariant initial state not null
	 * @return
	 */
	public Map<String, Set<ResourceState>> getResourceStatesByPath() {
		return resourceStatesByPath;
	}

	/**
	 * Return a map of all the paths to the sub states from the supplied
	 * ResourceState.
	 * @precondition begin state not null
	 * @invariant initial state not null
	 */
	public Map<String, Set<ResourceState>> getResourceStatesByPath(ResourceState begin) {
		assert(begin != null);
		Map<String, Set<ResourceState>> stateMap = new HashMap<String, Set<ResourceState>>();
		collectResourceStatesByPath(stateMap, begin);
		return stateMap;
	}

	private void collectResourceStatesByPath(Map<String, Set<ResourceState>> result) {
		collectResourceStatesByPath(result, initial);
	}

	private void collectResourceStatesByPath(Map<String, Set<ResourceState>> result, ResourceState begin) {
		List<ResourceState> states = new ArrayList<ResourceState>();
		collectResourceStatesByPath(result, states, begin);
	}

	private void collectResourceStatesByPath(Map<String, Set<ResourceState>> result, Collection<ResourceState> states, ResourceState currentState) {
		if (currentState == null || states.contains(currentState)) return;
		states.add(currentState);
		// add current state to results
		Set<ResourceState> thisStateSet = result.get(currentState.getResourcePath());
		if (thisStateSet == null)
			thisStateSet = new HashSet<ResourceState>();
		thisStateSet.add(currentState);
		result.put(currentState.getResourcePath(), thisStateSet);
		for (ResourceState next : currentState.getAllTargets()) {
//			if (!next.equals(currentState) && !next.isPseudoState()) {
			if (!next.equals(currentState)) {
				String path = next.getResourcePath();
				if (result.get(path) != null) {
					if (!result.get(path).contains(next)) {
						logger.debug("Adding to existing ResourceState[" + path + "] set (" + result.get(path) + "): " + next);
						result.get(path).add(next);
					}
				} else {
					logger.debug("Putting a ResourceState[" + path + "]: " + next);
					Set<ResourceState> set = new HashSet<ResourceState>();
					set.add(next);
					result.put(path, set);
				}
			}
			collectResourceStatesByPath(result, states, next);
		}
	}

	/**
	 * For a given state name, return the resource state.
	 * @param name
	 * @return
	 */
	public ResourceState getResourceStateByName(String name) {
		if (name == null)
			throw new IllegalArgumentException("State name not supplied");
		return getResourceStateByName().get(name);
	}

	/**
	 * Return a map of all the state names to ResourceState
	 * @invariant initial state not null
	 * @return
	 */
	public Map<String, ResourceState> getResourceStateByName() {
		return resourceStatesByName;
	}

	/**
	 * Return a map of all the state names to the sub states from the supplied
	 * ResourceState.
	 * @precondition begin state not null
	 * @invariant initial state not null
	 */
	public Map<String, ResourceState> getResourceStateByName(ResourceState begin) {
		assert(begin != null);
		Map<String, ResourceState> stateMap = new HashMap<String, ResourceState>();
		collectResourceStatesByName(stateMap, begin);
		return stateMap;
	}

	private void collectResourceStatesByName(Map<String, ResourceState> result) {
		collectResourceStatesByName(result, initial);
	}

	private void collectResourceStatesByName(Map<String, ResourceState> result, ResourceState begin) {
		List<ResourceState> states = new ArrayList<ResourceState>();
		collectResourceStatesByName(result, states, begin);
	}

	private void collectResourceStatesByName(Map<String, ResourceState> result, Collection<ResourceState> states, ResourceState currentState) {
		if (currentState == null || states.contains(currentState)) return;
		states.add(currentState);
		// add current state to results
		result.put(currentState.getName(), currentState);
		for (ResourceState next : currentState.getAllTargets()) {
			if (!next.equals(currentState)) {
				String name = next.getName();
				logger.debug("Putting a ResourceState[" + name + "]: " + next);
				result.put(name, next);
			}
			collectResourceStatesByName(result, states, next);
		}
	}

	/**
	 * Evaluate and return all the valid links (target states) from this resource state.
	 * @param rimHandler
	 * @param ctx
	 * @param resourceEntity
	 * @return
	 */
	public Collection<Link> injectLinks(HTTPHypermediaRIM rimHandler, InteractionContext ctx, RESTResource resourceEntity) {
		return injectLinks(rimHandler, ctx, resourceEntity, null);
	}
	/**
	 * Evaluate and return all the valid links (target states) from the current
	 * resource state (@see {@link InteractionContext#getCurrentState()}).
	 * @param rimHandler
	 * @param ctx
	 * @param resourceEntity
	 * @param selfTransition if we are injecting links into a resource that has resulted
	 *                       from a transition from another resource (e.g an auto transition
	 *                       or an embedded transition) then we need to use the transition 
	 *                       parameters as there are no path parameters available.
	 *                       i.e. because we've not made a request for this resource 
	 *                       through the whole jax-rs stack
	 * @return
	 */
	public Collection<Link> injectLinks(HTTPHypermediaRIM rimHander, InteractionContext ctx, RESTResource resourceEntity, Transition selfTransition) {
		//Add path and query parameters to the list of resource properties
		MultivaluedMap<String, String> resourceProperties = new MultivaluedMapImpl<String>();
		resourceProperties.putAll(ctx.getPathParameters());
		resourceProperties.putAll(ctx.getQueryParameters());

		ResourceState state = ctx.getCurrentState();
		List<Link> links = new ArrayList<Link>();
		if (resourceEntity == null)
			return links;
		
		Object entity = null;
		CollectionResource<?> collectionResource = null;
		if (resourceEntity instanceof EntityResource) {
			entity = ((EntityResource<?>) resourceEntity).getEntity();
		} else if (resourceEntity instanceof CollectionResource) {
			collectionResource = (CollectionResource<?>) resourceEntity;
			// TODO add support for properties on collections
			logger.warn("Injecting links into a collection, only support simple, non template, links as there are no properties on the collection at the moment");
		} else if (resourceEntity instanceof MetaDataResource) {
			// TODO deprecate all resource types apart from item (EntityResource) and collection (CollectionResource)
			logger.debug("Returning from the call to getLinks for a MetaDataResource without doing anything");
			return links;
		} else {
			throw new RuntimeException("Unable to get links, an error occurred");
		}
		
		// add link to GET 'self'
		if (selfTransition == null)
			selfTransition = state.getSelfTransition();
		links.add(createSelfLink(selfTransition, entity, resourceProperties));

		/*
		 * Add links to other application states (resources)
		 */
		Collection<ResourceState> targetStates = state.getAllTargets();
		for (ResourceState s : targetStates) {
			List<Transition> transitions = state.getTransitions(s);
			for(Transition transition : transitions) {
				TransitionCommandSpec cs = transition.getCommand();
				/* 
				 * build link and add to list of links
				 */
				if (cs.isForEach()) {
					if (collectionResource != null) {
						for (EntityResource<?> er : collectionResource.getEntities()) {
							Collection<Link> eLinks = er.getLinks();
							if (eLinks == null) {
								eLinks = new ArrayList<Link>();
							}
							eLinks.add(createLink(transition, er.getEntity(), resourceProperties));
							er.setLinks(eLinks);
						}
					}
				} else {
					boolean addLink = true;
					// evaluate the conditional expression
					Expression conditionalExp = cs.getEvaluation();
					if (conditionalExp != null) {
						addLink = conditionalExp.evaluate(rimHander, ctx);
					}
						
					if (addLink) {
						links.add(createLink(transition, entity, resourceProperties));
					}
				}
			}
		}
		resourceEntity.setLinks(links);
		return links;
	}

	/**
	 * Execute and return all the valid embedded links (target states) from the supplied
	 * resource.  Should be identical to {@link InteractionContext#getResource()}.
	 * @param rimHandler
	 * @param headers
	 * @param ctx
	 * @param resource
	 * @return
	 */
    public Map<Transition, RESTResource> embedResources(HTTPHypermediaRIM rimHandler, HttpHeaders headers, InteractionContext ctx, RESTResource resource) {
		ResourceRequestHandler resourceRequestHandler = rimHandler.getResourceRequestHandler();
		assert(resourceRequestHandler != null);
    	try {
			ResourceRequestConfig.Builder configBuilder = new ResourceRequestConfig.Builder();
			Collection<Link> links = resource.getLinks();
			if (links != null) {
				for (Link link : links) {
					Transition t = link.getTransition();
					/*
					 * when embedding resources we don't want to embed ourselves
					 * we only want to embed the 'EMBEDDED' transitions
					 */
					if (!t.getSource().equals(t.getTarget()) &&
							(t.getCommand().getFlags() & Transition.EMBEDDED) == Transition.EMBEDDED) {
						configBuilder.transition(t);
					}
				}
			}

			ResourceRequestConfig config = configBuilder.build();
			Map<Transition, ResourceRequestResult> results = resourceRequestHandler.getResources(rimHandler, headers, ctx, null, config);
			if(config.getTransitions() != null && config.getTransitions().size() > 0
					&& config.getTransitions().size() != results.keySet().size()) {
				throw new InteractionException(Status.INTERNAL_SERVER_ERROR, "Resource state [" + ctx.getCurrentState().getId() + "] did not return correct number of embedded resources.");
			}
			// don't replace any embedded resources added within the commands
			Map<Transition, RESTResource> resourceResults = null;
			if (ctx.getResource() != null && ctx.getResource().getEmbedded() != null) {
				resourceResults = ctx.getResource().getEmbedded();
			} else {
				resourceResults = new HashMap<Transition, RESTResource>();
			}
			for (Transition transition : results.keySet()) {
				ResourceRequestResult result = results.get(transition);
				if (result.getStatus() != HttpStatus.OK.getCode()) {
					logger.error("Failed to embed resource for transition [" + transition.getId() + "]");
				} else {
					resourceResults.put(transition, result.getResource());
				}
			}
			resource.setEmbedded(resourceResults);
			return resourceResults;
		} catch(InteractionException ie) {
			logger.error("Failed to embed resources [" + ctx.getCurrentState().getId() + "] with error [" + ie.getHttpStatus() + " - " + ie.getHttpStatus().getReasonPhrase() + "]: " + ie.getMessage());
			throw new RuntimeException(ie);
		}
    }

	/**
	 * Find the transition that was used by evaluating the LinkHeader and 
	 * create a a Link for that transition.
	 * @param pathParameters
	 * @param resourceEntity
	 * @param customLinkRelations
	 * @return
	 */
	public Link getLinkFromRelations(MultivaluedMap<String, String> pathParameters, RESTResource resourceEntity, LinkHeader linkHeader) {
		Link target = null;
		// Was a custom link relation supplied, informing us which link was used?
		if (linkHeader != null) {
			Set<String> relationships = linkHeader.getLinksByRelationship().keySet();
			for (String related : relationships) {
				Transition transition = getTransitionsById().get(related);
				if (transition != null) {
					target = createLinkToTarget(transition, resourceEntity, pathParameters);
				}
			}
		}
		return target;
	}

	public Map<String,Transition> getTransitionsById() {
		return transitionsById;
	}

	public Map<String,Transition> getTransitionsByRel() {
		return transitionsByRel;
	}

	/**
	 * Find the transition that was used by assuming the HTTP method was
	 * applied to this state; create a a Link for that transition.
	 * @param pathParameters
	 * @param resourceEntity
	 * @param currentState
	 * @param method
	 * @return
	 * @invariant method != null
	 */
	public Link getLinkFromMethod(MultivaluedMap<String, String> pathParameters, RESTResource resourceEntity, ResourceState currentState, String method) {
		assert(method != null);
		Link target = null;
		for (ResourceState nextState : currentState.getAllTargets()) {
			Transition transition = currentState.getTransition(nextState);
			if (method.contains(transition.getCommand().getMethod())) {
				// do not create link if this a pseudo state, effectively no state
				if (!transition.getTarget().isPseudoState())
					target = createLinkToTarget(transition, resourceEntity, pathParameters);
			}
		}
		return target;
	}

	/*
	 * @invariant {@link RequestContext} must have been initialised
	 */
	public Link createLinkToTarget(Transition transition, Object entity, MultivaluedMap<String, String> pathParameters) {
		return createLink(transition, entity, pathParameters);
	}
	/*
	 * create link to target preserving the query parameters, useful for redirect
	 */
	public Link createLinkToTarget(Transition transition, Object entity, MultivaluedMap<String, String> pathParameters, MultivaluedMap<String, String> queryParameters) {
		return createLink(transition, entity, pathParameters, queryParameters);
	}

	/*
	 * @precondition {@link RequestContext} must have been initialised
	 */
	private Link createSelfLink(Transition transition, Object entity, MultivaluedMap<String, String> pathParameters) {
		TransitionCommandSpec cs = transition.getCommand();
		return createLink(cs.getPath(), transition, entity, pathParameters, null);
	}

	/*
	 * Create a Link using the supplied transition, entity and path parameters
	 * @param resourcePath uri template resource path
	 * @param transition transition
	 * @param entity entity
	 * @param map path parameters
	 * @return link
	 */
	public Link createLink(Transition transition, Object entity, MultivaluedMap<String, String> transitionParameters) {
		return createLink(transition, entity, transitionParameters, null);
	}
	public Link createLink(Transition transition, Object entity, MultivaluedMap<String, String> transitionParameters, MultivaluedMap<String, String> queryParameters) {
		TransitionCommandSpec cs = transition.getCommand();
		return createLink(cs.getPath(), transition, entity, transitionParameters, queryParameters);
	}
	private Link createLink(String resourcePath, Transition transition, Object entity, MultivaluedMap<String, String> transitionParameters, MultivaluedMap<String, String> queryParameters) {
		Map<String, Object> transitionProperties = getTransitionProperties(transition, entity, transitionParameters);
		return createLink(resourcePath, transition, transitionProperties, entity, queryParameters);
	}

	/*
	 * Create a link using the supplied transition, entity and transition properties.
	 * This method is intended for re-using transition properties (path params, link params
	 * and entity properties).
	 * @param linkTemplate uri template
	 * @param transition transition
	 * @param transitionProperties transition properties
	 * @param entity entity
	 * @return link
	 * @precondition {@link RequestContext} must have been initialised
	 */
	private Link createLink(String resourcePath, Transition transition, Map<String, Object> transitionProperties, Object entity, MultivaluedMap<String, String> queryParameters) {
		assert(RequestContext.getRequestContext() != null);
		TransitionCommandSpec cs = transition.getCommand();
		UriBuilder linkTemplate = UriBuilder.fromUri(RequestContext.getRequestContext().getBasePath())
				.path(resourcePath);
		try {
			String rel = transition.getTarget().getRel();
			if (transition.getSource().equals(transition.getTarget())) {
				rel = "self"; 
			}
			String method = cs.getMethod();

			//Add template elements in linkage properties e.g. filter=fld eq {code} as query parameters
			Map<String, String> linkParameters = transition.getCommand().getUriParameters();
			setQueryParameters(linkTemplate, linkParameters, transitionProperties, resourcePath);
			
			// Pass any query parameters
			if (queryParameters != null) {
				for (String param : queryParameters.keySet()) {
					linkTemplate.queryParam(param, queryParameters.getFirst(param));
				}
			}
			
			//Build href from template
			URI href;
			if(entity != null && transformer == null) {
				logger.debug("Building link with entity (No Transformer) [" + entity + "] [" + transition + "]");
				href = linkTemplate.build(entity);
			}
			else {
				href = linkTemplate.buildFromMap(transitionProperties);
			}
			
			//Create the link
			Link link = new Link(transition, rel, href.toASCIIString(), method);
			logger.debug("Created link for transition [" + transition + "] [title=" + transition.getId()+ ", rel=" + rel + ", method=" + method + ", href=" + href.toString() + "(ASCII=" + href.toASCIIString() + ")]");
			return link;
		} catch (IllegalArgumentException e) {
			logger.error("An error occurred while creating link [" +  transition + "]", e);
			throw e;
		} catch (UriBuilderException e) {
			logger.error("An error occurred while creating link [" + transition + "]", e);
			throw e;
		}
	}
	
	/**
	 * Obtain transition properties.
	 * Transition properties are a list of path parameters,
	 * linkage properties and entity properties.
	 * @param transition transition
	 * @param entity usually an entity of the source state
	 * @param pathParameters path parameters
	 * @return map of transition properties
	 */
	public Map<String, Object> getTransitionProperties(Transition transition, Object entity, MultivaluedMap<String, String> pathParameters) {
		Map<String, Object> transitionProps = new HashMap<String, Object>();

		//Obtain path parameters
		if (pathParameters != null) {
			for (String key : pathParameters.keySet()) {
				transitionProps.put(key, pathParameters.getFirst(key));
			}
		}

		//Obtain entity properties
		Map<String, Object> entityProperties = null;
		if (entity != null && transformer != null) {
			logger.debug("Using transformer [" + transformer + "] to build properties for link [" + transition + "]");
			entityProperties = transformer.transform(entity);
			if (entityProperties != null) {
				transitionProps.putAll(entityProperties);
			}
		}

		//Obtain linkage properties
		Map<String, String> linkParameters = transition.getCommand().getUriParameters();
		if (linkParameters != null) {
			for (String key : linkParameters.keySet()) {
				String value = linkParameters.get(key);
				value = HypermediaTemplateHelper.templateReplace(value, transitionProps);
				transitionProps.put(key, value);
			}
		}
		
		return transitionProps;
	}
	
	/**
	 * Set template elements in link properties as query parameters.
	 * e.g. filter=fld eq {code} => ?code=123
	 * e.g. filter=fld eq {code}, code="mycode" => ?mycode=123
	 * @param linkTemplate URI builder
	 * @param linkParameters link properties
	 * @param properties entity and link properties
	 * @param targetStatePath Resource path of target state
	 */
	protected void setQueryParameters(UriBuilder linkTemplate, Map<String, String> linkParameters, Map<String, Object> properties, String targetStatePath) {
		if (linkParameters != null) {
			for(String key : linkParameters.keySet()) {
				String value = linkParameters.get(key);
				if (targetStatePath.contains("{"+key+"}")) {
// TODO, works fine without this, making this whole method dead code?
//					value = templateReplace(value, properties);
				} else {
					linkTemplate.queryParam(key, value);
				}
			}
		}
	}
	
	public InteractionCommand determinAction(String event, String path) {
		return null;
	}

}
