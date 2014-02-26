package com.temenos.interaction.core.workflow;

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


import javax.ws.rs.core.Response.Status.Family;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.temenos.interaction.core.command.InteractionCommand;
import com.temenos.interaction.core.command.InteractionContext;
import com.temenos.interaction.core.command.InteractionException;

/**
 * <p>This command implements a workflow that will retry if there is an error.</p>
 * Commands are added to this workflow and then retried according to the retry count
 * and the incremental backoff (configured in milliseconds).
 * @author aphethean
 */
public class RetryWorkflowStrategyCommand implements InteractionCommand {
	private final static Logger logger = LoggerFactory.getLogger(RetryWorkflowStrategyCommand.class);
	private InteractionCommand command;
	private int maxRetryCount;
	private long maxRetryInterval;
	
	/**
	 * Construct with a list of commands to execute.
	 * @param commands
	 * @param maxRetryCount
	 * @param maxRetryInterval (in milliseconds)
	 * @invariant commands not null
	 */
	public RetryWorkflowStrategyCommand(InteractionCommand command, int maxRetryCount, long maxRetryInterval) {
		this.command = command;
		this.maxRetryCount = maxRetryCount;
		this.maxRetryInterval = maxRetryInterval;
		if (command == null)
			throw new IllegalArgumentException("No commands supplied");		
	}

	/**
	 * @throws InteractionException 
	 * if Family.SERVER_ERROR error then retry maxRetryCount times
	 * and keep incremental interval according to maxRetryInterval 
	 */
	private Result commandExecute(InteractionContext ctx) throws InteractionException {
		Result result = null;
		int retryCount = -1;
		while ( ( maxRetryCount - retryCount++ ) > -1 ) {
			try {
				result = command.execute(ctx);
				break;
			} catch (InteractionException ex) {
				if (Family.SERVER_ERROR.equals(ex.getHttpStatus().getFamily()) && retryCount < maxRetryCount ) {
					long nextRetry = maxRetryInterval * (int)Math.pow(2,retryCount);
						logger.info("iris_request maxRetryCount=" + String.valueOf(maxRetryCount) +
								" maxRetryInterval=" + String.valueOf(maxRetryInterval) +
								" retryingNumber=" + String.valueOf(retryCount) +
								" nextRetryIn=" + String.valueOf(nextRetry) + " seconds");
					try {
						Thread.sleep(nextRetry);
					} catch (InterruptedException e) {
						logger.error("InterruptedException: " + e.getMessage());
					}
				} else { throw ex; }
			}
		}
		return result;
	}
	
	/**
	 * @throws InteractionException 
	 */
	@Override
	public Result execute(InteractionContext ctx) throws InteractionException {
		assert(command != null);
		if (ctx == null)
			throw new IllegalArgumentException("InteractionContext must be supplied");

		return commandExecute(ctx);
	}
}
