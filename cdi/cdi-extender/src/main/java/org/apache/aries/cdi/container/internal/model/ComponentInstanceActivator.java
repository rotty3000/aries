package org.apache.aries.cdi.container.internal.model;

import java.util.concurrent.Callable;

import org.apache.aries.cdi.container.internal.container.ContainerState;
import org.apache.aries.cdi.container.internal.container.Op;

public abstract class ComponentInstanceActivator {

	public ComponentInstanceActivator(ContainerState containerState) {
		_containerState = containerState;
	}

	public abstract Op activateOp();

	public abstract Op deactivateOp();

	protected final ContainerState _containerState;

	public abstract Callable<?> activate(ExtendedComponentInstanceDTO componentInstanceDTO);

	public abstract Callable<?> deactivate(ExtendedComponentInstanceDTO componentInstanceDTO);

}
