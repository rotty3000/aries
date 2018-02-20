package org.apache.aries.cdi.container.internal.model;

import java.util.List;

import org.osgi.service.cdi.runtime.dto.ComponentDTO;
import org.osgi.service.cdi.runtime.dto.template.ComponentTemplateDTO;
import org.osgi.service.cdi.runtime.dto.template.ConfigurationTemplateDTO;

public interface Component {

	List<ConfigurationTemplateDTO> configurationTemplates();

	List<ExtendedComponentInstanceDTO> instances(String pid);

	ComponentDTO snapshot();

	ComponentTemplateDTO template();

	boolean start();

	boolean stop();

}
