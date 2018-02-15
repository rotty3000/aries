package org.apache.aries.cdi.container.internal.phase;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import javax.enterprise.inject.spi.Extension;

import org.apache.aries.cdi.container.internal.CDIBundle;
import org.apache.aries.cdi.container.internal.container.ContainerState;
import org.apache.aries.cdi.container.test.BaseCDIBundleTest;
import org.apache.aries.cdi.container.test.MockServiceReference;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.dto.ServiceReferenceDTO;
import org.osgi.service.cdi.CDIConstants;
import org.osgi.service.cdi.runtime.dto.ContainerDTO;
import org.osgi.util.promise.Deferred;

public class ExtensionPhaseTest extends BaseCDIBundleTest {

	@Test
	public void extensions_tracking() throws Exception {
		when(bundle.getBundleContext()).thenReturn(bundleContext);

		Deferred<ServiceListener> slD = promiseFactory.deferred();
		Deferred<String> filterD = promiseFactory.deferred();

		doAnswer(
			(Answer<?>) invocation -> {
				slD.resolve((ServiceListener)invocation.getArgument(0));
				filterD.resolve((String)invocation.getArgument(1));
				return null;
			}
		).when(bundleContext).addServiceListener(any(), any());


		when(ccrBundle.loadClass(any())).then(new Answer<Class<?>>() {
			@Override
			public Class<?> answer(InvocationOnMock invocation) throws ClassNotFoundException {
				Object[] args = invocation.getArguments();
				return getClass().getClassLoader().loadClass((String)args[0]);
			}
		});

		Map<String, Object> attributes = new HashMap<>();

		attributes.put(CDIConstants.REQUIREMENT_EXTENSIONS_ATTRIBUTE, Arrays.asList("(foo=name)"));

		when(extenderRequirement.getAttributes()).thenReturn(attributes);

		ContainerState containerState = new ContainerState(bundle, ccrBundle, ccrChangeCount, promiseFactory);

		CDIBundle cdiBundle = new CDIBundle(
			ccr, containerState,
				new InitPhase(containerState,
					new ExtensionPhase(containerState, null)));

		cdiBundle.start();

		ContainerDTO containerDTO = ccr.getContainerDTO(bundle);
		assertNotNull(containerDTO);

		assertNotNull(containerDTO.bundle);
		assertEquals(1, containerDTO.bundle.id);
		assertEquals(24l, containerDTO.bundle.lastModified);
		assertEquals(Bundle.ACTIVE, containerDTO.bundle.state);
		assertEquals("foo", containerDTO.bundle.symbolicName);
		assertEquals("1.0.0", containerDTO.bundle.version);

		assertEquals(1, containerDTO.changeCount);

		assertTrue(containerDTO.errors + "", containerDTO.errors.isEmpty());
		assertTrue(containerDTO.extensions + "", containerDTO.extensions.isEmpty());

		assertNotNull(containerDTO.template);
		assertEquals(1, containerDTO.template.extensions.size());
		assertEquals("(foo=name)", containerDTO.template.extensions.get(0).serviceFilter);

		MockServiceReference<Extension> refA = new MockServiceReference<>(bundle, new Extension(){});
		refA.setProperty("foo", "name");
		MockServiceReference<Extension> refB = new MockServiceReference<>(bundle, new Extension(){});
		refB.setProperty("foo", "name");
		refB.setProperty(Constants.SERVICE_RANKING, 10);

		ServiceReferenceDTO[] dtos = new ServiceReferenceDTO[] {refA.toDTO(), refB.toDTO()};

		when(bundle.adapt(ServiceReferenceDTO[].class)).thenReturn(dtos);
		when(bundleContext.getService(refA)).thenReturn(refA.getService());
		when(bundleContext.getService(refB)).thenReturn(refB.getService());

		slD.getPromise().thenAccept(
			sl -> {
				sl.serviceChanged(new ServiceEvent(ServiceEvent.REGISTERED, refA));

				assertEquals(2, containerState.containerDTO().changeCount);
				assertEquals(1, containerState.containerDTO().extensions.size());
				assertEquals(1, containerState.containerDTO().extensions.get(0).service.id);

				sl.serviceChanged(new ServiceEvent(ServiceEvent.REGISTERED, refB));

				assertEquals(3, containerState.containerDTO().changeCount);
				assertEquals(1, containerState.containerDTO().extensions.size());
				assertEquals(2, containerState.containerDTO().extensions.get(0).service.id);

				sl.serviceChanged(new ServiceEvent(ServiceEvent.UNREGISTERING, refB));

				assertEquals(4, containerState.containerDTO().changeCount);
				assertEquals(1, containerState.containerDTO().extensions.size());
				assertEquals(1, containerState.containerDTO().extensions.get(0).service.id);

				sl.serviceChanged(new ServiceEvent(ServiceEvent.UNREGISTERING, refA));

				assertEquals(0, containerState.containerDTO().extensions.size());
			}
		).getValue();

		cdiBundle.destroy();
	}

}
