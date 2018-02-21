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
import org.apache.aries.cdi.container.internal.util.Maps;
import org.apache.aries.cdi.container.test.BaseCDIBundleTest;
import org.apache.aries.cdi.container.test.MockConfiguration;
import org.apache.aries.cdi.container.test.MockServiceRegistration;
import org.junit.Test;
import org.mockito.stubbing.Answer;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.service.cdi.runtime.dto.ContainerDTO;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ConfigurationEvent;
import org.osgi.util.promise.Deferred;
import org.osgi.util.promise.Promise;

public class ConfigurationPhaseTest extends BaseCDIBundleTest {

	@Test
	public void configuration_tracking() throws Exception {
		ConfigurationAdmin ca = mock(ConfigurationAdmin.class);

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

		MockServiceRegistration<ConfigurationAdmin> caReg = cast(
			bundle.getBundleContext().registerService(ConfigurationAdmin.class, ca, null));

		ContainerState containerState = new ContainerState(bundle, ccrBundle, ccrChangeCount, promiseFactory, ca);

		CDIBundle cdiBundle = new CDIBundle(
			ccr, containerState,
				new InitPhase(containerState,
					new ExtensionPhase(containerState,
						new ConfigurationPhase(containerState))));

		cdiBundle.start();

		ContainerDTO containerDTO = ccr.getContainerDTO(bundle);
		assertNotNull(containerDTO);
		assertEquals(1, containerDTO.changeCount);
		assertTrue(containerDTO.errors + "", containerDTO.errors.isEmpty());
		assertNotNull(containerDTO.template);

		final Filter filter = FrameworkUtil.createFilter("(objectClass=" + ConfigurationListener.class.getName() + ")");

		AtomicReference<ConfigurationListener> listener = new AtomicReference<>();

		do {
			serviceRegistrations.stream().filter(
				reg ->
					filter.match(reg.getReference())
			).findFirst().ifPresent(
				reg -> listener.set(cast(reg.getReference().getService()))
			);

			Thread.sleep(10);
		} while(listener.get() == null);

		final String pid = containerState.containerDTO().components.get(0).template.configurations.get(0).pid;

		assertNull(containerState.containerDTO().components.get(0).instances.get(0).properties);

		listener.get().configurationEvent(
			new ConfigurationEvent(caReg.getReference(), ConfigurationEvent.CM_DELETED, null, pid));

		assertNull(containerState.containerDTO().components.get(0).instances.get(0).properties);

		Deferred<Boolean> d1 = testPromiseFactory.deferred();

		CheckedCallback<Boolean, Boolean> c1 = new CheckedCallback<Boolean, Boolean>() {

			@Override
			public Promise<Boolean> call(Promise<Boolean> resolved) throws Exception {
				d1.resolve(resolved.getValue());
				return resolved;
			}

			@Override
			public boolean test(Op op) {
				return op == Op.CONTAINER_COMPONENT_START;
			}

		};

		containerState.addCallback(c1);

		listener.get().configurationEvent(
			new ConfigurationEvent(caReg.getReference(), ConfigurationEvent.CM_UPDATED, null, pid));

		d1.getPromise().then(
			s -> {
				assertNull(containerState.containerDTO().components.get(0).instances.get(0).properties);

				return s;
			}
		).getValue();

		containerState.removeCallback(c1);

		Deferred<Boolean> d2 = testPromiseFactory.deferred();

		CheckedCallback<Boolean, Boolean> c2 = new CheckedCallback<Boolean, Boolean>() {

			@Override
			public Promise<Boolean> call(Promise<Boolean> resolved) throws Exception {
				d2.resolve(resolved.getValue());
				return resolved;
			}

			@Override
			public boolean test(Op op) {
				return op == Op.CONTAINER_COMPONENT_START;
			}

		};

		containerState.addCallback(c2);

		listener.get().configurationEvent(
			new ConfigurationEvent(caReg.getReference(), ConfigurationEvent.CM_UPDATED, null, "foo.config"));

		d2.getPromise().then(
			s -> {
				Map<String, Object> properties = containerState.containerDTO().components.get(0).instances.get(0).properties;
				assertNotNull(properties);
				assertNull(properties.get("fiz"));
				assertEquals("bar", properties.get("foo"));
				return s;
			}
		).getValue();
	}

}
