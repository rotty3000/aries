package org.apache.aries.cdi.container.internal.model;

import org.apache.aries.cdi.container.internal.container.ContainerState;
import org.apache.aries.cdi.container.internal.container.Op;
import org.apache.aries.cdi.container.internal.phase.Phase;

public class FactoryActivator extends InstanceActivator {

	public static class Builder extends InstanceActivator.Builder<Builder> {

		public Builder(ContainerState containerState, Phase next) {
			super(containerState, next);
		}

		@Override
		public FactoryActivator build() {
			return new FactoryActivator(this);
		}

	}

	private FactoryActivator(Builder builder) {
		super(builder);
	}

	@Override
	public boolean close() {
		return false;
	}

	@Override
	public Op closeOp() {
		return Op.FACTORY_INSTANCE_DEACTIVATE;
	}

	@Override
	public boolean open() {
		return true;
	}

	@Override
	public Op openOp() {
		return Op.FACTORY_INSTANCE_ACTIVATE;
	}

}
