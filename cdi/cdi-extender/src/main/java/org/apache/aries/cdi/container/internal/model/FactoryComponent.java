package org.apache.aries.cdi.container.internal.model;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.aries.cdi.container.internal.container.ContainerState;
import org.apache.aries.cdi.container.internal.container.Op;
import org.osgi.service.cdi.MaximumCardinality;
import org.osgi.service.cdi.runtime.dto.ComponentDTO;
import org.osgi.service.cdi.runtime.dto.ComponentInstanceDTO;
import org.osgi.service.cdi.runtime.dto.template.ComponentTemplateDTO;
import org.osgi.service.cdi.runtime.dto.template.ConfigurationTemplateDTO;

public class FactoryComponent extends Component {

	private ContainerState _containerState;
	public FactoryComponent(
		ContainerState containerState,
		ComponentTemplateDTO template,
		InstanceActivator.Builder<?> builder) {

		super(containerState, null);

		_containerState = containerState;
		_template = template;
		_builder = builder;

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
							instanceDTO.builder = _builder;

							_snapshot.instances.add(instanceDTO);
						}
					)
				);
			}
		);
	}

	@Override
	public boolean close() {
		_snapshot.instances.stream().map(
			instance -> (ExtendedComponentInstanceDTO)instance
		).forEach(
			instance -> instance.stop()
		);

		return true;
	}

	@Override
	public Op closeOp() {
		return Op.FACTORY_COMPONENT_STOP;
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
	public boolean open() {
		_snapshot.instances.stream().map(
			instance -> (ExtendedComponentInstanceDTO)instance
		).forEach(
			instance -> instance.start()
		);

		return true;
	}

	@Override
	public Op openOp() {
		return Op.FACTORY_COMPONENT_START;
	}

	@Override
	public ComponentTemplateDTO template() {
		return _template;
	}

	private final InstanceActivator.Builder<?> _builder;
	private final ComponentDTO _snapshot;
	private final ComponentTemplateDTO _template;

}
