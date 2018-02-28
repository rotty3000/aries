package org.apache.aries.cdi.container.internal.model;

import javax.enterprise.inject.spi.Bean;

import org.osgi.service.cdi.runtime.dto.template.ComponentTemplateDTO;

public class ExtendedComponentTemplateDTO extends ComponentTemplateDTO {

	public Bean<?> bean;
}
