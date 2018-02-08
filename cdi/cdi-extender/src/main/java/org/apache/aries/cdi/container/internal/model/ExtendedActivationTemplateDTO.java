package org.apache.aries.cdi.container.internal.model;

import org.osgi.service.cdi.runtime.dto.template.ActivationTemplateDTO;

public class ExtendedActivationTemplateDTO extends ActivationTemplateDTO {

	/**
	 * The class which declared the activation (i.e. @Service).
	 */
	public Class<?> declaringClass;

}
