package org.apache.aries.cdi.container.internal.model;

import java.util.List;

import org.apache.aries.cdi.container.internal.container.ContainerState;
import org.apache.aries.cdi.container.internal.container.Op;
import org.apache.aries.cdi.container.internal.phase.Phase;
import org.osgi.service.cdi.runtime.dto.ComponentDTO;
import org.osgi.service.cdi.runtime.dto.ComponentInstanceDTO;
import org.osgi.service.cdi.runtime.dto.template.ComponentTemplateDTO;
import org.osgi.service.cdi.runtime.dto.template.ConfigurationTemplateDTO;

public abstract class Component extends Phase {

	public Component(ContainerState containerState, Phase next) {
		super(containerState, next);
	}

	public abstract Op closeOp();

	public abstract List<ConfigurationTemplateDTO> configurationTemplates();

	public abstract List<ComponentInstanceDTO> instances();

	public abstract Op openOp();

	public abstract ComponentDTO snapshot();

	public abstract ComponentTemplateDTO template();

}
