package org.apache.aries.cdi.container.test;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.concurrent.Executors;

import org.apache.aries.cdi.container.internal.CCR;
import org.apache.aries.cdi.container.internal.ChangeCount;
import org.junit.Before;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.dto.BundleDTO;
import org.osgi.framework.namespace.PackageNamespace;
import org.osgi.framework.wiring.BundleCapability;
import org.osgi.framework.wiring.BundleRequirement;
import org.osgi.framework.wiring.BundleWire;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.namespace.extender.ExtenderNamespace;
import org.osgi.service.cdi.CDIConstants;
import org.osgi.util.promise.PromiseFactory;

public class BaseCDIBundleTest {

	protected Bundle bundle;
	protected BundleContext bundleContext;
	protected BundleWiring bundleWiring;
	protected CCR ccr;
	protected Bundle ccrBundle;
	protected BundleWiring ccrBundleWiring;
	protected ChangeCount ccrChangeCount;
	protected BundleCapability extenderCapability;
	protected BundleRequirement extenderRequirement;
	protected BundleWire extenderWire;
	protected PromiseFactory promiseFactory = new PromiseFactory(Executors.newFixedThreadPool(1));

	@Before
	public void before() throws Exception {
		bundle = mock(Bundle.class);
		bundleContext = mock(BundleContext.class);
		bundleWiring = mock(BundleWiring.class);
		ccr = new CCR(promiseFactory);
		ccrBundle = mock(Bundle.class);
		ccrBundleWiring = mock(BundleWiring.class);
		ccrChangeCount = new ChangeCount();
		extenderCapability = mock(BundleCapability.class);
		extenderRequirement = mock(BundleRequirement.class);
		extenderWire = mock(BundleWire.class);

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

}
