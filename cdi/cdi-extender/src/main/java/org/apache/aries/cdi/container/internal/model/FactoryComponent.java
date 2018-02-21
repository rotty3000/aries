package org.apache.aries.cdi.container.internal.model;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.aries.cdi.container.internal.container.Op;
import org.osgi.service.cdi.runtime.dto.ComponentDTO;
import org.osgi.service.cdi.runtime.dto.template.ComponentTemplateDTO;
import org.osgi.service.cdi.runtime.dto.template.ConfigurationTemplateDTO;

public class FactoryComponent implements Component {

	public FactoryComponent(ComponentTemplateDTO template) {
		_template = template;
		_snapshot = new ComponentDTO();
		_snapshot.instances = new CopyOnWriteArrayList<>();
		_snapshot.template = new ComponentTemplateDTO();
	}

	@Override
	public List<ConfigurationTemplateDTO> configurationTemplates() {
		return null;
	}

	@Override
	public List<ExtendedComponentInstanceDTO> instances(String pid) {
		return null;
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
		return false;
	}

	@Override
	public Op stopOp() {
		return Op.FACTORY_COMPONENT_STOP;
	}

	@Override
	public boolean stop() {
		return false;
	}

	@Override
	public ComponentTemplateDTO template() {
		return _template;
	}

	private final ComponentDTO _snapshot;
	private final ComponentTemplateDTO _template;

}
