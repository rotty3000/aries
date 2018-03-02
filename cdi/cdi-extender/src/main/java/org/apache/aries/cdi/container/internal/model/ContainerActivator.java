package org.apache.aries.cdi.container.internal.model;

import org.apache.aries.cdi.container.internal.container.ContainerBootstrap;
import org.apache.aries.cdi.container.internal.container.ContainerState;
import org.apache.aries.cdi.container.internal.container.Op;
import org.apache.aries.cdi.container.internal.phase.Phase;
import org.apache.aries.cdi.container.internal.util.Logs;
import org.jboss.weld.bootstrap.WeldBootstrap;
import org.osgi.service.log.Logger;

public class ContainerActivator extends InstanceActivator {

	public static class Builder extends InstanceActivator.Builder<Builder> {

		public Builder(ContainerState containerState, Phase next) {
			super(containerState, next);
		}

		@Override
		public ContainerActivator build() {
			return new ContainerActivator(this);
		}

	}

	private ContainerActivator(Builder builder) {
		super(builder);
	}

	@Override
	public Op openOp() {
		return Op.CONTAINER_INSTANCE_ACTIVATE;
	}

	@Override
	public Op closeOp() {
		return Op.CONTAINER_INSTANCE_DEACTIVATE;
	}

	@Override
	public boolean close() {
		if (_cb != null) {
			_cb.shutdown();
			_cb = null;
		}

		instance.active = false;

		return true;
	}

	@Override
	public boolean open() {
		_cb = new ContainerBootstrap(containerState);

		WeldBootstrap bootstrap = _cb.getBootstrap();

		bootstrap.validateBeans();
		bootstrap.endInitialization();

		instance.active = true;

		return true;
	}

	private static final Logger _log = Logs.getLogger(ContainerActivator.class);

	private volatile ContainerBootstrap _cb;

}
