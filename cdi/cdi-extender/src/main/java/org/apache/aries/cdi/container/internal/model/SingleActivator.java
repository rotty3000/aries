package org.apache.aries.cdi.container.internal.model;

import java.util.concurrent.Callable;

import org.apache.aries.cdi.container.internal.container.ContainerState;
import org.apache.aries.cdi.container.internal.container.Op;

public class SingleActivator extends ComponentInstanceActivator {

	public SingleActivator(ContainerState containerState) {
		super(containerState);
	}

	@Override
	public Callable<?> activate(ExtendedComponentInstanceDTO componentInstanceDTO) {
		return null;
	}

	@Override
	public Op activateOp() {
		return Op.SINGLE_INSTANCE_ACTIVATE;
	}

	@Override
	public Callable<?> deactivate(ExtendedComponentInstanceDTO componentInstanceDTO) {
		return null;
	}

	@Override
	public Op deactivateOp() {
		return Op.SINGLE_INSTANCE_DEACTIVATE;
	}

}
