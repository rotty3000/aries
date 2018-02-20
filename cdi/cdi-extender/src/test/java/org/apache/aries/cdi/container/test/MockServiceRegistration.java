package org.apache.aries.cdi.container.test;

import java.util.Dictionary;
import java.util.Enumeration;

import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;

public class MockServiceRegistration<S> implements ServiceRegistration<S> {

	public MockServiceRegistration(MockServiceReference<S> mockServiceReference) {
		_mockServiceReference = mockServiceReference;
	}

	@Override
	public MockServiceReference<S> getReference() {
		return _mockServiceReference;
	}

	@Override
	public void setProperties(Dictionary<String, ?> properties) {
		for (Enumeration<String> enu = properties.keys(); enu.hasMoreElements();) {
			String key = enu.nextElement();
			if (key.equals(Constants.OBJECTCLASS) ||
				key.equals(Constants.SERVICE_BUNDLEID) ||
				key.equals(Constants.SERVICE_ID) ||
				key.equals(Constants.SERVICE_SCOPE)) {
				continue;
			}
			_mockServiceReference.setProperty(key, properties.get(key));
		}
	}

	@Override
	public void unregister() {
	}

	private final MockServiceReference<S> _mockServiceReference;

}
