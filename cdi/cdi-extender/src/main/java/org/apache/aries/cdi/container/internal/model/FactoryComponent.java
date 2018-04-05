package org.apache.aries.cdi.container.internal.model;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.enterprise.inject.spi.BeanManager;

import org.apache.aries.cdi.container.internal.container.ContainerState;
import org.apache.aries.cdi.container.internal.container.Op;
import org.apache.aries.cdi.container.internal.container.Op.Mode;
import org.apache.aries.cdi.container.internal.container.Op.Type;
import org.apache.aries.cdi.container.internal.util.Logs;
import org.osgi.service.cdi.MaximumCardinality;
import org.osgi.service.cdi.runtime.dto.ComponentDTO;
import org.osgi.service.cdi.runtime.dto.ComponentInstanceDTO;
import org.osgi.service.cdi.runtime.dto.template.ComponentTemplateDTO;
import org.osgi.service.cdi.runtime.dto.template.ConfigurationTemplateDTO;
import org.osgi.service.log.Logger;

public class FactoryComponent extends Component {

	public static class Builder extends Component.Builder<Builder> {

		public Builder(ContainerState containerState, FactoryActivator.Builder activatorBuilder) {
			super(containerState, activatorBuilder);
		}

		public Builder beanManager(BeanManager bm) {
			_bm = bm;
			return this;
		}

		@Override
		public FactoryComponent build() {
			return new FactoryComponent(this);
		}

		private BeanManager _bm;

	}

	protected FactoryComponent(Builder builder) {
		super(builder);

		_template = builder._templateDTO;

		_snapshot = new ComponentDTO();
		_snapshot.instances = new CopyOnWriteArrayList<>();
		_snapshot.template = _template;

		containerState.containerDTO().components.add(_snapshot);

		configurationTemplates().stream().filter(
			t -> t.maximumCardinality == MaximumCardinality.MANY
		).forEach(
			t -> {
				containerState.findConfigs(t.pid, true).ifPresent(
					arr -> Arrays.stream(arr).forEach(
						c -> {
							ExtendedComponentInstanceDTO instanceDTO = new ExtendedComponentInstanceDTO();
							instanceDTO.activations = new CopyOnWriteArrayList<>();
							instanceDTO.beanManager = builder._bm;
							instanceDTO.configurations = new CopyOnWriteArrayList<>();
							instanceDTO.containerState = containerState;
							instanceDTO.pid = c.getPid();
							instanceDTO.properties = null;
							instanceDTO.references = new CopyOnWriteArrayList<>();
							instanceDTO.template = builder._templateDTO;
							instanceDTO.builder = builder._activatorBuilder;

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
			instance -> {
				submit(instance.closeOp(), instance::close).onFailure(
					f -> {
						_log.error(l -> l.error("CCR Error in factory component close for {} on {}", instance.ident(), containerState.bundle()));
					}
				);
			}
		);

		return true;
	}

	@Override
	public Op closeOp() {
		return Op.of(Mode.CLOSE, Type.FACTORY_COMPONENT, _template.name);
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
			instance -> {
				submit(instance.openOp(), instance::open).onFailure(
					f -> {
						_log.error(l -> l.error("CCR Error in factory component open for {} on {}", instance.ident(), containerState.bundle()));
					}
				);
			}
		);

		return true;
	}

	@Override
	public Op openOp() {
		return Op.of(Mode.OPEN, Type.FACTORY_COMPONENT, _template.name);
	}

	@Override
	public ComponentTemplateDTO template() {
		return _template;
	}

	private static final Logger _log = Logs.getLogger(FactoryComponent.class);

	private final ComponentDTO _snapshot;
	private final ComponentTemplateDTO _template;

}
