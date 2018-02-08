package org.apache.aries.cdi.container.internal.v2.component;

import java.util.concurrent.CopyOnWriteArrayList;

import org.osgi.service.cdi.runtime.dto.ComponentDTO;
import org.osgi.service.cdi.runtime.dto.template.ComponentTemplateDTO;

public class FactoryComponent implements Component {

	public FactoryComponent(ComponentTemplateDTO template) {
		_template = template;
		_snapshot = new ComponentDTO();
		_snapshot.instances = new CopyOnWriteArrayList<>();
		_snapshot.template = new ComponentTemplateDTO();
	}

	@Override
	public ComponentDTO getSnapshot() {
		return _snapshot; // TODO make safe copy using converter
	}

	@Override
	public ComponentTemplateDTO getTemplate() {
		return _template; // TODO make safe copy using converter
	}

	private final ComponentDTO _snapshot;
	private final ComponentTemplateDTO _template;

}
