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

package org.apache.aries.cdi.container.internal;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.apache.aries.cdi.container.internal.container.ContainerState;
import org.osgi.framework.Bundle;
import org.osgi.service.cdi.runtime.CDIComponentRuntime;
import org.osgi.service.cdi.runtime.dto.ContainerDTO;
import org.osgi.service.cdi.runtime.dto.template.ContainerTemplateDTO;

public class CCR implements CDIComponentRuntime {

	public void add(Bundle bundle, ContainerState containerState) {
		_states.put(bundle, containerState);
	}

	@Override
	public Collection<ContainerDTO> getContainerDTOs(Bundle... bundles) {
		return _states.values().stream().map(
			cs -> cs.containerDTO()
		).collect(Collectors.toList());
	}

	@Override
	public ContainerDTO getContainerDTO(Bundle bundle) {
		final ContainerState containerState = _states.get(bundle);
		if (containerState == null) {
			return null;
		}
		return containerState.containerDTO();
	}

	@Override
	public long getContainerChangeCount(Bundle bundle) {
		final ContainerState containerState = _states.get(bundle);
		if (containerState == null) {
			return -1;
		}
		return containerState.containerDTO().changeCount;
	}

	@Override
	public ContainerTemplateDTO getContainerTemplateDTO(Bundle bundle) {
		final ContainerState containerState = _states.get(bundle);
		if (containerState == null) {
			return null;
		}
		return containerState.containerDTO().template;
	}

	public void remove(Bundle bundle) {
		_states.remove(bundle);
	}

	private final Map<Bundle, ContainerState> _states = new ConcurrentHashMap<>();

}