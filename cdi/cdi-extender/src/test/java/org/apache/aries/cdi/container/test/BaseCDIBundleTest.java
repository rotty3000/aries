package org.apache.aries.cdi.container.test;

import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.concurrent.Executors;

import org.apache.aries.cdi.container.internal.CCR;
import org.apache.aries.cdi.container.internal.ChangeCount;
import org.junit.Before;
import org.osgi.framework.Bundle;
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
	protected CCR ccr;
	protected Bundle ccrBundle;
	protected ChangeCount ccrChangeCount;
	protected PromiseFactory promiseFactory;
	protected PromiseFactory testPromiseFactory = new PromiseFactory(null);

	@Before
	public void before() throws Exception {
		TestUtil.configurations.clear();
		TestUtil.serviceListeners.clear();
		TestUtil.serviceRegistrations.clear();
		promiseFactory = new PromiseFactory(Executors.newFixedThreadPool(1));
		ccr = new CCR(promiseFactory);
		ccrChangeCount = new ChangeCount();

		BundleDTO ccrBundleDTO = new BundleDTO();
		ccrBundleDTO.id = 2;
		ccrBundleDTO.lastModified = 100l;
		ccrBundleDTO.state = Bundle.ACTIVE;
		ccrBundleDTO.symbolicName = "extender";
		ccrBundleDTO.version = "1.0.0";

		ccrBundle = TestUtil.mockBundle(
			ccrBundleDTO, b -> {
				when(
					b.adapt(BundleWiring.class).getRequiredWires(PackageNamespace.PACKAGE_NAMESPACE)
				).thenReturn(new ArrayList<>());
			}
		);

		BundleDTO bundleDTO = new BundleDTO();
		bundleDTO.id = 1;
		bundleDTO.lastModified = 24l;
		bundleDTO.state = Bundle.ACTIVE;
		bundleDTO.symbolicName = "foo";
		bundleDTO.version = "1.0.0";

		bundle = TestUtil.mockBundle(
			bundleDTO, b -> {
				BundleCapability extenderCapability = mock(BundleCapability.class);
				BundleRequirement extenderRequirement = mock(BundleRequirement.class);
				BundleWire extenderWire = mock(BundleWire.class);

				when(
					b.adapt(BundleWiring.class).getRequiredWires(ExtenderNamespace.EXTENDER_NAMESPACE)
				).thenReturn(Collections.singletonList(extenderWire));
				when(
					b.adapt(BundleWiring.class).listResources("OSGI-INF/cdi", "*.xml", BundleWiring.LISTRESOURCES_LOCAL)
				).thenReturn(Collections.singletonList("OSGI-INF/cdi/osgi-beans.xml"));
				when(extenderWire.getCapability()).thenReturn(extenderCapability);
				when(extenderCapability.getAttributes()).thenReturn(Collections.singletonMap(ExtenderNamespace.EXTENDER_NAMESPACE, CDIConstants.CDI_CAPABILITY_NAME));
				when(extenderWire.getRequirement()).thenReturn(extenderRequirement);
				when(extenderRequirement.getAttributes()).thenReturn(new HashMap<>());
			}
		);
	}

}
