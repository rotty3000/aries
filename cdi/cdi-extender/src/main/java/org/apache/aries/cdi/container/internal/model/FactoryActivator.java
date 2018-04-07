package org.apache.aries.cdi.container.internal.model;

import javax.enterprise.inject.spi.BeanManager;

import org.apache.aries.cdi.container.internal.container.ContainerState;
import org.apache.aries.cdi.container.internal.container.Op;
import org.apache.aries.cdi.container.internal.container.Op.Mode;
import org.apache.aries.cdi.container.internal.container.Op.Type;

public class FactoryActivator extends InstanceActivator {

	public static class Builder extends InstanceActivator.Builder<Builder> {

		public Builder(ContainerState containerState) {
			super(containerState, null);
		}

		public Builder beanManager(BeanManager beanManager) {
			this.beanManager = beanManager;
			return this;
		}

		@Override
		public FactoryActivator build() {
			return new FactoryActivator(this);
		}

		protected BeanManager beanManager;

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
		return Op.of(Mode.CLOSE, Type.FACTORY_ACTIVATOR, instance.template.name);
	}

	@Override
	public boolean open() {
		return true;
	}

	@Override
	public Op openOp() {
		return Op.of(Mode.OPEN, Type.FACTORY_ACTIVATOR, instance.template.name);
	}

}
