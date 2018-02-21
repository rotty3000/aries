package org.apache.aries.cdi.container.internal.model;

import org.apache.aries.cdi.container.internal.container.ContainerState;

public abstract class ActivationBuilder {

	public ActivationBuilder(ContainerState containerState) {
		_containerState = containerState;
	}

	protected final ContainerState _containerState;

}
