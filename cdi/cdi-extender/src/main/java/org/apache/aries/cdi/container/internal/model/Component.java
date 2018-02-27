package org.apache.aries.cdi.container.internal.model;

import java.util.List;

import org.apache.aries.cdi.container.internal.container.Op;
import org.osgi.service.cdi.runtime.dto.ComponentDTO;
import org.osgi.service.cdi.runtime.dto.ComponentInstanceDTO;
import org.osgi.service.cdi.runtime.dto.template.ComponentTemplateDTO;
import org.osgi.service.cdi.runtime.dto.template.ConfigurationTemplateDTO;

public interface Component {

	List<ConfigurationTemplateDTO> configurationTemplates();

	List<ComponentInstanceDTO> instances();

	ComponentDTO snapshot();

	ComponentTemplateDTO template();

	boolean start();

	Op startOp();

	boolean stop();

	Op stopOp();

}
