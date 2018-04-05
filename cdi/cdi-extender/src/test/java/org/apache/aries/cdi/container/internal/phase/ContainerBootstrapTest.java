package org.apache.aries.cdi.container.internal.phase;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.aries.cdi.container.internal.container.CheckedCallback;
import org.apache.aries.cdi.container.internal.container.ConfigurationListener;
import org.apache.aries.cdi.container.internal.container.ContainerBootstrap;
import org.apache.aries.cdi.container.internal.container.ContainerState;
import org.apache.aries.cdi.container.internal.container.Op;
import org.apache.aries.cdi.container.internal.model.ContainerActivator;
import org.apache.aries.cdi.container.internal.model.ExtendedComponentInstanceDTO;
import org.apache.aries.cdi.container.internal.model.FactoryComponent;
import org.apache.aries.cdi.container.internal.model.SingleComponent;
import org.apache.aries.cdi.container.test.BaseCDIBundleTest;
import org.apache.aries.cdi.container.test.TestUtil;
import org.apache.aries.cdi.container.test.beans.FooService;
import org.junit.Test;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.namespace.extender.ExtenderNamespace;
import org.osgi.service.cdi.CDIConstants;
import org.osgi.service.cdi.runtime.dto.ComponentDTO;
import org.osgi.service.cdi.runtime.dto.ContainerDTO;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.log.LoggerFactory;
import org.osgi.util.promise.Promise;
import org.osgi.util.tracker.ServiceTracker;

public class ContainerBootstrapTest extends BaseCDIBundleTest {

	@Test
	public void test_publishServices() throws Exception {
		Map<String, Object> attributes = new HashMap<>();

		attributes.put(
			CDIConstants.REQUIREMENT_OSGI_BEANS_ATTRIBUTE,
			Arrays.asList(
				FooService.class.getName()
			)
		);

		when(
			bundle.adapt(
				BundleWiring.class).getRequiredWires(
					ExtenderNamespace.EXTENDER_NAMESPACE).get(
						0).getRequirement().getAttributes()
		).thenReturn(attributes);

		ServiceTracker<ConfigurationAdmin, ConfigurationAdmin> caTracker = TestUtil.mockCaSt(bundle);
		ServiceTracker<LoggerFactory, LoggerFactory> loggerTracker = TestUtil.mockLoggerFactory(bundle);

		ContainerState containerState = new ContainerState(bundle, ccrBundle, ccrChangeCount, promiseFactory, caTracker, loggerTracker);

		ComponentDTO componentDTO = new ComponentDTO();
		componentDTO.instances = new CopyOnWriteArrayList<>();
		componentDTO.template = containerState.containerDTO().template.components.get(0);

		ExtendedComponentInstanceDTO componentInstanceDTO = new ExtendedComponentInstanceDTO();
		componentInstanceDTO.activations = new CopyOnWriteArrayList<>();
		componentInstanceDTO.configurations = new CopyOnWriteArrayList<>();
		componentInstanceDTO.containerState = containerState;
		componentInstanceDTO.pid = componentDTO.template.configurations.get(0).pid;
		componentInstanceDTO.properties = null;
		componentInstanceDTO.references = new CopyOnWriteArrayList<>();
		componentInstanceDTO.template = componentDTO.template;

		ContainerBootstrap containerBootstrap = new ContainerBootstrap(
			containerState,
			new ConfigurationListener.Builder(containerState),
			new SingleComponent.Builder(containerState, null),
			new FactoryComponent.Builder(containerState, null));

		componentInstanceDTO.builder = new ContainerActivator.Builder(containerState, containerBootstrap);

		componentDTO.instances.add(componentInstanceDTO);

		containerState.containerDTO().components.add(componentDTO);

		Promise<Boolean> p0 = containerState.addCallback(
			(CheckedCallback<Boolean, Boolean>) op -> {
				return op.mode == Op.Mode.OPEN && op.type == Op.Type.CONTAINER_PUBLISH_SERVICES;
			}
		);

		containerBootstrap.open();

		ContainerDTO containerDTO = containerState.containerDTO();
		assertNotNull(containerDTO);
		assertEquals(1, containerDTO.changeCount);
		assertTrue(containerDTO.errors + "", containerDTO.errors.isEmpty());
		assertNotNull(containerDTO.template);

		assertTrue(p0.timeout(200).getValue());
	}

}
