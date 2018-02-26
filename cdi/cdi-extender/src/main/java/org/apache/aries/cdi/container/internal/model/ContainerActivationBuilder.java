package org.apache.aries.cdi.container.internal.model;

import org.apache.aries.cdi.container.internal.container.ContainerState;
import org.apache.aries.cdi.container.internal.container.Op;

public class ContainerActivationBuilder extends ActivationBuilder {

	public ContainerActivationBuilder(ContainerState containerState) {
		super(containerState);
	}

	@Override
	public Op startOp() {
		return Op.CONTAINER_INSTANCE_ACTIVATE;
	}

}
