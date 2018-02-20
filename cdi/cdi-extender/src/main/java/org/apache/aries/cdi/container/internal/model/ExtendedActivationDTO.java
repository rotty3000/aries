package org.apache.aries.cdi.container.internal.model;

import org.osgi.service.cdi.runtime.dto.ActivationDTO;

public abstract class ExtendedActivationDTO extends ActivationDTO {

	public abstract void stop();

	public abstract void open();

}
