package org.apache.aries.cdi.container.internal.model;

import org.apache.aries.cdi.container.internal.container.ContainerState;
import org.apache.aries.cdi.container.internal.container.Op;

public class FactoryActivator extends InstanceActivator {

	public static class Builder extends InstanceActivator.Builder<Builder> {

		public Builder(ContainerState containerState) {
			super(containerState, null);
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
		return Op.FACTORY_INSTANCE_CLOSE;
	}

	@Override
	public boolean open() {
		return true;
	}

	@Override
	public Op openOp() {
		return Op.FACTORY_INSTANCE_OPEN;
	}

}
