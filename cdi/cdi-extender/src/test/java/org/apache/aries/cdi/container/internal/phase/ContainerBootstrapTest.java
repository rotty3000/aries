package org.apache.aries.cdi.container.internal.phase;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.aries.cdi.container.internal.container.CheckedCallback;
import org.apache.aries.cdi.container.internal.container.ConfigurationListener;
import org.apache.aries.cdi.container.internal.container.ContainerBootstrap;
import org.apache.aries.cdi.container.internal.container.ContainerState;
import org.apache.aries.cdi.container.internal.container.Op;
import org.apache.aries.cdi.container.internal.model.ExtendedReferenceDTO;
import org.apache.aries.cdi.container.internal.model.FactoryComponent;
import org.apache.aries.cdi.container.internal.model.SingleComponent;
import org.apache.aries.cdi.container.internal.util.Maps;
import org.apache.aries.cdi.container.internal.util.SRs;
import org.apache.aries.cdi.container.test.BaseCDIBundleTest;
import org.apache.aries.cdi.container.test.TestUtil;
import org.apache.aries.cdi.container.test.beans.Foo;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.dto.BundleDTO;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.namespace.extender.ExtenderNamespace;
import org.osgi.service.cdi.CDIConstants;
import org.osgi.service.cdi.MaximumCardinality;
import org.osgi.service.cdi.ReferencePolicy;
import org.osgi.service.cdi.ReferencePolicyOption;
import org.osgi.service.cdi.runtime.dto.ComponentInstanceDTO;
import org.osgi.service.cdi.runtime.dto.ContainerDTO;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.log.LoggerFactory;
import org.osgi.util.promise.Promise;
import org.osgi.util.promise.TimeoutException;
import org.osgi.util.tracker.ServiceTracker;

public class ContainerBootstrapTest extends BaseCDIBundleTest {

	@Test
	public void test_S_R_M_U_Service() throws Exception {
		Map<String, Object> attributes = new HashMap<>();

		attributes.put(
			CDIConstants.REQUIREMENT_OSGI_BEANS_ATTRIBUTE,
			Arrays.asList(
				"org.apache.aries.cdi.container.test.beans.Reference_S_R_M_U_Service"
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

		ContainerBootstrap containerBootstrap = new ContainerBootstrap(
			containerState,
			new ConfigurationListener.Builder(containerState),
			new SingleComponent.Builder(containerState, null),
			new FactoryComponent.Builder(containerState, null));

		Promise<Boolean> p0 = containerState.addCallback(
			(CheckedCallback<Boolean, Boolean>) cc -> {
				return cc == Op.CONTAINER_REFERENCES_OPEN;
			}
		);

		containerBootstrap.open();

		ContainerDTO containerDTO = containerState.containerDTO();
		assertNotNull(containerDTO);
		assertEquals(1, containerDTO.changeCount);
		assertTrue(containerDTO.errors + "", containerDTO.errors.isEmpty());
		assertNotNull(containerDTO.template);

		assertNotNull(p0.timeout(200).getValue());

		ComponentInstanceDTO containerComponentInstance = containerDTO.components.get(0).instances.get(0);

		List<ExtendedReferenceDTO> unresolvedReferences = containerComponentInstance.references.stream().map(
			r -> (ExtendedReferenceDTO)r
		).filter(
			r -> r.matches.size() < r.minimumCardinality
		).collect(Collectors.toList());

		assertEquals(1, unresolvedReferences.size());

		ExtendedReferenceDTO extendedReferenceDTO = unresolvedReferences.get(0);

		assertTrue(extendedReferenceDTO.matches.isEmpty());
		assertEquals(1, extendedReferenceDTO.minimumCardinality);
		assertNotNull(extendedReferenceDTO.serviceTracker);
		assertEquals("(objectClass=org.apache.aries.cdi.container.test.beans.Foo)", extendedReferenceDTO.targetFilter);
		assertNotNull(extendedReferenceDTO.template);
		assertEquals(MaximumCardinality.ONE, extendedReferenceDTO.template.maximumCardinality);
		assertEquals(1, extendedReferenceDTO.template.minimumCardinality);
		assertEquals("org.apache.aries.cdi.container.test.beans.Reference_S_R_M_U_Service.foo", extendedReferenceDTO.template.name);
		assertEquals(ReferencePolicy.STATIC, extendedReferenceDTO.template.policy);
		assertEquals(ReferencePolicyOption.RELUCTANT, extendedReferenceDTO.template.policyOption);
		assertEquals(Foo.class.getName(), extendedReferenceDTO.template.serviceType);
		assertEquals("", extendedReferenceDTO.template.targetFilter);

		// first test publishing a service targeting one of the optional references

		BundleDTO serviceBundleDTO = new BundleDTO();

		Bundle serviceBundle = TestUtil.mockBundle(serviceBundleDTO, b -> {});

		p0 = containerState.addCallback(
			(CheckedCallback<Boolean, Boolean>) cc -> {
				return cc == Op.CONTAINER_INSTANCE_OPEN;
			}
		);

		ServiceRegistration<Foo> sr1 = serviceBundle.getBundleContext().registerService(
			Foo.class, new Foo() {}, Maps.dict("sr1", "sr1"));

		assertTrue(p0.timeout(200).getValue());

		assertEquals(1, extendedReferenceDTO.matches.size());

		p0 = containerState.addCallback(
			(CheckedCallback<Boolean, Boolean>) cc -> {
				return cc == Op.CONTAINER_INSTANCE_CLOSE;
			}
		);

		serviceBundle.getBundleContext().registerService(
			Foo.class, new Foo() {}, Maps.dict("sr2", "sr2"));

		assertTrue("should be a TimeoutException", TimeoutException.class.equals(p0.timeout(200).getFailure().getClass()));

		assertEquals(2, extendedReferenceDTO.matches.size());

		p0 = containerState.addCallback(
			(CheckedCallback<Boolean, Boolean>) cc -> {
				return cc == Op.CONTAINER_INSTANCE_CLOSE;
			}
		);

		Foo foo = new Foo() {};

		ServiceRegistration<Foo> sr3 = serviceBundle.getBundleContext().registerService(
			Foo.class, foo, Maps.dict("sr3", "sr3", Constants.SERVICE_RANKING, 100));

		assertTrue("should be a TimeoutException", TimeoutException.class.equals(p0.timeout(200).getFailure().getClass()));

		assertEquals(3, extendedReferenceDTO.matches.size());

		p0 = containerState.addCallback(
			(CheckedCallback<Boolean, Boolean>) cc -> {
				return cc == Op.CONTAINER_INSTANCE_CLOSE;
			}
		);
		Promise<Boolean> p1 = containerState.addCallback(
			(CheckedCallback<Boolean, Boolean>) cc -> {
				return cc == Op.CONTAINER_INSTANCE_OPEN;
			}
		);

		sr1.unregister();

		assertNull(p0.timeout(200).getFailure());

		assertEquals(2, extendedReferenceDTO.matches.size());
		assertEquals(SRs.id(sr3.getReference()), SRs.id(extendedReferenceDTO.serviceTracker.getServiceReference()));
		assertEquals(foo, extendedReferenceDTO.serviceTracker.getService());

		assertTrue(p1.timeout(200).getValue());
	}

}
