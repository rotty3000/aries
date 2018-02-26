package org.apache.aries.cdi.container.internal.model;

import java.util.concurrent.Callable;

import org.apache.aries.cdi.container.internal.container.ContainerState;
import org.apache.aries.cdi.container.internal.container.Op;

public abstract class ActivationBuilder {

	public ActivationBuilder(ContainerState containerState) {
		_containerState = containerState;
	}

	public abstract Op startOp();

	protected final ContainerState _containerState;

	public Callable activate(ExtendedComponentInstanceDTO componentInstanceDTO) {
		return null;
	}

}
