package com.temenos.interaction.rimdsl;

import org.eclipse.xtext.common.services.DefaultTerminalConverters;
import org.eclipse.xtext.conversion.IValueConverter;
import org.eclipse.xtext.conversion.ValueConverter;
import org.eclipse.xtext.conversion.impl.STRINGValueConverter;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class RIMDslTerminalConverters extends DefaultTerminalConverters {

	@Inject
	private UriStringConverter uriStringConverter;
	@Inject
	private STRINGValueConverter stringConverter;
	
	@ValueConverter(rule = "URISTRING")
	public IValueConverter<String> URISTRING() {
		return uriStringConverter;
	}

	@ValueConverter(rule = "URIPARAM")
	public IValueConverter<String> URIPARAM() {
		return uriStringConverter;
	}

	@ValueConverter(rule = "DQ_STRING")
	public IValueConverter<String> DQ_STRING() {
		return stringConverter;
	}

}
