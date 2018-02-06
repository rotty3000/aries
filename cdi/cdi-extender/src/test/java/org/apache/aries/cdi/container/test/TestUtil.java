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

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.aries.cdi.container.internal.container.ContainerState;
import org.apache.aries.cdi.container.internal.model.AbstractModelBuilder;
import org.apache.aries.cdi.container.internal.model.BeansModel;
import org.jboss.weld.resources.spi.ResourceLoader;
import org.jboss.weld.serialization.spi.ProxyServices;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.osgi.framework.Bundle;
import org.osgi.framework.dto.BundleDTO;
import org.osgi.framework.namespace.PackageNamespace;
import org.osgi.framework.wiring.BundleCapability;
import org.osgi.framework.wiring.BundleRequirement;
import org.osgi.framework.wiring.BundleWire;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.namespace.extender.ExtenderNamespace;
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

	public static <T extends Comparable<T>> List<T> sort(Collection<T> set) {
		return sort(set, (c1, c2) -> c1.getClass().getName().compareTo(c2.getClass().getName()));
	}

	public static <T> List<T> sort(Collection<T> set, Comparator<T> comparator) {
		List<T> list = new ArrayList<>(set);

		Collections.sort(list, comparator);

		return list;
	}

	public static ContainerState getContainerState(BeansModel beansModel) throws Exception {
		Bundle bundle = mock(Bundle.class);
		BundleWiring bundleWiring = mock(BundleWiring.class);
		Bundle ccrBundle = mock(Bundle.class);
		BundleWiring ccrBundleWiring = mock(BundleWiring.class);
		BundleCapability extenderCapability = mock(BundleCapability.class);
		BundleRequirement extenderRequirement = mock(BundleRequirement.class);
		BundleWire extenderWire = mock(BundleWire.class);

		BundleDTO bundleDTO = new BundleDTO();
		bundleDTO.id = 1;
		bundleDTO.lastModified = 24l;
		bundleDTO.state = Bundle.ACTIVE;
		bundleDTO.symbolicName = "foo";
		bundleDTO.version = "1.0.0";
		when(bundle.getSymbolicName()).thenReturn(bundleDTO.symbolicName);
		when(bundle.adapt(BundleWiring.class)).thenReturn(bundleWiring);
		when(bundle.adapt(BundleDTO.class)).thenReturn(bundleDTO);
		when(bundle.getResource(any())).then(new Answer<URL>() {
			@Override
			public URL answer(InvocationOnMock invocation) throws ClassNotFoundException {
				Object[] args = invocation.getArguments();
				return TestUtil.class.getClassLoader().getResource((String)args[0]);
			}
		});
		when(bundle.loadClass(any())).then(new Answer<Class<?>>() {
			@Override
			public Class<?> answer(InvocationOnMock invocation) throws ClassNotFoundException {
				Object[] args = invocation.getArguments();
				return TestUtil.class.getClassLoader().loadClass((String)args[0]);
			}
		});
		when(bundleWiring.getBundle()).thenReturn(bundle);
		when(bundleWiring.getRequiredWires(ExtenderNamespace.EXTENDER_NAMESPACE)).thenReturn(Collections.singletonList(extenderWire));
		when(bundleWiring.listResources("OSGI-INF/cdi", "*.xml", BundleWiring.LISTRESOURCES_LOCAL)).thenReturn(Collections.singletonList("OSGI-INF/cdi/osgi-beans.xml"));

		when(extenderWire.getCapability()).thenReturn(extenderCapability);
		when(extenderCapability.getAttributes()).thenReturn(Collections.singletonMap(ExtenderNamespace.EXTENDER_NAMESPACE, CDIConstants.CDI_CAPABILITY_NAME));
		when(extenderWire.getRequirement()).thenReturn(extenderRequirement);
		when(extenderRequirement.getAttributes()).thenReturn(new HashMap<>());

		when(ccrBundle.adapt(BundleWiring.class)).thenReturn(ccrBundleWiring);
		when(ccrBundleWiring.getRequiredWires(PackageNamespace.PACKAGE_NAMESPACE)).thenReturn(new ArrayList<>());

		return new ContainerState(bundle, ccrBundle) {

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