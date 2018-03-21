package org.apache.aries.cdi.container.internal.util;

import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;

public class Filters {

	private Filters() {
		// no instances
	}

	public static final Filter asFilter(String pattern, Object... objects) {
		try {
			return FrameworkUtil.createFilter(String.format(pattern, objects));
		}
		catch (InvalidSyntaxException ise) {
			return Throw.exception(ise);
		}
	}

	public static final boolean isValid(String filterString) {
		try {
			FrameworkUtil.createFilter(filterString);

			return true;
		}
		catch (InvalidSyntaxException ise) {
			return false;
		}
	}

}
