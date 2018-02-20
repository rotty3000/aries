package org.apache.aries.cdi.container.internal.model;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.aries.cdi.container.internal.container.ContainerState;
import org.osgi.service.cdi.runtime.dto.ComponentDTO;
import org.osgi.service.cdi.runtime.dto.template.ComponentTemplateDTO;
import org.osgi.service.cdi.runtime.dto.template.ConfigurationTemplateDTO;

public class ContainerComponent implements Component {

	public ContainerComponent(ContainerState containerState, ComponentTemplateDTO template) {
		_containerState = containerState;
		_template = template;

		_snapshot = new ComponentDTO();
		_snapshot.instances = new CopyOnWriteArrayList<>();
		_snapshot.template = _template;

		_instanceDTO = new ExtendedComponentInstanceDTO();
		_instanceDTO.activations = new CopyOnWriteArrayList<>();
		_instanceDTO.configurations = new CopyOnWriteArrayList<>();
		_instanceDTO.properties = null;
		_instanceDTO.references = new CopyOnWriteArrayList<>();
		_instanceDTO.template = template;

		_snapshot.instances.add(_instanceDTO);

		_containerState.containerDTO().components.add(_snapshot);
	}

	@Override
	public List<ConfigurationTemplateDTO> configurationTemplates() {
		return _template.configurations;
	}

	public ContainerState containerState() {
		return _containerState;
	}

	@Override
	public List<ExtendedComponentInstanceDTO> instances(String pid) {
		return Collections.singletonList(_instanceDTO);
	}

	@Override
	public ComponentDTO snapshot() {
		return _snapshot;
	}

	@Override
	public boolean start() {
		return _instanceDTO.start();
	}

	@Override
	public boolean stop() {
		return _instanceDTO.stop();
	}

	@Override
	public ComponentTemplateDTO template() {
		return _template;
	}

	private final ContainerState _containerState;
	private final ExtendedComponentInstanceDTO _instanceDTO;
	private final ComponentDTO _snapshot;
	private final ComponentTemplateDTO _template;

}
