package org.apache.aries.cdi.container.internal.model;

import static org.apache.aries.cdi.container.internal.util.Filters.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import javax.enterprise.inject.spi.BeanManager;

import org.apache.aries.cdi.container.internal.bean.ReferenceBean;
import org.apache.aries.cdi.container.internal.container.ContainerState;
import org.apache.aries.cdi.container.internal.container.Op;
import org.apache.aries.cdi.container.internal.container.Op.Mode;
import org.apache.aries.cdi.container.internal.container.Op.Type;
import org.apache.aries.cdi.container.internal.container.ReferenceSync;
import org.apache.aries.cdi.container.internal.util.Logs;
import org.osgi.framework.Constants;
import org.osgi.service.cdi.ConfigurationPolicy;
import org.osgi.service.cdi.runtime.dto.ComponentInstanceDTO;
import org.osgi.service.cdi.runtime.dto.ConfigurationDTO;
import org.osgi.service.cdi.runtime.dto.ReferenceDTO;
import org.osgi.service.cdi.runtime.dto.template.ComponentTemplateDTO;
import org.osgi.service.cdi.runtime.dto.template.ConfigurationTemplateDTO;
import org.osgi.service.cdi.runtime.dto.template.ReferenceTemplateDTO;
import org.osgi.service.log.Logger;
import org.osgi.util.tracker.ServiceTracker;

public class ExtendedComponentInstanceDTO extends ComponentInstanceDTO {

	public boolean active;
	public BeanManager beanManager;
	public InstanceActivator.Builder<?> builder;
	public Long componentId = _componentIds.incrementAndGet();
	public ContainerState containerState;
	public String pid;
	public ComponentTemplateDTO template;
	public List<ReferenceBean> _referenceBeans = new CopyOnWriteArrayList<>();
	private final AtomicReference<InstanceActivator> _noRequiredDependenciesActivator = new AtomicReference<>();

	public boolean close() {
		containerState.submit(Op.of(Mode.CLOSE, Type.REFERENCES, template.name),
			() -> {
				references.removeIf(
					r -> {
						ExtendedReferenceDTO referenceDTO = (ExtendedReferenceDTO)r;
						referenceDTO.serviceTracker.close();
						return true;
					}
				);

				if (_noRequiredDependenciesActivator.get() != null) {
					containerState.submit(
						_noRequiredDependenciesActivator.get().closeOp(),
						() -> _noRequiredDependenciesActivator.get().close()
					).onFailure(
						f -> {
							_log.error(l -> l.error("CCR Error in CLOSE on {}", ident(), f));

							containerState.error(f);
						}
					);
				}

				return true;
			}
		).onFailure(
			f -> {
				_log.error(l -> l.error("CCR Error in component instance stop on {}", this, f));
			}
		);

		properties = null;

		return true;
	}

	public Op closeOp() {
		return Op.of(Mode.CLOSE, getType(), ident());
	}

	/**
	 * @return true when all the configuration templates are resolved, otherwise false
	 */
	public final boolean configurationsResolved() {
		for (ConfigurationTemplateDTO template : template.configurations) {
			if (template.policy == ConfigurationPolicy.REQUIRED) {
				// find a configuration snapshot or not resolved
				boolean found = false;
				for (ConfigurationDTO snapshot : configurations) {
					if (snapshot.template == template) {
						found = true;
					}
				}
				if (!found) {
					return false;
				}
			}
		}

		return true;
	}

	public final boolean referencesResolved() {
		for (ReferenceTemplateDTO template : template.references) {
			if (template.minimumCardinality > 0) {
				// find a reference snapshot or not resolved
				boolean found = false;
				for (ReferenceDTO snapshot : references) {
					if (!snapshot.template.equals(template)) continue;
					ExtendedReferenceDTO extended = (ExtendedReferenceDTO)snapshot;
					if (extended.matches.size() >= extended.minimumCardinality) {
						found = true;
					}
				}
				if (!found) {
					return false;
				}
			}
		}

		return true;
	}

