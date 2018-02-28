package org.apache.aries.cdi.container.internal.phase;

import static org.junit.Assert.*;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

import javax.inject.Provider;

import org.apache.aries.cdi.container.internal.container.CheckedCallback;
import org.apache.aries.cdi.container.internal.container.ConfigurationListener;
import org.apache.aries.cdi.container.internal.container.ContainerState;
import org.apache.aries.cdi.container.internal.container.Op;
import org.apache.aries.cdi.container.internal.model.CollectionType;
import org.apache.aries.cdi.container.internal.model.ContainerComponent;
import org.apache.aries.cdi.container.internal.model.ExtendedReferenceDTO;
import org.apache.aries.cdi.container.internal.model.ExtendedReferenceTemplateDTO;
import org.apache.aries.cdi.container.internal.util.Maps;
import org.apache.aries.cdi.container.test.BaseCDIBundleTest;
import org.apache.aries.cdi.container.test.MockConfiguration;
import org.apache.aries.cdi.container.test.TestUtil;
import org.apache.aries.cdi.container.test.beans.Foo;
import org.junit.Test;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cdi.runtime.dto.ComponentDTO;
import org.osgi.service.cdi.runtime.dto.ComponentInstanceDTO;
import org.osgi.service.cdi.runtime.dto.ContainerDTO;
import org.osgi.service.cdi.runtime.dto.ReferenceDTO;
import org.osgi.service.cdi.runtime.dto.template.ComponentTemplateDTO;
import org.osgi.service.cdi.runtime.dto.template.ComponentTemplateDTO.Type;
import org.osgi.service.cdi.runtime.dto.template.MaximumCardinality;
import org.osgi.service.cdi.runtime.dto.template.ReferenceTemplateDTO;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.util.converter.TypeReference;
import org.osgi.util.promise.Promise;
import org.osgi.util.tracker.ServiceTracker;

public class ContainerReferencesTest extends BaseCDIBundleTest {

