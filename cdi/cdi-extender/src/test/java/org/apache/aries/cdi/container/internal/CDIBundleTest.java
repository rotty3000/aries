package org.apache.aries.cdi.container.internal;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.aries.cdi.container.internal.container.ContainerState;
import org.apache.aries.cdi.container.internal.phase.InitPhase;
import org.apache.aries.cdi.container.test.TestUtil;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.MockitoJUnitRunner;
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
import org.osgi.service.cdi.runtime.dto.ContainerDTO;
import org.osgi.service.cdi.runtime.dto.template.ComponentTemplateDTO;
import org.osgi.service.cdi.runtime.dto.template.ConfigurationPolicy;
import org.osgi.service.cdi.runtime.dto.template.ConfigurationTemplateDTO;
import org.osgi.service.cdi.runtime.dto.template.MaximumCardinality;

@RunWith(MockitoJUnitRunner.class)
public class CDIBundleTest {

	@Before
	public void before() throws Exception {
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
				return getClass().getClassLoader().getResource((String)args[0]);
			}
		});
		when(bundle.loadClass(any())).then(new Answer<Class<?>>() {
			@Override
			public Class<?> answer(InvocationOnMock invocation) throws ClassNotFoundException {
				Object[] args = invocation.getArguments();
				return getClass().getClassLoader().loadClass((String)args[0]);
			}
		});
		when(bundleWiring.getBundle()).thenReturn(bundle);
		//when(bundleWiring.getRequiredWires(PackageNamespace.PACKAGE_NAMESPACE)).thenReturn(new ArrayList<>());
		when(bundleWiring.getRequiredWires(ExtenderNamespace.EXTENDER_NAMESPACE)).thenReturn(Collections.singletonList(extenderWire));
		when(bundleWiring.listResources("OSGI-INF/cdi", "*.xml", BundleWiring.LISTRESOURCES_LOCAL)).thenReturn(Collections.singletonList("OSGI-INF/cdi/osgi-beans.xml"));

		when(extenderWire.getCapability()).thenReturn(extenderCapability);
		when(extenderCapability.getAttributes()).thenReturn(Collections.singletonMap(ExtenderNamespace.EXTENDER_NAMESPACE, CDIConstants.CDI_CAPABILITY_NAME));
		when(extenderWire.getRequirement()).thenReturn(extenderRequirement);
		when(extenderRequirement.getAttributes()).thenReturn(new HashMap<>());

		when(ccrBundle.adapt(BundleWiring.class)).thenReturn(ccrBundleWiring);
		when(ccrBundleWiring.getRequiredWires(PackageNamespace.PACKAGE_NAMESPACE)).thenReturn(new ArrayList<>());
		//when(ccrBundleWiring.getBundle()).thenReturn(ccrBundle);
	}

	@Test
	public void initialTest() throws Exception {
		ContainerState containerState = new ContainerState(bundle, ccrBundle);

		CDIBundle cdiBundle = new CDIBundle(ccr, containerState, null);

		cdiBundle.start();

		ContainerDTO containerDTO = ccr.getContainerDTO(bundle);
		assertNotNull(containerDTO);

		assertNotNull(containerDTO.bundle);
		assertEquals(1, containerDTO.bundle.id);
		assertEquals(24l, containerDTO.bundle.lastModified);
		assertEquals(Bundle.ACTIVE, containerDTO.bundle.state);
		assertEquals("foo", containerDTO.bundle.symbolicName);
		assertEquals("1.0.0", containerDTO.bundle.version);

		assertEquals(0, containerDTO.changeCount);

		assertTrue(containerDTO.components + "", containerDTO.components.isEmpty());
		assertTrue(containerDTO.errors + "", containerDTO.errors.isEmpty());
		assertTrue(containerDTO.extensions + "", containerDTO.extensions.isEmpty());

		assertNotNull(containerDTO.template);
		assertEquals(0, containerDTO.template.components.size());
		assertEquals(0, containerDTO.template.extensions.size());
		assertEquals("foo", containerDTO.template.id);

		cdiBundle.destroy();

		assertNull(ccr.getContainerDTO(bundle));
	}

	@Test
	public void extensionsTest() throws Exception {
		Map<String, Object> attributes = new HashMap<>();

		attributes.put(CDIConstants.REQUIREMENT_EXTENSIONS_ATTRIBUTE, Arrays.asList("(foo=name)", "(fum=bar)"));

		when(extenderRequirement.getAttributes()).thenReturn(attributes);

		ContainerState containerState = new ContainerState(bundle, ccrBundle);

		CDIBundle cdiBundle = new CDIBundle(ccr, containerState, null);

		cdiBundle.start();

		ContainerDTO containerDTO = ccr.getContainerDTO(bundle);
		assertNotNull(containerDTO);

		assertNotNull(containerDTO.bundle);
		assertEquals(1, containerDTO.bundle.id);
		assertEquals(24l, containerDTO.bundle.lastModified);
		assertEquals(Bundle.ACTIVE, containerDTO.bundle.state);
		assertEquals("foo", containerDTO.bundle.symbolicName);
		assertEquals("1.0.0", containerDTO.bundle.version);

		assertEquals(0, containerDTO.changeCount);

		assertTrue(containerDTO.components + "", containerDTO.components.isEmpty());
		assertTrue(containerDTO.errors + "", containerDTO.errors.isEmpty());
		assertTrue(containerDTO.extensions + "", containerDTO.extensions.isEmpty());

		assertNotNull(containerDTO.template);
		assertEquals(0, containerDTO.template.components.size());
		assertEquals(2, containerDTO.template.extensions.size());
		assertEquals("(foo=name)", containerDTO.template.extensions.get(0).serviceFilter);
		assertEquals("(fum=bar)", containerDTO.template.extensions.get(1).serviceFilter);
		assertEquals("foo", containerDTO.template.id);

		cdiBundle.destroy();

		assertNull(ccr.getContainerDTO(bundle));
	}

	@Test
	public void componentsTest() throws Exception {
		when(ccrBundle.loadClass(any())).then(new Answer<Class<?>>() {
			@Override
			public Class<?> answer(InvocationOnMock invocation) throws ClassNotFoundException {
				Object[] args = invocation.getArguments();
				return getClass().getClassLoader().loadClass((String)args[0]);
			}
		});

		ContainerState containerState = new ContainerState(bundle, ccrBundle);

		CDIBundle cdiBundle = new CDIBundle(ccr, containerState, new InitPhase(containerState, null));

		cdiBundle.start();

		ContainerDTO containerDTO = ccr.getContainerDTO(bundle);
		assertNotNull(containerDTO);

		assertTrue(containerDTO.errors + "", containerDTO.errors.isEmpty());

		assertNotNull(containerDTO.template);
		List<ComponentTemplateDTO> components = TestUtil.sort(
			containerDTO.template.components, (a, b) -> a.name.compareTo(b.name));
		assertEquals(2, components.size());

		assertEquals(0, components.get(0).activations.size());
		assertEquals(1, components.get(0).beans.size());
		assertEquals(2, components.get(0).configurations.size());
		assertEquals("foo", components.get(0).name);
		assertEquals(6, components.get(0).references.size());
		assertEquals(ComponentTemplateDTO.Type.CONTAINER, components.get(0).type);

		assertEquals(0, components.get(1).activations.size());
		assertEquals(1, components.get(1).beans.size());
		assertEquals(1, components.get(1).configurations.size());
		assertEquals("foo.annotated", components.get(1).name);
		assertEquals(0, components.get(1).references.size());
		assertEquals(ComponentTemplateDTO.Type.SINGLE, containerDTO.template.components.get(1).type);

		cdiBundle.destroy();

		assertNull(ccr.getContainerDTO(bundle));
	}

	@Test
	public void components_2_Test() throws Exception {
		Map<String, Object> attributes = new HashMap<>();

		attributes.put(CDIConstants.REQUIREMENT_OSGI_BEANS_ATTRIBUTE, Arrays.asList("OSGI-INF/cdi/osgi-beans2.xml"));

		when(extenderRequirement.getAttributes()).thenReturn(attributes);

		when(ccrBundle.loadClass(any())).then(new Answer<Class<?>>() {
			@Override
			public Class<?> answer(InvocationOnMock invocation) throws ClassNotFoundException {
				Object[] args = invocation.getArguments();
				return getClass().getClassLoader().loadClass((String)args[0]);
			}
		});

		ContainerState containerState = new ContainerState(bundle, ccrBundle);

		CDIBundle cdiBundle = new CDIBundle(ccr, containerState, new InitPhase(containerState, null));

		cdiBundle.start();

		ContainerDTO containerDTO = ccr.getContainerDTO(bundle);
		assertNotNull(containerDTO);

		assertTrue(containerDTO.errors + "", containerDTO.errors.isEmpty());

		assertNotNull(containerDTO.template);
		List<ComponentTemplateDTO> components = TestUtil.sort(
			containerDTO.template.components, (a, b) -> a.name.compareTo(b.name));
		assertEquals(3, components.size());

		assertEquals(0, components.get(0).activations.size());
		assertEquals(1, components.get(0).beans.size());
		assertEquals(1, components.get(0).configurations.size());
		assertEquals("barService", components.get(0).name);
		assertEquals(0, components.get(0).references.size());
		assertEquals(ComponentTemplateDTO.Type.FACTORY, components.get(0).type);

		assertEquals(0, components.get(1).activations.size());
		assertEquals(3, components.get(1).beans.size());
		assertEquals(2, components.get(1).configurations.size());
		assertEquals("foo", components.get(1).name);
		assertEquals(7, components.get(1).references.size());
		assertEquals(ComponentTemplateDTO.Type.CONTAINER, components.get(1).type);

		assertEquals(0, components.get(2).activations.size());
		assertEquals(1, components.get(2).beans.size());
		assertEquals(1, components.get(2).configurations.size());
		assertEquals("foo.annotated", components.get(2).name);
		assertEquals(0, components.get(2).references.size());
		assertEquals(ComponentTemplateDTO.Type.SINGLE, components.get(2).type);

		cdiBundle.destroy();

		assertNull(ccr.getContainerDTO(bundle));
	}

	@Test
	public void components_verifyContainerComponent_Test() throws Exception {
		Map<String, Object> attributes = new HashMap<>();

		attributes.put(CDIConstants.REQUIREMENT_OSGI_BEANS_ATTRIBUTE, Arrays.asList("OSGI-INF/cdi/osgi-beans2.xml"));

		when(extenderRequirement.getAttributes()).thenReturn(attributes);

		when(ccrBundle.loadClass(any())).then(new Answer<Class<?>>() {
			@Override
			public Class<?> answer(InvocationOnMock invocation) throws ClassNotFoundException {
				Object[] args = invocation.getArguments();
				return getClass().getClassLoader().loadClass((String)args[0]);
			}
		});

		ContainerState containerState = new ContainerState(bundle, ccrBundle);

		CDIBundle cdiBundle = new CDIBundle(ccr, containerState, new InitPhase(containerState, null));

		cdiBundle.start();

		ContainerDTO containerDTO = ccr.getContainerDTO(bundle);
		assertNotNull(containerDTO);

		assertTrue(containerDTO.errors + "", containerDTO.errors.isEmpty());
		assertNotNull(containerDTO.template);

		List<ComponentTemplateDTO> components = TestUtil.sort(
			containerDTO.template.components, (a, b) -> a.name.compareTo(b.name));
		assertEquals(3, components.size());

		{ // component "barService"
			ComponentTemplateDTO componentTemplateDTO = components.get(0);
			assertEquals(0, componentTemplateDTO.activations.size());
			assertEquals(1, componentTemplateDTO.beans.size());
			assertEquals("org.apache.aries.cdi.container.test.beans.BarService", componentTemplateDTO.beans.get(0));
			assertEquals(1, componentTemplateDTO.configurations.size());
			assertEquals("barService", componentTemplateDTO.name);
			assertEquals(0, componentTemplateDTO.references.size());
			assertEquals(ComponentTemplateDTO.Type.FACTORY, componentTemplateDTO.type);

			{ // configuration "barService"
				ConfigurationTemplateDTO configurationTemplateDTO = componentTemplateDTO.configurations.get(0);
				assertEquals(true, configurationTemplateDTO.componentConfiguration);
				assertEquals(MaximumCardinality.MANY, configurationTemplateDTO.maximumCardinality);
				assertEquals("barService", configurationTemplateDTO.pid);
				assertEquals(ConfigurationPolicy.REQUIRED, configurationTemplateDTO.policy);
			}
		}

		{ // component "foo"
			ComponentTemplateDTO componentTemplateDTO = components.get(1);

			assertEquals(0, componentTemplateDTO.activations.size());
			assertEquals(3, componentTemplateDTO.beans.size());

			List<String> beans = TestUtil.sort(componentTemplateDTO.beans, (a, b) -> a.compareTo(b));
			assertEquals("org.apache.aries.cdi.container.test.beans.BarAnnotated", beans.get(0));
			assertEquals("org.apache.aries.cdi.container.test.beans.BarProducer", beans.get(1));
			assertEquals("org.apache.aries.cdi.container.test.beans.ObserverFoo", beans.get(2));

			assertEquals(2, componentTemplateDTO.configurations.size());
			assertEquals("foo", componentTemplateDTO.name);
			assertEquals(7, componentTemplateDTO.references.size());
			assertEquals(ComponentTemplateDTO.Type.CONTAINER, componentTemplateDTO.type);

			{ // configuration "osgi.cdi.foo"
				ConfigurationTemplateDTO configurationTemplateDTO = componentTemplateDTO.configurations.get(0);
				assertEquals(true, configurationTemplateDTO.componentConfiguration);
				assertEquals(MaximumCardinality.ONE, configurationTemplateDTO.maximumCardinality);
				assertEquals("osgi.cdi.foo", configurationTemplateDTO.pid);
				assertEquals(ConfigurationPolicy.OPTIONAL, configurationTemplateDTO.policy);
			}

			{ // "foo.config
				ConfigurationTemplateDTO configurationTemplateDTO = componentTemplateDTO.configurations.get(1);
				assertEquals(false, configurationTemplateDTO.componentConfiguration);
				assertEquals(MaximumCardinality.ONE, configurationTemplateDTO.maximumCardinality);
				assertEquals("foo.config", configurationTemplateDTO.pid);
				assertEquals(ConfigurationPolicy.REQUIRED, configurationTemplateDTO.policy);
			}
		}

		{ // component "foo.annotated"
			ComponentTemplateDTO componentTemplateDTO = components.get(2);
			assertEquals(0, componentTemplateDTO.activations.size());
			assertEquals(1, componentTemplateDTO.beans.size());
			assertEquals("org.apache.aries.cdi.container.test.beans.FooAnnotated", componentTemplateDTO.beans.get(0));
			assertEquals(1, componentTemplateDTO.configurations.size());
			assertEquals("foo.annotated", componentTemplateDTO.name);
			assertEquals(0, componentTemplateDTO.references.size());
			assertEquals(ComponentTemplateDTO.Type.SINGLE, componentTemplateDTO.type);

			{ // configuration "foo.annotated"
				ConfigurationTemplateDTO configurationTemplateDTO = componentTemplateDTO.configurations.get(0);
				assertEquals(true, configurationTemplateDTO.componentConfiguration);
				assertEquals(MaximumCardinality.ONE, configurationTemplateDTO.maximumCardinality);
				assertEquals("foo.annotated", configurationTemplateDTO.pid);
				assertEquals(ConfigurationPolicy.OPTIONAL, configurationTemplateDTO.policy);
			}
		}

		cdiBundle.destroy();

		assertNull(ccr.getContainerDTO(bundle));
	}

	@Mock
	Bundle bundle;
	@Mock
	BundleWiring bundleWiring;
	@Mock
	Bundle ccrBundle;
	@Mock
	BundleWiring ccrBundleWiring;
	@Mock
	BundleCapability extenderCapability;
	@Mock
	BundleRequirement extenderRequirement;
	@Mock
	BundleWire extenderWire;

	CCR ccr = new CCR();

}
