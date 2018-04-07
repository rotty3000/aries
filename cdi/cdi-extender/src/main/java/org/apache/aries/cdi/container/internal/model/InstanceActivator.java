package org.apache.aries.cdi.container.internal.model;

import org.apache.aries.cdi.container.internal.container.ContainerState;
import org.apache.aries.cdi.container.internal.container.Op;
import org.apache.aries.cdi.container.internal.container.Phase;
import org.osgi.framework.ServiceReference;

public abstract class InstanceActivator extends Phase {

	public abstract static class Builder<T extends Builder<T>> {

		public Builder(ContainerState containerState, Phase next) {
			_containerState = containerState;
			_next = next;
		}

		public abstract InstanceActivator build();

		public T setInstance(ExtendedComponentInstanceDTO instance) {
			_instance = instance;
			return (T)this;
		}

		public T setReference(ServiceReference<Object> reference) {
			_reference = reference;
			return (T)this;
		}

		public T setReferenceDTO(ExtendedReferenceDTO referenceDTO) {
			_referenceDTO = referenceDTO;
			return (T)this;
		}

		private ContainerState _containerState;
		private ExtendedComponentInstanceDTO _instance;
		private Phase _next;
		private ServiceReference<Object> _reference;
		private ExtendedReferenceDTO _referenceDTO;
	}

	protected InstanceActivator(Builder<?> builder) {
		super(builder._containerState, builder._next);

		this.instance = builder._instance;
		this.referenceDTO = builder._referenceDTO;
		this.reference = builder._reference;
	}

	@Override
	public abstract Op closeOp();

	@Override
	public abstract Op openOp();

	protected final ExtendedComponentInstanceDTO instance;
	protected final ExtendedReferenceDTO referenceDTO;
	protected final ServiceReference<Object> reference;

}
