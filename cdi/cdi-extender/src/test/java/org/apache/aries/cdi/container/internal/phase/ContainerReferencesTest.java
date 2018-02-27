package org.apache.aries.cdi.container.internal.phase;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import org.apache.aries.cdi.container.internal.container.CDIBundle;
import org.apache.aries.cdi.container.internal.container.CheckedCallback;
import org.apache.aries.cdi.container.internal.container.ContainerState;
import org.apache.aries.cdi.container.internal.container.Op;
import org.apache.aries.cdi.container.internal.util.Maps;
import org.apache.aries.cdi.container.test.BaseCDIBundleTest;
import org.apache.aries.cdi.container.test.MockConfiguration;
import org.junit.Test;
import org.mockito.stubbing.Answer;
import org.osgi.service.cdi.runtime.dto.ComponentDTO;
import org.osgi.service.cdi.runtime.dto.ComponentInstanceDTO;
import org.osgi.service.cdi.runtime.dto.ContainerDTO;
import org.osgi.service.cdi.runtime.dto.template.ComponentTemplateDTO.Type;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.util.promise.Deferred;
import org.osgi.util.promise.Promise;
import org.osgi.util.tracker.ServiceTracker;

public class ContainerReferencesTest extends BaseCDIBundleTest {

	@Test
	public void configuration_tracking() throws Exception {
		ServiceTracker<ConfigurationAdmin, ConfigurationAdmin> caTracker = new ServiceTracker<>(bundle.getBundleContext(), ConfigurationAdmin.class, null);
		caTracker.open();
		ConfigurationAdmin ca = mock(ConfigurationAdmin.class);
		bundle.getBundleContext().registerService(ConfigurationAdmin.class, ca, null);

		when(ca.listConfigurations(anyString())).then(
			(Answer<Configuration[]>) listConfigurations -> {
				String query = listConfigurations.getArgument(0);
				if (query.contains("service.pid=foo.config")) {
					MockConfiguration mockConfiguration = new MockConfiguration("foo.config", null);
					mockConfiguration.update(Maps.dict("fiz", "buz"));
					return new Configuration[] {mockConfiguration};
				}
				return null;
			}
		);

		ContainerState containerState = new ContainerState(bundle, ccrBundle, ccrChangeCount, promiseFactory, caTracker);

		CDIBundle cdiBundle = new CDIBundle(
			ccr, containerState,
				new InitPhase(containerState,
					new ExtensionPhase(containerState,
						new ConfigurationPhase(containerState))));

		cdiBundle.start();

		ContainerDTO containerDTO = containerState.containerDTO();
		assertNotNull(containerDTO);
		assertEquals(1, containerDTO.changeCount);
		assertTrue(containerDTO.errors + "", containerDTO.errors.isEmpty());
		assertNotNull(containerDTO.template);

		Deferred<Object> waitForContainerComponent = testPromiseFactory.deferred();

		containerState.addCallback(new CheckedCallback<Boolean, Boolean>() {

			@Override
			public boolean test(Op op) {
				return op == Op.CONTAINER_COMPONENT_START;
			}

			@Override
			public Promise<Boolean> call(Promise<Boolean> resolved) throws Exception {
				waitForContainerComponent.resolveWith(resolved);
				return resolved;
			}

		});

		waitForContainerComponent.getPromise().getValue();

		ComponentDTO componentDTO = containerDTO.components.stream().filter(
			c -> c.template.type == Type.CONTAINER
		).findFirst().get();

		assertNotNull(componentDTO);

		ComponentInstanceDTO componentInstanceDTO = componentDTO.instances.get(0);

		assertNotNull(componentInstanceDTO);

		Deferred<Boolean> referencesD = testPromiseFactory.deferred();

		containerState.addCallback(new CheckedCallback<Boolean, Boolean>() {

			@Override
			public Promise<Boolean> call(Promise<Boolean> resolved) throws Exception {
				referencesD.resolve(resolved.getValue());
				return resolved;
			}

			@Override
			public boolean test(Op op) {
				return op == Op.CONTAINER_INSTANCE_ACTIVATE;
			}

		});

		assertEquals(6, componentInstanceDTO.references.size());
	}

}
