package org.apache.aries.cdi.container.internal.model;

import static org.apache.aries.cdi.container.internal.util.Filters.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.aries.cdi.container.internal.bean.ReferenceBean;
import org.apache.aries.cdi.container.internal.container.ContainerState;
import org.apache.aries.cdi.container.internal.container.Op;
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
	public InstanceActivator.Builder<?> builder;
	public Long componentId = _componentIds.incrementAndGet();
	public ContainerState containerState;
	public String pid;
	public ComponentTemplateDTO template;
	public List<ReferenceBean> _referenceBeans = new CopyOnWriteArrayList<>();

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

	// TODO
	// The types of activations:
	// - container
	// - container service
	// - factory/single component immediate
	// - factory/single component service instance
	//
	// Here there's only a single component (the container component):
	// @References defined by container beans
	// - activation of the CDI container
	// -- publish services defined by the CDI container beans
	//
	// These component evolve concurrently (this only happens once the container component is resolved):
	// --- track single and factory component configurations
	// ----- track single and factory component references
	// ------ activate immediate components OR publish single and factory component services
	//

	public boolean start() {
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
			Op.CONTAINER_REFERENCES_OPEN,
			() -> {
				references.stream().map(ExtendedReferenceDTO.class::cast).forEach(
					r -> r.serviceTracker.open()
				);
				return null;
			}
		);

		InstanceActivator activator = builder.setInstance(this).build();

		containerState.submit(
			activator.openOp(), activator::open
		).then(
			null,
			f -> {
				_log.error(l -> l.error("CCR Error in OPEN on {}", this, f.getFailure()));

				containerState.error(f.getFailure());
			}
		);

		return true;
	}

	public boolean stop() {
		properties = null;

		references.removeIf(
			r -> {
				ExtendedReferenceDTO referenceDTO = (ExtendedReferenceDTO)r;
				referenceDTO.serviceTracker.close();
				return true;
			}
		);

		InstanceActivator activator = builder.setInstance(this).build();

		try {
			return activator.close();
		}
		catch (Throwable t) {
			_log.error(l -> l.error("CCR Error in component instance stop on {}", this, t));

			return false;
		}
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

	private static Logger _log = Logs.getLogger(ExtendedComponentInstanceDTO.class);
	private static final AtomicLong _componentIds = new AtomicLong();

}
