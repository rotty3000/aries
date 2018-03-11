package org.apache.aries.cdi.container.internal.container;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.osgi.framework.ServiceObjects;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cdi.reference.ReferenceServiceObjects;

public class ReferenceServiceObjectsImpl<T> implements ReferenceServiceObjects<T> {

	public ReferenceServiceObjectsImpl(ServiceObjects<T> so) {
		_so = so;
	}

	public void close() {
		_objects.removeIf(
			o -> {
				_so.ungetService(o);
				return true;
			}
		);
	}

	@Override
	public T getService() {
		T service = _so.getService();
		_objects.add(service);
		return service;
	}

	@Override
	public ServiceReference<T> getServiceReference() {
		return _so.getServiceReference();
	}

	@Override
	public void ungetService(T service) {
		_objects.remove(service);
		_so.ungetService(service);
	}

	private final Set<T> _objects = ConcurrentHashMap.newKeySet();
	private final ServiceObjects<T> _so;
}