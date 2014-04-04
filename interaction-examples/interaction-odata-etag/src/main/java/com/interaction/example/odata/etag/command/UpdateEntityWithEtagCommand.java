package com.interaction.example.odata.etag.command;

/*
 * #%L
 * interaction-example-odata-airline
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


import com.temenos.interaction.core.command.InteractionCommand;
import com.temenos.interaction.core.command.InteractionContext;
import com.temenos.interaction.core.command.InteractionException;

public class UpdateEntityWithEtagCommand implements InteractionCommand {

	private InteractionCommand updateEntityCommand;
	private GETEntityWithEtagCommand getEntityCommand;
	
	public UpdateEntityWithEtagCommand(InteractionCommand updateEntityCommand, GETEntityWithEtagCommand getEntityCommand) {
		this.updateEntityCommand = updateEntityCommand;
		this.getEntityCommand = getEntityCommand;
	}
	
	/* Implement InteractionCommand interface */
	
	@Override
	public Result execute(InteractionContext ctx) throws InteractionException {
		assert(ctx != null);
		
		//Check if the resource has been modified if an etag has been provided
		InteractionContext getCtx = new InteractionContext(ctx, null, null, null, null);
		getCtx.setResource(null);
		String etag = getEntityCommand.getEtag(getCtx);
		String ifMatch = ctx.getPreconditionIfMatch();
		if(ifMatch != null && !ifMatch.equals(etag)) {
			return Result.CONFLICT;
		}
		return updateEntityCommand.execute(ctx);
	}
}