	public boolean open() {
		if (!configurationsResolved() || (properties != null)) {
			return false;
		}

		properties = componentProperties();

		template.references.stream().map(ExtendedReferenceTemplateDTO.class::cast).forEach(
			t -> {
				ExtendedReferenceDTO referenceDTO = new ExtendedReferenceDTO();

				referenceDTO.matches = new CopyOnWriteArrayList<>();
				referenceDTO.minimumCardinality = minimumCardinality(t.name, t.minimumCardinality);
				referenceDTO.targetFilter = targetFilter(t.serviceType, t.name, t.targetFilter);
				referenceDTO.template = t;
				referenceDTO.serviceTracker = new ServiceTracker<>(
					containerState.bundleContext(),
					asFilter(referenceDTO.targetFilter),
					new ReferenceSync(referenceDTO, this, builder));

				references.add(referenceDTO);
			}
		);

		containerState.submit(
			Op.of(Mode.OPEN, Type.REFERENCES, template.name),
			() -> {
				references.stream().map(ExtendedReferenceDTO.class::cast).forEach(
					r -> r.serviceTracker.open()
				);

				return referencesResolved();
			}
		).then(
			s -> {
				if (!s.getValue()) {
					// none of the reference dependencies are required
					_noRequiredDependenciesActivator.set(builder.setInstance(this).build());

					return containerState.submit(
						_noRequiredDependenciesActivator.get().openOp(),
						() -> _noRequiredDependenciesActivator.get().open()
					).onFailure(
						f -> {
							_log.error(l -> l.error("CCR Error in OPEN on {}", ident(), f));

							containerState.error(f);
						}
					);
				}

				return s;
			}
		);

		return true;
	}

	public Op openOp() {
		return Op.of(Mode.OPEN, getType(), ident());
	}

	private Map<String, Object> componentProperties() {
		Map<String, Object> props = new HashMap<>();
		props.putAll(template.properties);
		List<String> servicePids = new ArrayList<>();

		for (ConfigurationTemplateDTO t : template.configurations) {
			configurations.stream().filter(
				c -> c.template.equals(t) && t.componentConfiguration
			).findFirst().ifPresent(
				c -> {
					Map<String, Object> copy = new HashMap<>(c.properties);

					Optional.ofNullable(
						copy.remove(Constants.SERVICE_PID)
					).map(String.class::cast).ifPresent(
						v -> servicePids.add(v)
					);

					props.putAll(copy);
				}
			);
		}

		props.put(Constants.SERVICE_PID, servicePids);
		props.put("component.id", componentId);
		props.put("component.name", template.name);

		return props;
	}

	private Type getType() {
		switch (template.type) {
			case SINGLE: return Type.SINGLE_INSTANCE;
			case FACTORY: return Type.FACTORY_INSTANCE;
			default: return Type.CONTAINER_INSTANCE;
		}
	}

	private int minimumCardinality(String componentName, int minimumCardinality) {
		return Optional.ofNullable(
			properties.get(componentName.concat(".cardinality.minimum"))
		).map(
			v -> Integer.valueOf(String.valueOf(v))
		).filter(
			v -> v >= minimumCardinality
		).orElse(minimumCardinality);
	}

	private String targetFilter(String serviceType, String componentName, String targetFilter) {
		String base = "(objectClass=".concat(serviceType).concat(")");
		String extraFilter = Optional.ofNullable(
			properties.get(componentName.concat(".target"))
		).map(
			v -> v + targetFilter
		).orElse(targetFilter);

		if (extraFilter.length() == 0) {
			return base;
		}
		return "(&".concat(base).concat(extraFilter).concat(")");
	}

	public String ident() {
		return template.name + "[" + componentId + "]";
	}

	private static Logger _log = Logs.getLogger(ExtendedComponentInstanceDTO.class);
	private static final AtomicLong _componentIds = new AtomicLong();

}
