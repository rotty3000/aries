package org.apache.aries.cdi.container.internal.container;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.aries.cdi.container.internal.model.Component;
import org.apache.aries.cdi.container.internal.model.ExtendedComponentInstanceDTO;
import org.apache.aries.cdi.container.internal.util.Logs;
import org.apache.aries.cdi.container.internal.util.Maps;
import org.apache.aries.cdi.container.internal.util.Throw;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.cdi.runtime.dto.ConfigurationDTO;
import org.osgi.service.cdi.runtime.dto.template.ConfigurationTemplateDTO;
import org.osgi.service.cdi.runtime.dto.template.MaximumCardinality;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationEvent;
import org.osgi.service.log.Logger;

public class ConfigurationListener implements org.osgi.service.cm.ConfigurationListener {

	public ConfigurationListener(ContainerState containerState, Component component) {
		_component = component;
		_containerState = containerState;
	}

	public void close() {
		_containerState.promiseFactory().submit(_component::stop);
	}

	@Override
	public void configurationEvent(ConfigurationEvent event) {
		_component.configurationTemplates().stream().filter(
			t -> {
				if (((t.maximumCardinality == MaximumCardinality.MANY) && t.pid.equals(event.getFactoryPid())) ||
					((t.maximumCardinality == MaximumCardinality.ONE) && t.pid.equals(event.getPid()))) {
					return true;
				}
				return false;
			}
		).findFirst().ifPresent(
			t -> processEvent(t, event)
		);
	}

	private void processEvent(ConfigurationTemplateDTO t, ConfigurationEvent event) {
		final AtomicBoolean needToRefresh = new AtomicBoolean(false);

		List<ExtendedComponentInstanceDTO> instances = _component.instances(event.getPid());

		instances.stream().forEach(
			instance -> instance.configurations.stream().filter(
				c -> c.template.equals(t)
			).forEach(
				c -> {
					instance.configurations.remove(c);
					needToRefresh.set(true);
				}
			)
		);

		switch (event.getType()) {
			case ConfigurationEvent.CM_DELETED:
				break;
			case ConfigurationEvent.CM_LOCATION_CHANGED:
			case ConfigurationEvent.CM_UPDATED:
				findConfig(event.getPid()).ifPresent(
					configuration -> {
						ConfigurationDTO configurationDTO2 = new ConfigurationDTO();

						configurationDTO2.properties = Maps.of(configuration.getProcessedProperties(event.getReference()));
						configurationDTO2.template = t;

						instances.stream().forEach(
							instance -> {
								instance.configurations.add(configurationDTO2);
								needToRefresh.set(true);
							}
						);
					}
				);
				break;
		}

		if (needToRefresh.get()) {
			_containerState.promiseFactory().submit(_component::stop).then(
				s -> {
					return _containerState.promiseFactory().submit(_component::start);
				}
			);
		}
	}

	Optional<Configuration> findConfig(String pid) {
		try {
			return Optional.ofNullable(
				_containerState.configurationAdmin().listConfigurations(
					"(service.pid=".concat(pid).concat(")"))
			).map(
				arr -> arr[0]
			);
		}
		catch (InvalidSyntaxException | IOException e) {
			_log.error(l -> l.error("CCR unexpected failure fetching configuration for {}", pid, e));

			return Throw.exception(e);
		}
	}

	private static final Logger _log = Logs.getLogger(ConfigurationListener.class);

	private final Component _component;
	private final ContainerState _containerState;

}
