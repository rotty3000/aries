package org.apache.aries.cdi.container.internal.model;

import org.apache.aries.cdi.container.internal.container.ContainerState;
import org.apache.aries.cdi.container.internal.util.Logs;
import org.osgi.service.log.Logger;

public class ContainerActivationDTO extends ExtendedActivationDTO {

	public ContainerState containerState;

	@Override
	public void stop() {
		_log.debug(l -> l.debug("CCR shutdown the CDI container on {}", containerState.bundle()));
	}

	@Override
	public void open() {
		_log.debug(l -> l.debug("CCR start the CDI container on {}", containerState.bundle()));
	}

	private static final Logger _log = Logs.getLogger(ContainerActivationDTO.class);

}
