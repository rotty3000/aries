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

import static org.apache.aries.cdi.container.internal.util.Reflection.*;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.aries.cdi.container.internal.container.CDIBundle;
import org.apache.aries.cdi.container.internal.container.CheckedCallback;
import org.apache.aries.cdi.container.internal.container.ConfigurationListener;
import org.apache.aries.cdi.container.internal.container.ContainerState;
import org.apache.aries.cdi.container.internal.container.Op;
import org.apache.aries.cdi.container.internal.model.ContainerComponent;
import org.apache.aries.cdi.container.internal.util.Maps;
import org.apache.aries.cdi.container.test.BaseCDIBundleTest;
import org.apache.aries.cdi.container.test.MockConfiguration;
import org.apache.aries.cdi.container.test.MockServiceRegistration;
import org.apache.aries.cdi.container.test.TestUtil;
import org.junit.Test;
import org.mockito.stubbing.Answer;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.service.cdi.runtime.dto.ContainerDTO;
import org.osgi.service.cdi.runtime.dto.template.ComponentTemplateDTO;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ConfigurationEvent;
import org.osgi.util.promise.Promise;
import org.osgi.util.tracker.ServiceTracker;

public class ConfigurationPhaseTest extends BaseCDIBundleTest {

	@Test
	public void configuration_tracking() throws Exception {
		ServiceTracker<ConfigurationAdmin, ConfigurationAdmin> caTracker = new ServiceTracker<>(bundle.getBundleContext(), ConfigurationAdmin.class, null);
		caTracker.open();
		ConfigurationAdmin ca = mock(ConfigurationAdmin.class);
		MockServiceRegistration<ConfigurationAdmin> caReg = cast(
			bundle.getBundleContext().registerService(ConfigurationAdmin.class, ca, null));

		when(ca.listConfigurations(anyString())).then(
			(Answer<Configuration[]>) listConfigurations -> {
				String query = listConfigurations.getArgument(0);
				if (query.contains("service.pid=foo.config")) {
					MockConfiguration mockConfiguration = new MockConfiguration("foo.config", null);
					mockConfiguration.update(Maps.dict("fiz", "buz"));
					return new Configuration[] {mockConfiguration};
				}
				else if (query.contains("service.pid=osgi.cdi.foo")) {
					MockConfiguration mockConfiguration = new MockConfiguration("osgi.cdi.foo", null);
					mockConfiguration.update(Maps.dict("foo", "bar"));
					return new Configuration[] {mockConfiguration};
				}
				return null;
			}
		);

		ContainerState containerState = new ContainerState(bundle, ccrBundle, ccrChangeCount, promiseFactory, caTracker);

		ComponentTemplateDTO containerTemplate = containerState.containerDTO().template.components.get(0);

		ContainerComponent containerComponent = new ContainerComponent(containerState, containerTemplate);

		CDIBundle cdiBundle = new CDIBundle(
			ccr, containerState,
				new ExtensionPhase(containerState,
					new ConfigurationListener(containerState, containerComponent)));

		Promise<Boolean> p0 = containerState.addCallback(
			(CheckedCallback<Boolean, Boolean>) op -> {
				return op == Op.CONTAINER_COMPONENT_START;
			}
		);

		cdiBundle.start();

		ContainerDTO containerDTO = ccr.getContainerDTO(bundle);
		assertNotNull(containerDTO);
		assertEquals(1, containerDTO.changeCount);
		assertTrue(containerDTO.errors + "", containerDTO.errors.isEmpty());
		assertNotNull(containerDTO.template);

		final Filter filter = FrameworkUtil.createFilter("(objectClass=" + ConfigurationListener.class.getName() + ")");

		AtomicReference<ConfigurationListener> listener = new AtomicReference<>();

		do {
			TestUtil.serviceRegistrations.stream().filter(
				reg ->
					filter.match(reg.getReference())
			).findFirst().ifPresent(
				reg -> listener.set(cast(reg.getReference().getService()))
			);

			Thread.sleep(10);
		} while(listener.get() == null);

		p0.getValue();

		final String pid = containerState.containerDTO().components.get(0).template.configurations.get(0).pid;

		assertNotNull(containerState.containerDTO().components.get(0).instances.get(0).properties);
		assertEquals("bar", containerState.containerDTO().components.get(0).instances.get(0).properties.get("foo"));

		Promise<Boolean> p1 = containerState.addCallback(
			(CheckedCallback<Boolean, Boolean>) op -> {
				return op == Op.CONTAINER_COMPONENT_START;
			}
		);

		listener.get().configurationEvent(
			new ConfigurationEvent(caReg.getReference(), ConfigurationEvent.CM_DELETED, null, pid));

		p1.getValue();

		assertNotNull(containerState.containerDTO().components.get(0).instances.get(0).properties);
		assertNull(containerState.containerDTO().components.get(0).instances.get(0).properties.get("foo"));

		Promise<Boolean> p2 = containerState.addCallback(
			(CheckedCallback<Boolean, Boolean>) op -> {
				return op == Op.CONTAINER_COMPONENT_START;
			}
		);

		listener.get().configurationEvent(
			new ConfigurationEvent(caReg.getReference(), ConfigurationEvent.CM_UPDATED, null, pid));

		p2.getValue();

		assertNotNull(containerState.containerDTO().components.get(0).instances.get(0).properties);
		assertEquals("bar", containerState.containerDTO().components.get(0).instances.get(0).properties.get("foo"));

		Promise<Boolean> p3 = containerState.addCallback(
			(CheckedCallback<Boolean, Boolean>) op -> {
				return op == Op.CONTAINER_COMPONENT_START;
			}
		);

		listener.get().configurationEvent(
			new ConfigurationEvent(caReg.getReference(), ConfigurationEvent.CM_UPDATED, null, "foo.config"));

		p3.getValue();

		Map<String, Object> properties = containerState.containerDTO().components.get(0).instances.get(0).properties;
		assertNotNull(properties);
		assertNull(properties.get("fiz"));
		assertEquals("bar", properties.get("foo"));
	}

}
