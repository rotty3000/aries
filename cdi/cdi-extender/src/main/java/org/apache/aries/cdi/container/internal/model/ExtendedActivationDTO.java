package org.apache.aries.cdi.container.internal.model;

import java.util.function.Consumer;

import org.osgi.service.cdi.runtime.dto.ActivationDTO;

public class ExtendedActivationDTO extends ActivationDTO {

	public Consumer<ExtendedActivationDTO> onClose;

}
