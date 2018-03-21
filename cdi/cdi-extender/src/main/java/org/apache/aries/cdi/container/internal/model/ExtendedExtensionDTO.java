package org.apache.aries.cdi.container.internal.model;

import javax.enterprise.inject.spi.Extension;

import org.osgi.framework.ServiceObjects;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cdi.runtime.dto.ExtensionDTO;

public class ExtendedExtensionDTO extends ExtensionDTO {
	public ServiceObjects<Extension> extension;

	public ServiceReference<Extension> serviceReference;
}
