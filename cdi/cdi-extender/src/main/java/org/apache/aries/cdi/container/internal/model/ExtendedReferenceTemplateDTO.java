package org.apache.aries.cdi.container.internal.model;

import java.lang.reflect.Type;

import org.osgi.service.cdi.runtime.dto.template.ReferenceTemplateDTO;

public class ExtendedReferenceTemplateDTO extends ReferenceTemplateDTO {

	/**
	 * The bean class which the synthetic bean required to resolve the
	 * injection point must provide.
	 */
	public Class<?> beanClass;

	/**
	 * The collection type of the reference injection point.
	 */
	public CollectionType collectionType;

	/**
	 * The class which declared the reference.
	 */
	public Class<?> declaringClass;

	/**
	 * The type of the injection point declaring the configuration.
	 */
	public Type injectionPointType;

}
