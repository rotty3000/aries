package org.apache.aries.cdi.container.internal.model;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.aries.cdi.container.internal.container.ContainerState;
import org.apache.aries.cdi.container.internal.container.Op;
import org.osgi.service.cdi.runtime.dto.ComponentDTO;
import org.osgi.service.cdi.runtime.dto.ComponentInstanceDTO;
import org.osgi.service.cdi.runtime.dto.template.ComponentTemplateDTO;
import org.osgi.service.cdi.runtime.dto.template.ConfigurationTemplateDTO;
import org.osgi.service.cdi.runtime.dto.template.MaximumCardinality;

public class FactoryComponent implements Component {

	private ContainerState _containerState;
	public FactoryComponent(ContainerState containerState, ComponentTemplateDTO template) {
		_containerState = containerState;
		_template = template;

		_snapshot = new ComponentDTO();
		_snapshot.instances = new CopyOnWriteArrayList<>();
		_snapshot.template = _template;

		_containerState.containerDTO().components.add(_snapshot);

		configurationTemplates().stream().filter(
			t -> t.maximumCardinality == MaximumCardinality.MANY
		).forEach(
			t -> {
				_containerState.findConfigs(t.pid, true).ifPresent(
					arr -> Arrays.stream(arr).forEach(
						c -> {
							ExtendedComponentInstanceDTO instanceDTO = new ExtendedComponentInstanceDTO();
							instanceDTO.activations = new CopyOnWriteArrayList<>();
							instanceDTO.configurations = new CopyOnWriteArrayList<>();
							instanceDTO.containerState = _containerState;
							instanceDTO.pid = c.getPid();
							instanceDTO.properties = null;
							instanceDTO.references = new CopyOnWriteArrayList<>();
							instanceDTO.template = template;
							instanceDTO.activator = new FactoryActivator(_containerState);

							_snapshot.instances.add(instanceDTO);
						}
					)
				);
			}
		);
	}

	@Override
	public List<ConfigurationTemplateDTO> configurationTemplates() {
		return _template.configurations;
	}

	@Override
	public List<ComponentInstanceDTO> instances() {
		return _snapshot.instances;
	}

	@Override
	public ComponentDTO snapshot() {
		return _snapshot;
	}

	@Override
	public Op startOp() {
		return Op.FACTORY_COMPONENT_START;
	}

	@Override
	public boolean start() {
		_snapshot.instances.stream().map(
			instance -> (ExtendedComponentInstanceDTO)instance
		).forEach(
			instance -> instance.start()
		);

		return true;
	}

	@Override
	public Op stopOp() {
		return Op.FACTORY_COMPONENT_STOP;
	}

	@Override
	public boolean stop() {
		_snapshot.instances.stream().map(
			instance -> (ExtendedComponentInstanceDTO)instance
		).forEach(
			instance -> instance.stop()
		);

		return true;
	}

	@Override
	public ComponentTemplateDTO template() {
		return _template;
	}

	private final ComponentDTO _snapshot;
	private final ComponentTemplateDTO _template;

}
