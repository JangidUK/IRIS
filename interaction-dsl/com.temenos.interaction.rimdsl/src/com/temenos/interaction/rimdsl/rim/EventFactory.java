package com.temenos.interaction.rimdsl.rim;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.emf.common.util.EList;

import com.temenos.interaction.rimdsl.rim.Event;
import com.temenos.interaction.rimdsl.rim.RimFactory;

/**
 * Constructs events and add to the list.
 *
 * @author aphethean
 *
 */
public class EventFactory {
	
	private final static String GET_EVENT = "GET";
	private final static String POST_EVENT = "POST";
	
	private Map<String, Event> eventMap = new HashMap<String, Event>();
	private EList<Event> modelReferences;
	
	public EventFactory(EList<Event> modelReferences) {
		this.modelReferences = modelReferences;
	}
	
	public Event createGET() {
		if (eventMap.get(GET_EVENT) == null) {
        	Event event = RimFactory.eINSTANCE.createEvent();
			event.setHttpMethod("GET");
			event.setName(GET_EVENT);
			modelReferences.add(event);
			eventMap.put(GET_EVENT, event);
		}
		return eventMap.get(GET_EVENT);
	}
	
	public Event createPOST() {
		if (eventMap.get(POST_EVENT) == null) {
			Event event = RimFactory.eINSTANCE.createEvent();
			event.setHttpMethod("POST");
			event.setName(POST_EVENT);
			modelReferences.add(event);
			eventMap.put(POST_EVENT, event);
		}
		return eventMap.get(POST_EVENT);
	}

}
