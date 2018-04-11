package org.apache.aries.cdi.extension.jndi;

import java.util.Dictionary;
import java.util.Hashtable;

import javax.enterprise.inject.spi.Extension;
import javax.naming.spi.ObjectFactory;

import org.osgi.annotation.bundle.Capability;
import org.osgi.annotation.bundle.Header;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.osgi.namespace.service.ServiceNamespace;
import org.osgi.service.cdi.CDIConstants;
import org.osgi.service.cdi.annotations.RequireCDIImplementation;
import org.osgi.service.jndi.JNDIConstants;
import org.osgi.service.log.LoggerFactory;
import org.osgi.util.tracker.ServiceTracker;

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
		_lft = new ServiceTracker<>(context, LoggerFactory.class, null);
		_lft.open();

		Dictionary<String, Object> properties = new Hashtable<>();
		properties.put(CDIConstants.CDI_EXTENSION_PROPERTY, "aries.cdi.jndi");
		properties.put(JNDIConstants.JNDI_URLSCHEME, "java");

		_serviceRegistration = context.registerService(
			new String[] {Extension.class.getName(), ObjectFactory.class.getName()},
			new JndiExtensionFactory(_lft.getService()), properties);
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		_serviceRegistration.unregister();
	}

	private volatile ServiceTracker<LoggerFactory, LoggerFactory> _lft;
	@SuppressWarnings("rawtypes")
	private ServiceRegistration _serviceRegistration;

}