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

public class ContainerComponent extends Component {

	public ContainerComponent(
		ContainerState containerState,
		InstanceActivator.Builder<?> builder) {

		super(containerState, null);

		_template = containerState.containerDTO().template.components.get(0);

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
		_instanceDTO.template = _template;
		_instanceDTO.builder = builder;

		_snapshot.instances.add(_instanceDTO);

		containerState.containerDTO().components.add(_snapshot);
	}

	@Override
	public boolean close() {
		return _instanceDTO.close();
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
	public boolean open() {
		return _instanceDTO.open();
	}

	@Override
	public ComponentDTO snapshot() {
		return _snapshot;
	}

	@Override
	public Op openOp() {
		return Op.CONTAINER_COMPONENT_OPEN;
	}

	@Override
	public Op closeOp() {
		return Op.CONTAINER_COMPONENT_CLOSE;
	}

	@Override
	public ComponentTemplateDTO template() {
		return _template;
	}

	private final ExtendedComponentInstanceDTO _instanceDTO;
	private final ComponentDTO _snapshot;
	private final ComponentTemplateDTO _template;

}
