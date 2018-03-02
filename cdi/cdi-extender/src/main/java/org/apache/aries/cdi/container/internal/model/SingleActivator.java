package org.apache.aries.cdi.container.internal.model;

import org.apache.aries.cdi.container.internal.container.ContainerState;
import org.apache.aries.cdi.container.internal.container.Op;
import org.apache.aries.cdi.container.internal.phase.Phase;

public class SingleActivator extends InstanceActivator {

	public static class Builder extends InstanceActivator.Builder<Builder> {

		public Builder(ContainerState containerState, Phase next) {
			super(containerState, next);
		}

		@Override
		public SingleActivator build() {
			return new SingleActivator(this);
		}

	}

	private SingleActivator(Builder builder) {
		super(builder);
	}

	@Override
	public boolean open() {
		return true;
	}

	@Override
	public Op openOp() {
		return Op.SINGLE_INSTANCE_ACTIVATE;
	}

	@Override
	public boolean close() {
		return true;
	}

	@Override
	public Op closeOp() {
		return Op.SINGLE_INSTANCE_DEACTIVATE;
	}

}
