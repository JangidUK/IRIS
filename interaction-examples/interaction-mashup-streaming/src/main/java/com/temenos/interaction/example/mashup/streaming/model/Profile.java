package com.temenos.interaction.example.mashup.streaming.model;

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


import javax.persistence.Basic;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

@Entity
public class Profile {

	@Id
	@Basic(optional = false)
	private String userID;
	
	private String name;
	private String email;
	
	@Temporal(TemporalType.TIMESTAMP)
	private java.util.Date DOB;
	
	private String location;
	
	@Temporal(TemporalType.TIMESTAMP)
	private java.util.Date pictureTaken;
		
	public Profile() {}

	public String getUserID() {
		return userID;
	}

	public String getName() {
		return name;
	}

	public String getEmail() {
		return email;
	}

	public java.util.Date getDOB() {
		return DOB;
	}

	public String getLocation() {
		return location;
	}

	public java.util.Date getPictureTaken() {
		return pictureTaken;
	}
}