	@Test
	public void reference_tracking() throws Exception {
		ServiceTracker<ConfigurationAdmin, ConfigurationAdmin> caTracker = TestUtil.mockCaSt(bundle);

		MockConfiguration mockConfiguration = new MockConfiguration("foo.config", null);
		mockConfiguration.update(Maps.dict("fiz", "buz"));
		TestUtil.configurations.add(mockConfiguration);

		ContainerState containerState = new ContainerState(bundle, ccrBundle, ccrChangeCount, promiseFactory, caTracker);

		ComponentTemplateDTO containerTemplate = containerState.containerDTO().template.components.get(0);

		ContainerComponent containerComponent = new ContainerComponent(containerState, containerTemplate);

		ConfigurationListener configurationListener = new ConfigurationListener(containerState, containerComponent);

		Promise<Boolean> p0 = containerState.addCallback(
			(CheckedCallback<Boolean, Boolean>) cc -> {
				return cc == Op.CONTAINER_COMPONENT_START;
			}
		);

		configurationListener.open();

		ContainerDTO containerDTO = containerState.containerDTO();
		assertNotNull(containerDTO);
		assertEquals(1, containerDTO.changeCount);
		assertTrue(containerDTO.errors + "", containerDTO.errors.isEmpty());
		assertNotNull(containerDTO.template);

		p0.getValue();

		ComponentDTO componentDTO = containerDTO.components.stream().filter(
			c -> c.template.type == Type.CONTAINER
		).findFirst().get();

		assertNotNull(componentDTO);
		assertEquals(1, componentDTO.instances.size());

		ComponentInstanceDTO componentInstanceDTO = componentDTO.instances.get(0);

		assertNotNull(componentInstanceDTO);
		assertEquals(6, componentInstanceDTO.references.size());

		// are we currently blocked waiting for those references?

		Promise<Boolean> p1 = containerState.addCallback(
			(CheckedCallback<Boolean, Boolean>) cc -> {
				return cc == Op.CONTAINER_INSTANCE_ACTIVATE;
			}
		);

		assertNotNull(p1.timeout(500).getFailure());

		List<ReferenceDTO> references = TestUtil.sort(
			componentInstanceDTO.references, (a, b) -> a.template.name.compareTo(b.template.name));

		{
			ExtendedReferenceDTO reference = (ExtendedReferenceDTO)references.get(0);
			ExtendedReferenceTemplateDTO template = (ExtendedReferenceTemplateDTO)reference.template;

			assertEquals(CollectionType.SERVICE, template.collectionType);
			assertEquals(new TypeReference<Provider<Collection<Foo>>>() {}.getType(), template.injectionPointType);
			assertEquals(MaximumCardinality.MANY, template.maximumCardinality);
			assertEquals(0, template.minimumCardinality);
			assertEquals("org.apache.aries.cdi.container.test.beans.BarAnnotated.dynamicFoos", template.name);
			assertEquals(ReferenceTemplateDTO.Policy.DYNAMIC, template.policy);
			assertEquals(ReferenceTemplateDTO.PolicyOption.RELUCTANT, template.policyOption);
			assertEquals(Foo.class.getName(), template.serviceType);
			assertEquals("", template.targetFilter);

			assertEquals(0, reference.matches.size());
			assertEquals(0, reference.minimumCardinality);
			assertEquals("(objectClass=" + Foo.class.getName() + ")", reference.targetFilter);
		}

		{
			ExtendedReferenceDTO reference = (ExtendedReferenceDTO)references.get(1);
			ExtendedReferenceTemplateDTO template = (ExtendedReferenceTemplateDTO)reference.template;

			assertEquals(CollectionType.SERVICE, template.collectionType);
			assertEquals(new TypeReference<Foo>() {}.getType(), template.injectionPointType);
			assertEquals(MaximumCardinality.ONE, template.maximumCardinality);
			assertEquals(1, template.minimumCardinality);
			assertEquals("org.apache.aries.cdi.container.test.beans.BarAnnotated.foo", template.name);
			assertEquals(ReferenceTemplateDTO.Policy.STATIC, template.policy);
			assertEquals(ReferenceTemplateDTO.PolicyOption.GREEDY, template.policyOption);
			assertEquals(Foo.class.getName(), template.serviceType);
			assertEquals("", template.targetFilter);

			assertEquals(0, reference.matches.size());
			assertEquals(1, reference.minimumCardinality);
			assertEquals("(objectClass=" + Foo.class.getName() + ")", reference.targetFilter);
		}

		{
			ExtendedReferenceDTO reference = (ExtendedReferenceDTO)references.get(2);
			ExtendedReferenceTemplateDTO template = (ExtendedReferenceTemplateDTO)reference.template;

			assertEquals(CollectionType.SERVICE, template.collectionType);
			assertEquals(new TypeReference<Optional<Foo>>() {}.getType(), template.injectionPointType);
			assertEquals(MaximumCardinality.ONE, template.maximumCardinality);
			assertEquals(0, template.minimumCardinality);
			assertEquals("org.apache.aries.cdi.container.test.beans.BarAnnotated.fooOptional", template.name);
			assertEquals(ReferenceTemplateDTO.Policy.STATIC, template.policy);
			assertEquals(ReferenceTemplateDTO.PolicyOption.RELUCTANT, template.policyOption);
			assertEquals(Foo.class.getName(), template.serviceType);
			assertEquals("", template.targetFilter);

			assertEquals(0, reference.matches.size());
			assertEquals(0, reference.minimumCardinality);
			assertEquals("(objectClass=" + Foo.class.getName() + ")", reference.targetFilter);
		}

		{
			ExtendedReferenceDTO reference = (ExtendedReferenceDTO)references.get(3);
			ExtendedReferenceTemplateDTO template = (ExtendedReferenceTemplateDTO)reference.template;

			assertEquals(CollectionType.PROPERTIES, template.collectionType);
			assertEquals(new TypeReference<Collection<Map<String, Object>>>() {}.getType(), template.injectionPointType);
			assertEquals(MaximumCardinality.MANY, template.maximumCardinality);
			assertEquals(0, template.minimumCardinality);
			assertEquals("org.apache.aries.cdi.container.test.beans.BarAnnotated.propertiesFoos", template.name);
			assertEquals(ReferenceTemplateDTO.Policy.STATIC, template.policy);
			assertEquals(ReferenceTemplateDTO.PolicyOption.RELUCTANT, template.policyOption);
			assertEquals(Foo.class.getName(), template.serviceType);
			assertEquals("", template.targetFilter);

			assertEquals(0, reference.matches.size());
			assertEquals(0, reference.minimumCardinality);
			assertEquals("(objectClass=" + Foo.class.getName() + ")", reference.targetFilter);
		}

		{
			ExtendedReferenceDTO reference = (ExtendedReferenceDTO)references.get(4);
			ExtendedReferenceTemplateDTO template = (ExtendedReferenceTemplateDTO)reference.template;

			assertEquals(CollectionType.REFERENCE, template.collectionType);
			assertEquals(new TypeReference<Collection<ServiceReference<Foo>>>() {}.getType(), template.injectionPointType);
			assertEquals(MaximumCardinality.MANY, template.maximumCardinality);
			assertEquals(0, template.minimumCardinality);
			assertEquals("org.apache.aries.cdi.container.test.beans.BarAnnotated.serviceReferencesFoos", template.name);
			assertEquals(ReferenceTemplateDTO.Policy.STATIC, template.policy);
			assertEquals(ReferenceTemplateDTO.PolicyOption.RELUCTANT, template.policyOption);
			assertEquals(Foo.class.getName(), template.serviceType);
			assertEquals("(service.scope=prototype)", template.targetFilter);

			assertEquals(0, reference.matches.size());
			assertEquals(0, reference.minimumCardinality);
			assertEquals("(&(objectClass=" + Foo.class.getName() + ")(service.scope=prototype))", reference.targetFilter);
		}

		{
			ExtendedReferenceDTO reference = (ExtendedReferenceDTO)references.get(5);
			ExtendedReferenceTemplateDTO template = (ExtendedReferenceTemplateDTO)reference.template;

			assertEquals(CollectionType.TUPLE, template.collectionType);
			assertEquals(new TypeReference<Collection<Entry<Map<String, Object>, Integer>>>() {}.getType(), template.injectionPointType);
			assertEquals(MaximumCardinality.MANY, template.maximumCardinality);
			assertEquals(0, template.minimumCardinality);
			assertEquals("org.apache.aries.cdi.container.test.beans.BarAnnotated.tupleIntegers", template.name);
			assertEquals(ReferenceTemplateDTO.Policy.STATIC, template.policy);
			assertEquals(ReferenceTemplateDTO.PolicyOption.RELUCTANT, template.policyOption);
			assertEquals(Integer.class.getName(), template.serviceType);
			assertEquals("", template.targetFilter);

			assertEquals(0, reference.matches.size());
			assertEquals(0, reference.minimumCardinality);
			assertEquals("(objectClass=" + Integer.class.getName() + ")", reference.targetFilter);
		}
	}

