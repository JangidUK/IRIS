package com.temenos.interaction.media.odata.xml.atom;

/*
 * #%L
 * interaction-media-odata-xml
 * %%
 * Copyright (C) 2012 - 2014 Temenos Holdings N.V.
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


import org.odata4j.format.xml.XmlFormatWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.temenos.interaction.core.hypermedia.CollectionResourceState;
import com.temenos.interaction.core.hypermedia.Link;
import com.temenos.interaction.core.hypermedia.ResourceState;
import com.temenos.interaction.core.hypermedia.Transition;
import com.temenos.interaction.core.resource.RESTResource;

public class ODataLinkInterceptor implements LinkInterceptor {
	private final Logger logger = LoggerFactory.getLogger(ODataLinkInterceptor.class);

	// this class uses AtomXMLProvider as a helper
	private AtomXMLProvider providerHelper;
	
	public ODataLinkInterceptor(AtomXMLProvider providerHelper) {
		this.providerHelper = providerHelper;
	}
	
	@Override
	public Link addingLink(RESTResource resource, Link linkToAdd) {
		assert(resource != null);
		logger.debug("Link rel["+linkToAdd.getRel()+"] title["+linkToAdd.getTitle()+"] href["+linkToAdd.getHref()+"]");

		Link result = linkToAdd;
		String rel = getODataLinkRelation(result, providerHelper.getEntitySet(result.getTransition().getTarget()));
		
		/*
		 * Identify 'self' link
		 */
		Link selfLink = null;
		for (Link link : resource.getLinks()) {
			// prefer edit
			if ((selfLink == null && ("self".equals(link.getRel()) || "edit".equals(link.getRel())))
					|| (selfLink != null && !"edit".equals(selfLink.getRel()) && "edit".equals(link.getRel()))) {
				selfLink = link;
			}
		}
		if (selfLink != null && !selfLink.equals(linkToAdd)
				&& (linkToAdd.getRel().equals("item") || linkToAdd.getRel().equals("collection") 
						|| linkToAdd.getRel().equals("self") || linkToAdd.getRel().equals("edit"))
				&& linkToAdd.getHref().equals(selfLink.getHref())) {
			result = null;
		}

		/*
		 * Remove duplicate links (mainly for the 'edit' case)
		 */
		if (result != null) {
			Link firstInstance = null;
			for (Link link : resource.getLinks()) {
				// is this the first instance of this rel/href combination
				if (firstInstance != null
						&& !firstInstance.equals(result)
						&& rel.equals(link.getRel())
						&& result.getHref().equals(link.getHref())) {
					result = null;
					break;
				}
				if (result.getRel().equals(link.getRel())
						&& result.getHref().equals(link.getHref())) {
					firstInstance = link;
				}
			}
		}

		if (result != null) {
			result = new Link(result.getTransition(), 
					result.getTitle(), 
					rel, 
					result.getHref(), 
					result.getConsumes(), 
					result.getProduces(), 
					result.getMethod(), 
					result.getExtensions());
		}
		return result;
	}

	/**
	 * Return the OData link relation from the specified link.
	 * @param link link
	 * @return odata link rel
	 */
	public String getODataLinkRelation(Link link, String entitySetName) {
//		assert(entitySetName != null);
		String rel = link.getRel();
		Transition transition = link.getTransition();
		if(transition == null) {
			return rel;
		}
		// hack, just until we fix this up
		rel = rel.replace("item", "");
		rel = rel.replace("collection", "");

		// don't change the link relations if they have been specified in the RIM
		if (rel.length() > 0)
			return rel;
		
		if (transition.isGetFromCollectionToEntityResource() || (rel.equals("self") || rel.equals("edit"))) {
			if (rel.length() == 0) {
				//Links from collection to entity resource of an entity are considered 'self' links within an odata feed
				rel = "self";
			}
		} else if (transition.getTarget() instanceof CollectionResourceState) {
			rel = XmlFormatWriter.related + entitySetName + (rel != null && rel.length() > 0 ? " " : "") + rel;
		} else if (transition.getTarget() instanceof ResourceState) {
			//entry type relations should use the entityType name
			rel = XmlFormatWriter.related + transition.getTarget().getEntityName() + (rel != null && rel.length() > 0 ? " " : "") + rel;
		}

		return rel;
	}

}
