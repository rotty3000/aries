package org.apache.aries.cdi.container.internal.v2.component;

import java.util.concurrent.CopyOnWriteArrayList;

import org.osgi.service.cdi.runtime.dto.ComponentDTO;
import org.osgi.service.cdi.runtime.dto.template.ComponentTemplateDTO;
import org.osgi.service.cdi.runtime.dto.template.ConfigurationPolicy;
import org.osgi.service.cdi.runtime.dto.template.ConfigurationTemplateDTO;
import org.osgi.service.cdi.runtime.dto.template.MaximumCardinality;
import org.osgi.service.cdi.runtime.dto.template.ReferenceTemplateDTO;

public class ContainerComponent implements Component {

	public ContainerComponent(String containerId) {
		_dto = new ComponentDTO();
		_dto.template = new ComponentTemplateDTO();
		_dto.template.activations = new CopyOnWriteArrayList<>();
		_dto.template.configurations = new CopyOnWriteArrayList<>();

		ConfigurationTemplateDTO configDTO = new ConfigurationTemplateDTO();
		configDTO.componentConfiguration = true;
		configDTO.maximumCardinality = MaximumCardinality.ONE;
		configDTO.pid = containerId;
		configDTO.policy = ConfigurationPolicy.OPTIONAL;

		_dto.template.configurations.add(configDTO);
		_dto.template.name = containerId;
		_dto.template.references = new CopyOnWriteArrayList<>();
		_dto.template.type = ComponentTemplateDTO.Type.CONTAINER;
	}

	@Override
	public void addConfiguration(ConfigurationTemplateDTO dto) {
		if (dto == null) return;
		_dto.template.configurations.add(dto);
	}

	@Override
	public void addReference(ReferenceTemplateDTO dto) {
		if (dto == null) return;
		_dto.template.references.add(dto);
	}

	@Override
	public ComponentDTO getSnapshot() {
		return _dto; // TODO make safe copy using converter
	}

	@Override
	public ComponentTemplateDTO getTemplate() {
		return _dto.template; // TODO make safe copy using converter
	}

	private final ComponentDTO _dto;

}
