package org.apache.aries.cdi.container.internal.model;

import java.util.Arrays;
import java.util.List;

import org.apache.aries.cdi.container.internal.container.ContainerState;
import org.apache.aries.cdi.container.internal.container.Phase;
import org.osgi.service.cdi.runtime.dto.ComponentDTO;
import org.osgi.service.cdi.runtime.dto.ComponentInstanceDTO;
import org.osgi.service.cdi.runtime.dto.template.ComponentTemplateDTO;
import org.osgi.service.cdi.runtime.dto.template.ConfigurationTemplateDTO;

public abstract class Component extends Phase {

	public abstract static class Builder<T extends Builder<T>> {

		public Builder(ContainerState containerState, InstanceActivator.Builder<?> activatorBuilder) {
			_containerState = containerState;
			_activatorBuilder = activatorBuilder;
		}

		@SuppressWarnings("unchecked")
		public T template(ComponentTemplateDTO templateDTO) {
			_templateDTO = templateDTO;
			return (T)this;
		}

		public abstract Component build();

		protected InstanceActivator.Builder<?> _activatorBuilder;
		protected ContainerState _containerState;
		protected Phase _next;
		protected ComponentTemplateDTO _templateDTO;

	}

	Component(Builder<?> builder) {
		super(builder._containerState, null);
		_activatorBuilder = builder._activatorBuilder;
	}

	public InstanceActivator.Builder<?> activatorBuilder() {
		return _activatorBuilder;
	}

	public abstract List<ConfigurationTemplateDTO> configurationTemplates();

	public abstract List<ComponentInstanceDTO> instances();

	public abstract ComponentDTO snapshot();

	public abstract ComponentTemplateDTO template();

	@Override
	public String toString() {
		return Arrays.asList(getClass().getSimpleName(), template().name).toString();
	}

	private final InstanceActivator.Builder<?> _activatorBuilder;

}
