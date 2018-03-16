package org.apache.aries.cdi.extension.jndi;

import java.util.Dictionary;
import java.util.Hashtable;

import javax.enterprise.inject.spi.Extension;

import org.osgi.annotation.bundle.Capability;
import org.osgi.annotation.bundle.Header;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.osgi.namespace.service.ServiceNamespace;
import org.osgi.service.cdi.annotations.RequireCDIImplementation;

@Capability(
	attribute = {
		"objectClass:List<String>=javax.enterprise.inject.spi.Extension",
		"osgi.cdi.extension=aries.cdi.jndi"},
	namespace = ServiceNamespace.SERVICE_NAMESPACE
)
@Header(
	name = Constants.BUNDLE_ACTIVATOR,
	value = "org.apache.aries.cdi.extension.jndi.JndiActivator"
)
@RequireCDIImplementation
public class JndiActivator implements BundleActivator {

	@Override
	public void start(BundleContext context) throws Exception {
		Dictionary<String, Object> properties = new Hashtable<>();
		properties.put("osgi.cdi.extension", "aries.cdi.jndi");
		_serviceRegistration = context.registerService(Extension.class, new JndiExtensionFactory(), properties);
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		_serviceRegistration.unregister();
	}

	private ServiceRegistration<Extension> _serviceRegistration;

}
