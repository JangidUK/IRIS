package com.temenos.interaction.rimdsl;

import org.eclipse.xtext.conversion.ValueConverterException;
import org.eclipse.xtext.conversion.impl.AbstractValueConverter;
import org.eclipse.xtext.nodemodel.INode;

/**
 * Handler the escaping of any URISTRING types within RIMDsl
 *
 * @author aphethean
 */
public class UriStringConverter extends AbstractValueConverter<String> {

	public String toString(String value) throws ValueConverterException {
		return '"' + value + '"';
	}

	public String toValue(String string, INode node) {
		if (string == null)
			return null;
		return string.substring(1, string.length() - 1);
	}

	
}
