package com.temenos.interaction.core.hypermedia.expression;

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


import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.core.MultivaluedMap;

import org.apache.wink.common.http.HttpStatus;

import com.temenos.interaction.core.command.InteractionContext;
import com.temenos.interaction.core.hypermedia.HypermediaTemplateHelper;
import com.temenos.interaction.core.hypermedia.ResourceState;
import com.temenos.interaction.core.hypermedia.ResourceStateMachine;
import com.temenos.interaction.core.hypermedia.Transition;
import com.temenos.interaction.core.resource.EntityResource;
import com.temenos.interaction.core.resource.RESTResource;
import com.temenos.interaction.core.rim.HTTPHypermediaRIM;
import com.temenos.interaction.core.rim.ResourceRequestConfig;
import com.temenos.interaction.core.rim.ResourceRequestResult;
import com.temenos.interaction.core.rim.SequentialResourceRequestHandler;

public class ResourceGETExpression implements Expression {

	public enum Function {
		OK,
		NOT_FOUND
	}
	
	public final Function function;
	public final String state;
	public final Transition transition;
	public final Set<Transition> transitions = new HashSet<Transition>();
	
	public ResourceGETExpression(ResourceState target, Function function) {
		this.function = function;
		this.state = null;
		this.transition = new Transition.Builder().method("GET").target(target).flags(Transition.EXPRESSION).build();
		this.transitions.add(transition);
	}

	// keep old way until 0.5.0
	@Deprecated
	public ResourceGETExpression(String state, Function function) {
		this.function = function;
		this.state = state;
		this.transition = null;
	}
	
	public Function getFunction() {
		return function;
	}

	public String getState() {
		String state = null;
		if (transition != null) {
			state = transition.getTarget().getName();
		}
		return state;
	}
	
	@Override
	public boolean evaluate(HTTPHypermediaRIM rimHandler, InteractionContext ctx) {
		ResourceStateMachine hypermediaEngine = rimHandler.getHypermediaEngine();
		ResourceState target = null;
		Transition ourTransition = transition;
		if (ourTransition == null) {
			target = hypermediaEngine.getResourceStateByName(state);
			ourTransition = ctx.getCurrentState().getTransition(target);
		} else {
			target = ourTransition.getTarget();
		}
    	assert(ourTransition != null);
		if (target == null)
			throw new IllegalArgumentException("Indicates a problem with the RIM, it allowed an invalid state to be supplied");
		assert(target.getActions() != null);
		assert(target.getActions().size() == 1);
		
		//Create a new interaction context for this state
    	MultivaluedMap<String, String> pathParameters = getPathParametersForTargetState(hypermediaEngine, ctx, ourTransition);
    	InteractionContext newCtx = new InteractionContext(ctx, null, pathParameters, null, target);

    	//Get the target resource
		ResourceRequestConfig config = new ResourceRequestConfig.Builder()
				.transition(ourTransition)
				.injectLinks(false)
				.embedResources(false)
				.build();
		Map<Transition, ResourceRequestResult> results = new SequentialResourceRequestHandler().getResources(rimHandler, null, newCtx, null, config);
		assert(results.values() != null && results.values().size() == 1);
		ResourceRequestResult result = results.values().iterator().next();
		
		//Ignore the resource and its links, just interested in the result status
		if (HttpStatus.OK.getCode() == result.getStatus() 
				&& getFunction().equals(Function.OK)) {
			return true;
		}
		if (HttpStatus.OK.getCode() != result.getStatus() 
				&& getFunction().equals(Function.NOT_FOUND)) {
			return true;
		}
		return false;
	}

	@Override
	public Set<Transition> getTransitions() {
		return transitions;
	}

	public String toString() {
		StringBuffer sb = new StringBuffer();
		if (getFunction().equals(ResourceGETExpression.Function.OK))
			sb.append("OK(").append(getState()).append(")");
		if (getFunction().equals(ResourceGETExpression.Function.NOT_FOUND))
			sb.append("NOT_FOUND").append(getState()).append(")");
		return sb.toString();
	}
	
	/*
	 * Obtain path parameters to use when accessing
	 * a resource state on an expression.  
	 */
	private MultivaluedMap<String, String> getPathParametersForTargetState(ResourceStateMachine hypermediaEngine, InteractionContext ctx, Transition transition) {
    	Map<String, Object> transitionProperties = new HashMap<String, Object>();
    	// by default add all the path parameters to access the target
    	if (ctx.getPathParameters() != null) {
    		for (String key : ctx.getPathParameters().keySet()){
    			transitionProperties.put(key, ctx.getPathParameters().getFirst(key));
    		}
    	}
   		RESTResource resource = ctx.getResource();
   		if(resource != null && resource instanceof EntityResource) {
   			Object entity = ((EntityResource<?>) resource).getEntity();
   	    	transitionProperties.putAll(hypermediaEngine.getTransitionProperties(transition, entity, null)); 
   		}    		
    	
    	//apply transition properties to path parameters 
    	return HypermediaTemplateHelper.getPathParametersForTargetState(transition, transitionProperties);
	}
}
