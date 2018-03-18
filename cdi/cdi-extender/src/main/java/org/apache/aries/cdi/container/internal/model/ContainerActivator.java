package org.apache.aries.cdi.container.internal.model;

import org.apache.aries.cdi.container.internal.container.ContainerBootstrap;
import org.apache.aries.cdi.container.internal.container.ContainerState;
import org.apache.aries.cdi.container.internal.container.Op;
import org.apache.aries.cdi.container.internal.util.Logs;
import org.jboss.weld.bootstrap.WeldBootstrap;
import org.osgi.service.cdi.MaximumCardinality;
import org.osgi.service.cdi.ReferencePolicy;
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

		next.map(ContainerBootstrap.class::cast).ifPresent(
			next -> {
				next.open();

				WeldBootstrap bootstrap = next.getBootstrap();

				bootstrap.validateBeans();
				bootstrap.endInitialization();

				if (referenceDTO.template.policy == ReferencePolicy.DYNAMIC) {
					if (referenceDTO.template.maximumCardinality == MaximumCardinality.MANY) {
					}
				}
			}
		);

		instance.active = true;

		return true;
	}

	private static final Logger _log = Logs.getLogger(ContainerActivator.class);

}
