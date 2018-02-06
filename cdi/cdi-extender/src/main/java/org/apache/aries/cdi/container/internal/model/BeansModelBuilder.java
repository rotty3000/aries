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

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.osgi.framework.Bundle;
import org.osgi.framework.wiring.BundleWiring;

public class BeansModelBuilder extends AbstractModelBuilder {

	public BeansModelBuilder(ClassLoader classLoader, BundleWiring bundleWiring, Map<String, Object> cdiAttributes) {
		_classLoader = classLoader;
		_bundleWiring = bundleWiring;
		_attributes = cdiAttributes;
		_bundle = _bundleWiring.getBundle();
	}

	@Override
	public Map<String, Object> getAttributes() {
		return _attributes;
	}

	@Override
	public ClassLoader getClassLoader() {
		return _classLoader;
	}

	@Override
	public URL getResource(String resource) {
		return _bundle.getResource(resource);
	}

	@Override
	public List<String> getDefaultResources() {
		return new ArrayList<>(_bundleWiring.listResources("OSGI-INF/cdi", "*.xml", BundleWiring.LISTRESOURCES_LOCAL));
	}

	private final Map<String, Object> _attributes;
	private final Bundle _bundle;
	private final ClassLoader _classLoader;
	private final BundleWiring _bundleWiring;

}
