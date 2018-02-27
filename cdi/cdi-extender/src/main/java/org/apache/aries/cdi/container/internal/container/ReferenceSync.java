package org.apache.aries.cdi.container.internal.container;

import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.aries.cdi.container.internal.model.CollectionType;
import org.apache.aries.cdi.container.internal.model.ExtendedComponentInstanceDTO;
import org.apache.aries.cdi.container.internal.model.ExtendedReferenceDTO;
import org.apache.aries.cdi.container.internal.model.ExtendedReferenceTemplateDTO;
import org.apache.aries.cdi.container.internal.model.ReferenceEventImpl;
import org.apache.aries.cdi.container.internal.util.DTOs;
import org.apache.aries.cdi.container.internal.util.Maps;
import org.osgi.framework.ServiceObjects;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cdi.reference.ReferenceServiceObjects;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

public class ReferenceSync implements ServiceTrackerCustomizer<Object, Object> {

	public ReferenceSync(
		ExtendedReferenceDTO referenceDTO,
		ExtendedComponentInstanceDTO componentInstanceDTO) {

		_referenceDTO = referenceDTO;
		_componentInstanceDTO = componentInstanceDTO;
		_containerState = _componentInstanceDTO.containerState;
		_template = (ExtendedReferenceTemplateDTO)_referenceDTO.template;
	}

	@Override
	public Object addingService(final ServiceReference<Object> reference) {
		_referenceDTO.references.add(reference);
		_referenceDTO.matches = DTOs.from(_referenceDTO.references);

		try {
			if (_template.collectionType == CollectionType.OBSERVER) {
				return new ReferenceEventImpl<>(reference, _containerState.bundleContext());
			}
			else if (_template.collectionType == CollectionType.PROPERTIES) {
				return Maps.of(reference.getProperties());
			}
			else if (_template.collectionType == CollectionType.REFERENCE) {
				return reference;
			}
			else if (_template.collectionType == CollectionType.SERVICEOBJECTS) {
				return new InternalReferenceServiceObjects(
					_containerState.bundleContext().getServiceObjects(reference));
			}
			else if (_template.collectionType == CollectionType.TUPLE) {
				return new SimpleImmutableEntry<>(
					Maps.of(reference.getProperties()),
					_containerState.bundleContext().getService(reference));
			}

			return _containerState.bundleContext().getService(reference);
		}
		finally {
			_componentInstanceDTO.activate(reference);
		}
	}

	@Override
	public void modifiedService(ServiceReference<Object> reference, Object service) {
		if (_template.collectionType == CollectionType.OBSERVER) {
			((ReferenceEventImpl<?>)service).modifiedService();

			return;
		}

		// TODO check this
		//removedService(reference, service);
		//addingService(reference);
	}

	@Override
	public void removedService(ServiceReference<Object> reference, Object service) {
		_referenceDTO.references.remove(reference);
		_referenceDTO.matches = DTOs.from(_referenceDTO.references);
		_componentInstanceDTO.deactivate(reference);

		try {
			if (_template.collectionType == CollectionType.OBSERVER) {
				((ReferenceEventImpl<?>)service).removedService();

				return;
			}
			else if (_template.collectionType == CollectionType.PROPERTIES) {
				return;
			}
			else if (_template.collectionType == CollectionType.REFERENCE) {
				return;
			}
			else if (_template.collectionType == CollectionType.SERVICEOBJECTS) {
				((InternalReferenceServiceObjects)service).close();

				return;
			}

			_containerState.bundleContext().ungetService(reference);
		}
		finally {
			_componentInstanceDTO.activate(reference);
		}
	}

	private final ExtendedComponentInstanceDTO _componentInstanceDTO;
	private final ContainerState _containerState;
	private final ExtendedReferenceDTO _referenceDTO;
	private final ExtendedReferenceTemplateDTO _template;

	private class InternalReferenceServiceObjects implements ReferenceServiceObjects<Object> {
		public InternalReferenceServiceObjects(ServiceObjects<Object> so) {
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
		public Object getService() {
			Object service = _so.getService();
			_objects.add(service);
			return service;
		}

		@Override
		public ServiceReference<Object> getServiceReference() {
			return _so.getServiceReference();
		}

		@Override
		public void ungetService(Object service) {
			_objects.remove(service);
			_so.ungetService(service);
		}

		private final Set<Object> _objects = ConcurrentHashMap.newKeySet();
		private final ServiceObjects<Object> _so;
	}

}
