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

package org.apache.aries.cdi.container.test;

import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.apache.aries.cdi.container.internal.container.ContainerState;
import org.apache.aries.cdi.container.internal.model.AbstractModelBuilder;
import org.apache.aries.cdi.container.internal.model.BeansModel;
import org.jboss.weld.resources.spi.ResourceLoader;
import org.jboss.weld.serialization.spi.ProxyServices;
import org.osgi.service.cdi.CDIConstants;

public class TestUtil {

	public static AbstractModelBuilder getModelBuilder(final String osgiBeansFile) {
		return getModelBuilder(
			Arrays.asList(
				"OSGI-INF/cdi/org.apache.aries.cdi.container.test.beans.Bar.xml",
				"OSGI-INF/cdi/org.apache.aries.cdi.container.test.beans.BarAnnotated.xml",
				"OSGI-INF/cdi/org.apache.aries.cdi.container.test.beans.BarBadlyAnnotated.xml",
				"OSGI-INF/cdi/org.apache.aries.cdi.container.test.beans.FooAnnotated.xml",
				"OSGI-INF/cdi/org.apache.aries.cdi.container.test.beans.FooService.xml"
			),  osgiBeansFile);
	}

	public static AbstractModelBuilder getModelBuilder(
		final List<String> defaultResources, final String osgiBeansFile) {

		return new AbstractModelBuilder() {

			@Override
			public List<String> getDefaultResources() {
				return defaultResources;
			}

			@Override
			public URL getResource(String resource) {
				return getClassLoader().getResource(resource);
			}

			@Override
			public ClassLoader getClassLoader() {
				return getClass().getClassLoader();
			}

			@Override
			public Map<String, Object> getAttributes() {
				if (osgiBeansFile == null) {
					return Collections.emptyMap();
				}

				return Collections.singletonMap(
					CDIConstants.REQUIREMENT_OSGI_BEANS_ATTRIBUTE, Arrays.asList(osgiBeansFile));
			}
		};
	}

	public static <T extends Comparable<T>> Collection<T> sort(Collection<T> set) {
		return sort(set, (c1, c2) -> c1.getClass().getName().compareTo(c2.getClass().getName()));
	}

	public static <T> Collection<T> sort(Collection<T> set, Comparator<T> comparator) {
		List<T> list = new ArrayList<>(set);

		Collections.sort(list, comparator);

		return list;
	}

	public static ContainerState getContainerState(BeansModel beansModel) {
		return new ContainerState(null, null) {

			@Override
			public BeansModel beansModel() {
				return beansModel;
			}

			@Override
			public <T extends ResourceLoader & ProxyServices> T loader() {
				return null;
			}

		};
	}

}