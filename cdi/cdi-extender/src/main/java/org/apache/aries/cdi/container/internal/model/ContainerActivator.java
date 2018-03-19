package org.apache.aries.cdi.container.internal.model;

import org.apache.aries.cdi.container.internal.container.ContainerBootstrap;
import org.apache.aries.cdi.container.internal.container.ContainerState;
import org.apache.aries.cdi.container.internal.container.Op;
import org.apache.aries.cdi.container.internal.util.Logs;
import org.osgi.service.log.Logger;

public class ContainerActivator extends InstanceActivator {

	public static class Builder extends InstanceActivator.Builder<Builder> {

		public Builder(ContainerState containerState, ContainerBootstrap next) {
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
		return Op.CONTAINER_INSTANCE_OPEN;
	}

	@Override
	public Op closeOp() {
		return Op.CONTAINER_INSTANCE_CLOSE;
	}

	@Override
	public boolean close() {
		boolean result = next.map(
			next -> {
				try {
					return next.close();
				}
				catch (Throwable t) {
					_log.error(l -> l.error("CCR Failure in container activator close on {}", next, t));

					return false;
				}
			}
		).get();

		instance.active = false;

		return result;
	}

	@Override
	public boolean open() {
		if (!instance.referencesResolved()) {
			return false;
		}

		boolean result = next.map(
			next -> {
				try {
					return next.open();
				}
				catch (Throwable t) {
					_log.error(l -> l.error("CCR Failure in container activator open on {}", next, t));

					return false;
				}
			}
		).get();

		if (result) {
			instance.active = true;
		}

		return result;
	}

	private static final Logger _log = Logs.getLogger(ContainerActivator.class);

}
