package org.apache.aries.cdi.container.internal.container;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.aries.cdi.container.internal.model.Component;
import org.apache.aries.cdi.container.internal.model.ExtendedComponentInstanceDTO;
import org.apache.aries.cdi.container.internal.model.ExtendedConfigurationDTO;
import org.apache.aries.cdi.container.internal.model.FactoryActivator;
import org.apache.aries.cdi.container.internal.phase.Phase;
import org.apache.aries.cdi.container.internal.util.Logs;
import org.apache.aries.cdi.container.internal.util.Maps;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cdi.MaximumCardinality;
import org.osgi.service.cdi.runtime.dto.ComponentInstanceDTO;
import org.osgi.service.cdi.runtime.dto.template.ConfigurationTemplateDTO;
import org.osgi.service.cm.ConfigurationEvent;
import org.osgi.service.log.Logger;

public class ConfigurationListener extends Phase implements org.osgi.service.cm.ConfigurationListener {

	public ConfigurationListener(
		ContainerState containerState,
		Component component) {

		super(containerState, component);
	}

	@Override
	public boolean close() {
		if (_listenerService != null) {
			_listenerService.unregister();
		}

		next.map(next -> (Component)next).ifPresent(
			next -> submit(next.closeOp(), next::close).then(
				null,
				f -> {
					_log.error(l -> l.error("CCR Failure in configuration listener close on {}", next, f.getFailure()));

					error(f.getFailure());
				}
			)
		);

		return true;
	}

	@Override
	public void configurationEvent(ConfigurationEvent event) {
		next.map(next -> (Component)next).ifPresent(
			next -> next.configurationTemplates().stream().filter(
				t -> {
					if (((t.maximumCardinality == MaximumCardinality.MANY) && t.pid.equals(event.getFactoryPid())) ||
							((t.maximumCardinality == MaximumCardinality.ONE) && t.pid.equals(event.getPid()))) {
						return true;
					}
					return false;
				}
			).findFirst().ifPresent(
				t -> processEvent(next, t, event)
			)
		);
	}

	@Override
	public boolean open() {
		_listenerService = containerState.bundleContext().registerService(
			ConfigurationListener.class, this, null);

		next.map(next -> (Component)next).ifPresent(
			next -> {
				next.configurationTemplates().stream().forEach(
					template -> {
						if (template.maximumCardinality == MaximumCardinality.ONE) {
							containerState.findConfig(template.pid).ifPresent(
								c -> processEvent(
										next,
										template,
										new ConfigurationEvent(
											containerState.caTracker().getServiceReference(),
											ConfigurationEvent.CM_UPDATED,
											null,
											c.getPid()))
							);
						}
						else {
							containerState.findConfigs(template.pid, true).ifPresent(
								arr -> Arrays.stream(arr).forEach(
									c -> processEvent(
											next,
											template,
											new ConfigurationEvent(
												containerState.caTracker().getServiceReference(),
												ConfigurationEvent.CM_UPDATED,
												c.getFactoryPid(),
												c.getPid())))
							);
						}
					}
				);
			}
		);

		return true;
	}

	private void processEvent(Component component, ConfigurationTemplateDTO t, ConfigurationEvent event) {
		final AtomicBoolean needToRefresh = new AtomicBoolean(false);

		List<ComponentInstanceDTO> instances = component.instances();

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
				if (t.maximumCardinality == MaximumCardinality.MANY) {
					instances.stream().map(
						instance -> (ExtendedComponentInstanceDTO)instance
					).filter(
						instance -> event.getPid().equals(instance.pid)
					).forEach(
						instance -> {
							if (instances.remove(instance)) {
								instance.stop();
							}
						}
					);
				}
				break;
			case ConfigurationEvent.CM_LOCATION_CHANGED:
			case ConfigurationEvent.CM_UPDATED:
				if ((t.maximumCardinality == MaximumCardinality.MANY) &&
					!instances.stream().map(
						instance -> (ExtendedComponentInstanceDTO)instance
					).filter(
						instance -> event.getPid().equals(instance.pid)
					).findFirst().isPresent()) {

					ExtendedComponentInstanceDTO instanceDTO = new ExtendedComponentInstanceDTO();
					instanceDTO.activations = new CopyOnWriteArrayList<>();
					instanceDTO.configurations = new CopyOnWriteArrayList<>();
					instanceDTO.containerState = containerState;
					instanceDTO.pid = event.getPid();
					instanceDTO.properties = null;
					instanceDTO.references = new CopyOnWriteArrayList<>();
					instanceDTO.template = component.template();
					instanceDTO.builder = new FactoryActivator.Builder(containerState, null);

					if (instances.add(instanceDTO)) {
						instanceDTO.start();
					}
				}

				containerState.findConfig(event.getPid()).ifPresent(
					configuration -> {
						ExtendedConfigurationDTO configurationDTO2 = new ExtendedConfigurationDTO();

						configurationDTO2.configuration = configuration;
						configurationDTO2.pid = event.getPid();
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
			startComponent(component);
		}
	}

	private void startComponent(Component component) {
		submit(component.closeOp(), component::close).then(
			s -> {
				return submit(component.openOp(), component::open).then(
					null,
					f -> {
						_log.error(l -> l.error("CCR Failure during configuration start on {}", component, f.getFailure()));

						error(f.getFailure());
					}
				);
			},
			f -> {
				_log.error(l -> l.error("CCR Failure during component refresh {}", component, f.getFailure()));

				error(f.getFailure());
			}
		);
	}

	private static final Logger _log = Logs.getLogger(ConfigurationListener.class);

	private volatile ServiceRegistration<ConfigurationListener> _listenerService;

}
