package com.temenos.interaction.example.mashup.streaming;

/*
 * #%L
 * interaction-example-mashup-streaming
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

import java.util.logging.Logger;

import com.temenos.interaction.core.command.InteractionCommand;
import com.temenos.interaction.core.command.InteractionContext;
import com.temenos.interaction.core.command.InteractionException;
import com.temenos.interaction.core.resource.EntityResource;
import com.temenos.interaction.core.web.RequestContext;
import com.temenos.interaction.example.mashup.streaming.model.Profile;

public class GETProfileCommand implements InteractionCommand {
    private final static Logger logger = Logger.getLogger(Persistence.class.getName());

	private Persistence persistence;
	
	public GETProfileCommand(Persistence p) {
		persistence = p;
	}

	/* Implement InteractionCommand interface */
	
	@Override
	public Result execute(InteractionContext ctx) throws InteractionException {
		assert(ctx != null);
		Profile profile = getProfile(ctx);
		if (profile != null) {
			ctx.setResource(new EntityResource<Profile>(profile));
			return Result.SUCCESS;
		} else {
			return Result.FAILURE;
		}
	}

	public Profile getProfile(InteractionContext ctx) {
		// retrieve from a database for the signed in user.
		RequestContext requestContext = RequestContext.getRequestContext();
		String id = (requestContext.getUserPrincipal() != null ? requestContext.getUserPrincipal().getName() : null);
		if (id == null) {
			id = ctx.getQueryParameters().getFirst("userid");
		}
		// use test default
		if (id == null) {
			id = "someone";
		}
		logger.info("Getting Profile for ["+id+"]");
		Profile profile = persistence.getProfile(id);
		return profile;
	}
}
