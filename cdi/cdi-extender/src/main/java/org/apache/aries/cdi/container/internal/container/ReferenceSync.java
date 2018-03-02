package org.apache.aries.cdi.container.internal.container;

import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Arrays;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.apache.aries.cdi.container.internal.model.CollectionType;
import org.apache.aries.cdi.container.internal.model.ExtendedComponentInstanceDTO;
import org.apache.aries.cdi.container.internal.model.ExtendedReferenceDTO;
import org.apache.aries.cdi.container.internal.model.ExtendedReferenceTemplateDTO;
import org.apache.aries.cdi.container.internal.model.InstanceActivator;
import org.apache.aries.cdi.container.internal.model.ReferenceEventImpl;
import org.apache.aries.cdi.container.internal.util.Conversions;
import org.apache.aries.cdi.container.internal.util.DTOs;
import org.apache.aries.cdi.container.internal.util.Logs;
import org.apache.aries.cdi.container.internal.util.Maps;
import org.osgi.framework.ServiceObjects;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cdi.MaximumCardinality;
import org.osgi.service.cdi.ReferencePolicy;
import org.osgi.service.cdi.ReferencePolicyOption;
import org.osgi.service.cdi.reference.ReferenceServiceObjects;
import org.osgi.service.cdi.runtime.dto.template.ReferenceTemplateDTO;
import org.osgi.service.log.Logger;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

public class ReferenceSync implements ServiceTrackerCustomizer<Object, Object> {

	public ReferenceSync(
		ExtendedReferenceDTO referenceDTO,
		ExtendedComponentInstanceDTO componentInstanceDTO,
		InstanceActivator.Builder<?> builder) {

		_referenceDTO = referenceDTO;
		_componentInstanceDTO = componentInstanceDTO;
		_containerState = _componentInstanceDTO.containerState;
		_builder = builder;
		_template = (ExtendedReferenceTemplateDTO)_referenceDTO.template;
	}

	@Override
	public Object addingService(final ServiceReference<Object> reference) {
		boolean resolved = (_referenceDTO.matches.size() >= _template.minimumCardinality);
		boolean dynamic = (_template.policy == ReferencePolicy.DYNAMIC);
		boolean reluctant = (_template.policyOption == ReferencePolicyOption.RELUCTANT);
		boolean active = _componentInstanceDTO.active;
		boolean unary = _template.maximumCardinality == MaximumCardinality.ONE;
		CollectionType collectionType = _template.collectionType;
		boolean requiresUpdate = true;

		if (resolved && reluctant && active) {
			requiresUpdate = false;
		}

		_referenceDTO.matches = DTOs.from(_referenceDTO.serviceTracker.getServiceReferences(), reference);

		try {
			if (collectionType == CollectionType.OBSERVER) {
				return new ReferenceEventImpl<>(reference, _containerState.bundleContext());
			}
			else if (collectionType == CollectionType.PROPERTIES) {
				return Maps.of(reference.getProperties());
			}
			else if (collectionType == CollectionType.REFERENCE) {
				return reference;
			}
			else if (collectionType == CollectionType.SERVICEOBJECTS) {
				return new InternalReferenceServiceObjects(
					_containerState.bundleContext().getServiceObjects(reference));
			}
			else if (collectionType == CollectionType.TUPLE) {
				return new SimpleImmutableEntry<>(
					Maps.of(reference.getProperties()),
					_containerState.bundleContext().getService(reference));
			}

			return _containerState.bundleContext().getService(reference);
		}
		finally {
			if (requiresUpdate) {
				InstanceActivator activator = _builder.setInstance(
					_componentInstanceDTO
				).setReferenceDTO(
					_referenceDTO
				).setReference(
					reference
				).build();

				if (dynamic) {
					updateDymanically(activator);
				}
				else {
					updateStatically(activator);
				}
			}
		}
	}

	private void updateStatically(InstanceActivator activator) {
		_containerState.submit(
			activator.closeOp(), activator::close
		).then(
			s -> _containerState.submit(
				activator.openOp(), activator::open
			).then(
				null,
				f -> {
					_log.error(l -> l.error("CCR Error during activate on {}", _componentInstanceDTO, f.getFailure()));

					// TODO do we have to hold the error in component instance?
				}
			)
		).then(
			null,
			f -> {
				_log.error(l -> l.error("CCR Error during deactivate on {}", _componentInstanceDTO, f.getFailure()));

				// TODO do we have to hold the error in component instance?
			}
		);
	}

	private void updateDymanically(InstanceActivator activator) {
		_containerState.submit(
			activator.openOp(), activator::open
		).then(
			null,
			f -> {
				_log.error(l -> l.error("CCR Error during activate on {}", _componentInstanceDTO, f.getFailure()));

				// TODO do we have to hold the error in component instance?
			}
		);
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
		boolean resolved = (_referenceDTO.matches.size() >= _template.minimumCardinality);
		boolean dynamic = (_template.policy == ReferencePolicy.DYNAMIC);
		boolean reluctant = (_template.policyOption == ReferencePolicyOption.RELUCTANT);
		boolean active = _componentInstanceDTO.active;
		boolean unary = _template.maximumCardinality == MaximumCardinality.ONE;
		CollectionType collectionType = _template.collectionType;
		boolean requiresUpdate = false;

		if (resolved && reluctant && active) {
			requiresUpdate = true;
		}

		_referenceDTO.matches = DTOs.from(
			Arrays.stream(_referenceDTO.serviceTracker.getServiceReferences()).filter(
				r -> !r.equals(reference)
			).collect(Collectors.toList())
		);

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
			if (requiresUpdate) {
				InstanceActivator activator = _builder.setInstance(
					_componentInstanceDTO
				).setReferenceDTO(
					_referenceDTO
				).setReference(
					reference
				).build();

				if (dynamic) {
					updateDymanically(activator);
				}
				else {
					updateStatically(activator);
				}
			}
		}
	}

	@Override
	public String toString() {
		if (_string == null) {
			_string = Conversions.convert(_referenceDTO.template).to(ReferenceTemplateDTO.class).toString();
		}
		return _string;
	}

	private static final Logger _log = Logs.getLogger(ReferenceSync.class);

	private final InstanceActivator.Builder<?> _builder;
	private final ExtendedComponentInstanceDTO _componentInstanceDTO;
	private final ContainerState _containerState;
	private final ExtendedReferenceDTO _referenceDTO;
	private volatile String _string;
	private final ExtendedReferenceTemplateDTO _template;

	private static class InternalReferenceServiceObjects implements ReferenceServiceObjects<Object> {
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
