package org.apache.aries.cdi.container.internal.model;

import org.apache.aries.cdi.container.internal.container.ContainerBootstrap;
import org.apache.aries.cdi.container.internal.container.ContainerState;
import org.apache.aries.cdi.container.internal.container.Op;
import org.apache.aries.cdi.container.internal.container.Op.Mode;
import org.apache.aries.cdi.container.internal.container.Op.Type;
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
	public boolean close() {
		boolean result = next.map(
			next -> {
				submit(next.closeOp(), next::close).onFailure(
					f -> {
						_log.error(l -> l.error("CCR Failure in container activator close on {}", next, f));
					}
				);

				return true;
			}
		).orElse(true);

		instance.active = false;

		return result;
	}

	@Override
	public Op closeOp() {
		return Op.of(Mode.CLOSE, Type.CONTAINER_INSTANCE, instance.template.name);
	}

	@Override
	public boolean open() {
		if (!instance.referencesResolved()) {
			return false;
		}

		boolean result = next.map(
			next -> {
				submit(next.openOp(), next::open).onFailure(
					f -> {
						_log.error(l -> l.error("CCR Failure in container activator open on {}", next, f));
					}
				);

				return true;
			}
		).orElse(true);

		if (result) {
			instance.active = true;
		}

		return result;
	}

	@Override
	public Op openOp() {
		return Op.of(Mode.OPEN, Type.CONTAINER_INSTANCE, instance.template.name);
	}

	private static final Logger _log = Logs.getLogger(ContainerActivator.class);

}
