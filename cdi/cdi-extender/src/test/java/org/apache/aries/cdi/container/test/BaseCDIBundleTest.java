package org.apache.aries.cdi.container.test;

import static org.apache.aries.cdi.container.internal.util.Reflection.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.net.URL;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

import org.apache.aries.cdi.container.internal.CCR;
import org.apache.aries.cdi.container.internal.ChangeCount;
import org.junit.Before;
import org.mockito.stubbing.Answer;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceRegistration;
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
	protected volatile List<Map.Entry<ServiceListener, Filter>> serviceListeners;
	protected volatile List<MockServiceRegistration<?>> serviceRegistrations;
	protected PromiseFactory testPromiseFactory = new PromiseFactory(null);

	@Before
	public void before() throws Exception {
		serviceListeners = new CopyOnWriteArrayList<>();
		serviceRegistrations = new CopyOnWriteArrayList<>();
		promiseFactory = new PromiseFactory(Executors.newFixedThreadPool(1));
		ccr = new CCR(promiseFactory);
		ccrChangeCount = new ChangeCount();

		BundleDTO ccrBundleDTO = new BundleDTO();
		ccrBundleDTO.id = 2;
		ccrBundleDTO.lastModified = 100l;
		ccrBundleDTO.state = Bundle.ACTIVE;
		ccrBundleDTO.symbolicName = "extender";
		ccrBundleDTO.version = "1.0.0";

		ccrBundle = mockBundle(
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

		bundle = mockBundle(
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

	@SuppressWarnings("unchecked")
	Bundle mockBundle(BundleDTO bundleDTO, Consumer<Bundle> extra) throws Exception {
		Bundle bundle = mock(Bundle.class);
		BundleContext bundleContext = mock(BundleContext.class);
		BundleWiring bundleWiring = mock(BundleWiring.class);

		when(bundle.getBundleContext()).thenReturn(bundleContext);
		when(bundle.toString()).thenReturn(bundleDTO.symbolicName + "[" + bundleDTO.id + "]");
		when(bundle.getBundleId()).thenReturn(bundleDTO.id);
		when(bundle.getLastModified()).thenReturn(bundleDTO.lastModified);
		when(bundle.getSymbolicName()).thenReturn(bundleDTO.symbolicName);
		when(bundle.adapt(BundleWiring.class)).thenReturn(bundleWiring);
		when(bundle.adapt(BundleDTO.class)).thenReturn(bundleDTO);
		when(bundle.getResource(any())).then(
			(Answer<URL>) getResource -> {
				return getClass().getClassLoader().getResource((String)getResource.getArgument(0));
			}
		);
		when(bundle.loadClass(any())).then(
			(Answer<Class<?>>) loadClass -> {
				return getClass().getClassLoader().loadClass((String)loadClass.getArgument(0));
			}
		);
		when(bundleWiring.getBundle()).thenReturn(bundle);
		when(bundleContext.getBundle()).thenReturn(bundle);
		when(bundleContext.getService(any())).then(
			(Answer<Object>) getService -> {
				return serviceRegistrations.stream().filter(
					reg -> reg.getReference().equals(getService.getArgument(0))
				).findFirst().get().getReference().getService();
			}
		);
		doAnswer(
			(Answer<ServiceRegistration<?>>) registerService -> {
				Class<?> clazz = registerService.getArgument(0);
				MockServiceReference<?> mockServiceReference = new MockServiceReference<>(
					bundle, registerService.getArgument(1), clazz);

				Optional.ofNullable(
					registerService.getArgument(2)
				).map(
					arg -> (Dictionary<String, Object>)arg
				).ifPresent(
					dict -> {
						for (Enumeration<String> enu = dict.keys(); enu.hasMoreElements();) {
							String key = enu.nextElement();
							if (key.equals(Constants.OBJECTCLASS) ||
								key.equals(Constants.SERVICE_BUNDLEID) ||
								key.equals(Constants.SERVICE_ID) ||
								key.equals(Constants.SERVICE_SCOPE)) {
								continue;
							}
							mockServiceReference.setProperty(key, dict.get(key));
						}
					}
				);

				MockServiceRegistration<?> mockServiceRegistration = new MockServiceRegistration<>(mockServiceReference);
				serviceRegistrations.add(mockServiceRegistration);
				return mockServiceRegistration;
			}
		).when(bundleContext).registerService(any(Class.class), any(Object.class), any());
		doAnswer(
			(Answer<Void>) addServiceListener -> {
				ServiceListener sl = cast(addServiceListener.getArgument(0));
				Filter filter = FrameworkUtil.createFilter(addServiceListener.getArgument(1));
				serviceListeners.add(new SimpleEntry<>(sl, filter));
				return null;
			}
		).when(bundleContext).addServiceListener(any(), any());

		extra.accept(bundle);

		return bundle;
	}

}
