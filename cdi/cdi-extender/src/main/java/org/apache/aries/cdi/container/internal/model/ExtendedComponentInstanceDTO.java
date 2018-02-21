package org.apache.aries.cdi.container.internal.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import org.osgi.framework.Constants;
import org.osgi.service.cdi.runtime.dto.ComponentInstanceDTO;
import org.osgi.service.cdi.runtime.dto.ConfigurationDTO;
import org.osgi.service.cdi.runtime.dto.template.ComponentTemplateDTO;
import org.osgi.service.cdi.runtime.dto.template.ConfigurationPolicy;
import org.osgi.service.cdi.runtime.dto.template.ConfigurationTemplateDTO;

public class ExtendedComponentInstanceDTO extends ComponentInstanceDTO {

	public ComponentTemplateDTO template;

	public Long componentId = _componentIds.incrementAndGet();

	public ActivationBuilder activationBuilder;

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

	// TODO
	// The types of activations:
	// - container
	// - container service
	// - factory/single component immediate
	// - factory/single component service instance

	public boolean start() {
		if (!configurationsResolved()) {
			return false;
		}

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
					).map(
						v -> (String)v
					).ifPresent(
						v -> servicePids.add(v)
					);

					props.putAll(copy);
				}
			);
		}

		props.put(Constants.SERVICE_PID, servicePids);
		props.put("component.id", componentId);
		props.put("component.name", template.name);

		properties = props;

		// open all the service trackers
		template.references.forEach(
			r -> {
				// when the references are resolved, create activations
//				ReferenceDTO referenceDTO = new ReferenceDTO();
//				referenceDTO.
//
//				references.add(e);
			}
		);

		return true;
	}

	public boolean stop() {
		// close service trackers

		references.stream().forEach(
			r -> {
			}
		);

		properties = null;

		return true;
	}

	private static final AtomicLong _componentIds = new AtomicLong();

}
