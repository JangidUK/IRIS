package com.temenos.interaction.core.workflow;

/*
 * #%L
 * interaction-core
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

import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import javax.ws.rs.core.Response.Status;

import org.junit.Test;

import com.temenos.interaction.core.command.InteractionCommand;
import com.temenos.interaction.core.command.InteractionCommand.Result;
import com.temenos.interaction.core.command.InteractionContext;
import com.temenos.interaction.core.command.InteractionException;
/**
 * Test for RetryWorkflowStrategyCommand
 *
 * @author mjangid
 *
 */
public class TestRetryWorkflowStrategyCommand {
	
	@Test
	public void testCommandsSuccessExecution() throws InteractionException {
		InteractionCommand mockCommand = mock(InteractionCommand.class);
		when(mockCommand.execute(any(InteractionContext.class))).thenReturn(Result.SUCCESS);
		
		InteractionContext ctx = mock(InteractionContext.class);
		RetryWorkflowStrategyCommand w = 
				new RetryWorkflowStrategyCommand(mockCommand,5,5);
		w.execute(ctx);
		verify(mockCommand, times(1)).execute(ctx);
	}
	
	@Test
	public void testCommandsRetryExecution() throws InteractionException {
		InteractionCommand mockCommand = mock(InteractionCommand.class);
		when(mockCommand.execute(any(InteractionContext.class))).thenThrow(
				new InteractionException(Status.INTERNAL_SERVER_ERROR, "Test Exception"));
		
		InteractionContext mockContext = mock(InteractionContext.class);
		RetryWorkflowStrategyCommand w = 
				new RetryWorkflowStrategyCommand(mockCommand,3,1);
		boolean exceptionThrown = false;
		try {
			w.execute(mockContext);
		} catch (InteractionException e) {
			exceptionThrown = true;
		}
		assertTrue(exceptionThrown);
		verify(mockCommand, times(4)).execute(any(InteractionContext.class));		
	}
	
	@Test(expected = InteractionException.class)
	public void testCommandsRetryExecutionTime() throws InteractionException {
		InteractionCommand mockCommand = mock(InteractionCommand.class);
		when(mockCommand.execute(any(InteractionContext.class))).thenThrow(
				new InteractionException(Status.INTERNAL_SERVER_ERROR, "Test Exception"));
		
		InteractionContext mockContext = mock(InteractionContext.class);
		
		int shouldWaitInMillis = 3500; // e.g. 500 before 1st attempt, 1000 before 2nd attempt, and 2000 before 3rd attempt
		long startTime = System.currentTimeMillis();
		RetryWorkflowStrategyCommand w = 
				new RetryWorkflowStrategyCommand(mockCommand, 3, 500);
		try {
			w.execute(mockContext);
		} finally {
			long elapsedTime = System.currentTimeMillis() - startTime;
			assertTrue(elapsedTime >= shouldWaitInMillis);
		}
	}
	
	@Test
	public void testCommandsBadRequest() throws InteractionException {
		InteractionCommand mockCommand = mock(InteractionCommand.class);
		when(mockCommand.execute(any(InteractionContext.class))).thenThrow(
				new InteractionException(Status.BAD_REQUEST, "Test Exception"));
		
		InteractionContext mockContext = mock(InteractionContext.class);
		RetryWorkflowStrategyCommand w = 
				new RetryWorkflowStrategyCommand(mockCommand,3,1);
		boolean exceptionThrown = false;
		try {
			w.execute(mockContext);
		} catch (InteractionException e) {
			exceptionThrown = true;
		}
		assertTrue(exceptionThrown);
		verify(mockCommand, times(1)).execute(any(InteractionContext.class));		
	}
}
