package org.apache.aries.cdi.container.internal.model;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.aries.cdi.container.internal.container.ContainerState;
import org.apache.aries.cdi.container.internal.container.Op;
import org.apache.aries.cdi.container.internal.container.Op.Mode;
import org.apache.aries.cdi.container.internal.container.Op.Type;
import org.apache.aries.cdi.container.internal.util.Conversions;
import org.osgi.service.cdi.runtime.dto.ComponentDTO;
import org.osgi.service.cdi.runtime.dto.ComponentInstanceDTO;
import org.osgi.service.cdi.runtime.dto.template.ComponentTemplateDTO;
import org.osgi.service.cdi.runtime.dto.template.ConfigurationTemplateDTO;
import org.osgi.service.log.Logger;

public class ContainerComponent extends Component {

	public static class Builder extends Component.Builder<Builder> {

		public Builder(ContainerState containerState, ContainerActivator.Builder activatorBuilder) {
			super(containerState, activatorBuilder);
		}

		@Override
		public ContainerComponent build() {
			template(_containerState.containerComponentTemplateDTO());
			return new ContainerComponent(this);
		}

	}

	protected ContainerComponent(Builder builder) {
		super(builder);

		_log = containerState.containerLogs().getLogger(getClass());

		_template = builder._templateDTO;

		_snapshot = new ComponentDTO();
		_snapshot.instances = new CopyOnWriteArrayList<>();
		_snapshot.template = _template;

		_instanceDTO = new ExtendedComponentInstanceDTO(containerState, builder._activatorBuilder);
		_instanceDTO.activations = new CopyOnWriteArrayList<>();
		_instanceDTO.configurations = new CopyOnWriteArrayList<>();
		_instanceDTO.pid = _template.configurations.get(0).pid;
		_instanceDTO.properties = null;
		_instanceDTO.references = new CopyOnWriteArrayList<>();
		_instanceDTO.template = _template;

		_snapshot.instances.add(_instanceDTO);

		containerState.containerDTO().components.add(_snapshot);
	}

	@Override
	public boolean close() {
		submit(_instanceDTO.closeOp(), _instanceDTO::close).onFailure(
			f -> {
				_log.error(l -> l.error("CCR Error in container component close for {} on {}", _template.name, containerState.bundle()));
			}
		);

		return true;
	}

	@Override
	public Op closeOp() {
		return Op.of(Mode.CLOSE, Type.CONTAINER_COMPONENT, _template.name);
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
		Boolean enabled = _instanceDTO.configurations.stream().filter(
			c -> c.template.pid.equals(_instanceDTO.template.name)
		).findFirst().map(
			c -> Conversions.convert(
				c.properties.get(_template.name.concat(".enabled"))
			).defaultValue(Boolean.TRUE).to(Boolean.class)
		).orElse(Boolean.TRUE);

		if (!enabled) {
			return _snapshot.enabled = false;
		}

		submit(_instanceDTO.openOp(), _instanceDTO::open).onFailure(
			f -> {
				_log.error(l -> l.error("CCR Error in container component open for {} on {}", _template.name, containerState.bundle()));
			}
		);

		return true;
	}

	@Override
	public Op openOp() {
		return Op.of(Mode.OPEN, Type.CONTAINER_COMPONENT, _template.name);
	}

	@Override
	public ComponentDTO snapshot() {
		return _snapshot;
	}

	@Override
	public ComponentTemplateDTO template() {
		return _template;
	}


	private final ExtendedComponentInstanceDTO _instanceDTO;
	private final Logger _log;
	private final ComponentDTO _snapshot;
	private final ComponentTemplateDTO _template;

}
