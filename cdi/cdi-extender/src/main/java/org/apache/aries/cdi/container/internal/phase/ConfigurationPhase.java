/**
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.aries.cdi.container.internal.phase;

import org.apache.aries.cdi.container.internal.container.ConfigurationListener;
import org.apache.aries.cdi.container.internal.container.ContainerState;
import org.apache.aries.cdi.container.internal.model.ContainerComponent;
import org.apache.aries.cdi.container.internal.util.Logs;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cdi.runtime.dto.template.ComponentTemplateDTO;
import org.osgi.service.log.Logger;

public class ConfigurationPhase extends Phase {

	public ConfigurationPhase(ContainerState containerState) {
		super(containerState, null);
	}

	@Override
	public boolean close() {
		_log.debug(l -> l.debug("CCR Begin configuration CLOSE on {}", bundle()));

		_listenerService.unregister();

		_configurationListener.close();

		_configurationListener = null;

		_log.debug(l -> l.debug("CCR Ended configuration CLOSE on {}", bundle()));

		return true;
	}

	@Override
	public boolean open() {
		_log.debug(l -> l.debug("CCR Begin configuration OPEN on {}", bundle()));

		ComponentTemplateDTO containerTemplate = containerState.containerDTO().template.components.get(0);

		ContainerComponent containerComponent = new ContainerComponent(containerState, containerTemplate);

		_configurationListener = new ConfigurationListener(containerState, containerComponent);

		_listenerService = containerState.bundleContext().registerService(
			ConfigurationListener.class, _configurationListener, null);

		_configurationListener.open();

		_log.debug(l -> l.debug("CCR Ended configuration OPEN on {}", bundle()));

		return true;
	}

	private static final Logger _log = Logs.getLogger(ConfigurationPhase.class);

	private ConfigurationListener _configurationListener;
	private ServiceRegistration<ConfigurationListener> _listenerService;

}