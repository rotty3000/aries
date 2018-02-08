package org.apache.aries.cdi.container.internal.v2.component;

import org.osgi.service.cdi.runtime.dto.ComponentDTO;
import org.osgi.service.cdi.runtime.dto.template.ComponentTemplateDTO;

public interface Component {

	ComponentDTO getSnapshot();

	ComponentTemplateDTO getTemplate();


}
