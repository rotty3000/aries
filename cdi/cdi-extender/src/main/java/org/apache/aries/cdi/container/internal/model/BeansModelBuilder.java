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

package org.apache.aries.cdi.container.internal.model;

import static org.apache.aries.cdi.container.internal.util.Reflection.*;
import static org.osgi.service.cdi.CDIConstants.*;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.aries.cdi.container.internal.container.ContainerState;
import org.apache.aries.cdi.container.internal.util.Logs;
import org.osgi.framework.Bundle;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.service.log.Logger;

public class BeansModelBuilder {

	public BeansModelBuilder(
		ContainerState containerState,
		ClassLoader aggregateClassLoader,
		BundleWiring bundleWiring,
		Map<String, Object> cdiAttributes) {

		_containerState = Optional.ofNullable(containerState);
		_aggregateClassLoader = aggregateClassLoader;
		_bundleWiring = bundleWiring;
		_attributes = cdiAttributes;
		_bundle = _bundleWiring.getBundle();
	}

	public BeansModel build() {
		List<URL> beanDescriptorURLs = new ArrayList<URL>();
		Map<String, Object> attributes = getAttributes();

		List<String> beanDescriptorPaths = cast(attributes.get(REQUIREMENT_BEANS_ATTRIBUTE));

		if (beanDescriptorPaths != null) {
			for (String descriptorPath : beanDescriptorPaths) {
				URL url = getResource(descriptorPath);

				if (url != null) {
					beanDescriptorURLs.add(url);
				}
			}
		}

		@SuppressWarnings("unchecked")
		List<String> beanClassNames = Optional.ofNullable(
			_attributes.get(REQUIREMENT_OSGI_BEANS_ATTRIBUTE)
		).map(v -> (List<String>)v).orElse(Collections.emptyList());

		Map<String, OSGiBean> beans = new HashMap<>();

		for (String beanClassName : beanClassNames) {
			try {
				Class<?> clazz = _aggregateClassLoader.loadClass(beanClassName);

				beans.put(beanClassName, new OSGiBean.Builder(clazz).build());
			}
			catch (Exception e) {
				_log.error(l -> l.error("CCR Error loading class {}", beanClassName, e));

				_containerState.ifPresent(cs -> cs.error(e));
			}
		}

		return new BeansModel(beans, beanDescriptorURLs);
	}

	public Map<String, Object> getAttributes() {
		return _attributes;
	}

	public URL getResource(String resource) {
		return _bundle.getResource(resource);
	}

	private final Logger _log = Logs.getLogger(BeansModelBuilder.class);

	private final ClassLoader _aggregateClassLoader;
	private final Map<String, Object> _attributes;
	private final Bundle _bundle;
	private final BundleWiring _bundleWiring;
	private final Optional<ContainerState> _containerState;

}