	@Test
	public void check_all_components() throws Exception {
		ServiceTracker<ConfigurationAdmin, ConfigurationAdmin> caTracker = TestUtil.mockCaSt(bundle);

		MockConfiguration mockConfiguration = new MockConfiguration("foo.config", null);
		mockConfiguration.update(Maps.dict("fiz", "buz"));
		TestUtil.configurations.add(mockConfiguration);

		ContainerState containerState = new ContainerState(bundle, ccrBundle, ccrChangeCount, promiseFactory, caTracker);

		ComponentTemplateDTO containerTemplate = containerState.containerDTO().template.components.get(0);

		ContainerComponent containerComponent = new ContainerComponent(containerState, containerTemplate);

		ConfigurationListener configurationListener = new ConfigurationListener(containerState, containerComponent);

		Promise<Boolean> p0 = containerState.addCallback(
			(CheckedCallback<Boolean, Boolean>) cc -> {
				return cc == Op.CONTAINER_COMPONENT_START;
			}
		);

		configurationListener.open();

		ContainerDTO containerDTO = containerState.containerDTO();
		assertNotNull(containerDTO);
		assertEquals(1, containerDTO.changeCount);
		assertTrue(containerDTO.errors + "", containerDTO.errors.isEmpty());
		assertNotNull(containerDTO.template);

		p0.getValue();

	}

}
