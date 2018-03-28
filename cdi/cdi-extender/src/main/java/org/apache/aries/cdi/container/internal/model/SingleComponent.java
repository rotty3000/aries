package org.apache.aries.cdi.container.internal.model;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.aries.cdi.container.internal.container.ContainerState;
import org.apache.aries.cdi.container.internal.container.Op;
import org.osgi.service.cdi.runtime.dto.ComponentDTO;
import org.osgi.service.cdi.runtime.dto.ComponentInstanceDTO;
import org.osgi.service.cdi.runtime.dto.template.ComponentTemplateDTO;
import org.osgi.service.cdi.runtime.dto.template.ConfigurationTemplateDTO;

public class SingleComponent extends Component {

	public static class Builder extends Component.Builder<Builder> {

		public Builder(ContainerState containerState, SingleActivator.Builder activatorBuilder) {
			super(containerState, activatorBuilder);
		}

		@Override
		public SingleComponent build() {
			return new SingleComponent(this);
		}

	}

	protected SingleComponent(Builder builder) {
		super(builder);

		_template = builder._templateDTO;

		_snapshot = new ComponentDTO();
		_snapshot.instances = new CopyOnWriteArrayList<>();
		_snapshot.template = _template;

		_instanceDTO = new ExtendedComponentInstanceDTO();
		_instanceDTO.activations = new CopyOnWriteArrayList<>();
		_instanceDTO.configurations = new CopyOnWriteArrayList<>();
		_instanceDTO.containerState = containerState;
		_instanceDTO.pid = _template.configurations.get(0).pid;
		_instanceDTO.properties = null;
		_instanceDTO.references = new CopyOnWriteArrayList<>();
		_instanceDTO.template = builder._templateDTO;
		_instanceDTO.builder = builder._activatorBuilder;

		_snapshot.instances.add(_instanceDTO);

		containerState.containerDTO().components.add(_snapshot);
	}

	@Override
	public boolean close() {
		return _instanceDTO.close();
	}

	@Override
	public Op closeOp() {
		return Op.SINGLE_COMPONENT_CLOSE;
	}

	@Override
	public List<ConfigurationTemplateDTO> configurationTemplates() {
		return _template.configurations;
	}

	@Override
	public List<ComponentInstanceDTO> instances() {
		return Collections.singletonList(_instanceDTO);
	}

	@Override
	public ComponentDTO snapshot() {
		return _snapshot;
	}

	@Override
	public boolean open() {
		return _instanceDTO.open();
	}

	@Override
	public Op openOp() {
		return Op.SINGLE_COMPONENT_OPEN;
	}

	@Override
	public ComponentTemplateDTO template() {
		return _template;
	}

	private final ExtendedComponentInstanceDTO _instanceDTO;
	private final ComponentDTO _snapshot;
	private final ComponentTemplateDTO _template;

}
