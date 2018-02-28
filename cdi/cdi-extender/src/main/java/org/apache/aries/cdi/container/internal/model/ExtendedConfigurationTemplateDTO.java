package org.apache.aries.cdi.container.internal.model;

import java.lang.reflect.Type;

import javax.enterprise.inject.spi.Bean;

import org.osgi.service.cdi.runtime.dto.template.ConfigurationTemplateDTO;

public class ExtendedConfigurationTemplateDTO extends ConfigurationTemplateDTO {

	/**
	 * The bean class which the synthetic bean required to resolve the
	 * injection point must provide.
	 */
	public Class<?> beanClass;

	/**
	 * The class which declared the reference.
	 */
	public Class<?> declaringClass;

	/**
	 * The type of the injection point declaring the configuration.
	 */
	public Type injectionPointType;

	public Bean<?> bean;

}
