package org.apache.aries.cdi.container.internal.model;

import org.osgi.service.cdi.runtime.dto.ConfigurationDTO;
import org.osgi.service.cm.Configuration;

public class ExtendedConfigurationDTO extends ConfigurationDTO {

	/**
	 * The actual PID whether the template is a factory or single
	 * configuration.
	 */
	public String pid;

	public Configuration configuration;

}
