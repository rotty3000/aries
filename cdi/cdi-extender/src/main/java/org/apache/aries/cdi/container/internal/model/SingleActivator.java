package org.apache.aries.cdi.container.internal.model;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.enterprise.context.BeforeDestroyed;
import javax.enterprise.context.Destroyed;
import javax.enterprise.context.Initialized;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.Bean;

import org.apache.aries.cdi.container.internal.container.ComponentContext.With;
import org.apache.aries.cdi.container.internal.container.ContainerState;
import org.apache.aries.cdi.container.internal.container.Op;
import org.apache.aries.cdi.container.internal.container.Op.Mode;
import org.apache.aries.cdi.container.internal.util.Logs;
import org.apache.aries.cdi.container.internal.util.Maps;
import org.apache.aries.cdi.container.internal.util.SRs;
import org.apache.aries.cdi.container.internal.util.Throw;
import org.osgi.framework.Bundle;
import org.osgi.framework.PrototypeServiceFactory;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cdi.ServiceScope;
import org.osgi.service.cdi.annotations.ComponentScoped;
import org.osgi.service.cdi.runtime.dto.template.ActivationTemplateDTO;
import org.osgi.service.log.Logger;

public class SingleActivator extends InstanceActivator {

	public static class Builder extends InstanceActivator.Builder<Builder> {

		public Builder(ContainerState containerState) {
			super(containerState, null);
		}

		@Override
		public SingleActivator build() {
			return new SingleActivator(this);
		}

	}

	private SingleActivator(Builder builder) {
		super(builder);
	}

	@Override
	public boolean close() {
		if (serviceRegistration != null) {
			serviceRegistration.unregister();
		}

		instance.activations.removeIf(
			a -> {
				ExtendedActivationDTO extended = (ExtendedActivationDTO)a;
				extended.onClose.accept(extended);
				return true;
			}
		);

		return true;
	}

	@Override
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public boolean open() {
		if (!instance.referencesResolved()) {
			return false;
		}

		ActivationTemplateDTO activationTemplate = instance.template.activations.get(0);
		ExtendedComponentTemplateDTO extended = (ExtendedComponentTemplateDTO)instance.template;

		if (activationTemplate.serviceClasses.isEmpty() /* immediate */) {
			ExtendedActivationDTO activationDTO = new ExtendedActivationDTO();
			try (With with = new With(activationDTO)) {
				activationDTO.errors = new CopyOnWriteArrayList<>();
				activationDTO.template = activationTemplate;
				try {
					Object object = containerState.componentContext().get(
						(Bean)extended.bean,
						(CreationalContext)instance.beanManager.createCreationalContext(extended.bean));
					instance.beanManager.fireEvent(object, Initialized.Literal.of(ComponentScoped.class));
					activationDTO.onClose = a -> {
						instance.beanManager.fireEvent(object, BeforeDestroyed.Literal.of(ComponentScoped.class));
						containerState.componentContext().destroy(a);
						instance.beanManager.fireEvent(instance.properties, Destroyed.Literal.of(ComponentScoped.class));
					};
				}
				catch (Throwable t) {
					_log.error(l -> l.error("CCR Error single activator create for {} on {}", instance, bundle(), t));

					activationDTO.errors.add(Throw.asString(t));
				}
			}
			instance.activations.add(activationDTO);
		}
		else if (activationTemplate.scope == ServiceScope.SINGLETON) {
			ExtendedActivationDTO activationDTO = new ExtendedActivationDTO();
			try (With with = new With(activationDTO)) {
				activationDTO.errors = new CopyOnWriteArrayList<>();
				activationDTO.template = activationTemplate;
				try {
					Object object = containerState.componentContext().get(
						(Bean)extended.bean,
						(CreationalContext)instance.beanManager.createCreationalContext(extended.bean));
					serviceRegistration = containerState.bundleContext().registerService(
						activationTemplate.serviceClasses.toArray(new String[0]),
						object,
						Maps.dict(instance.properties));
					activationDTO.service = SRs.from(serviceRegistration.getReference());
					instance.beanManager.fireEvent(object, Initialized.Literal.of(ComponentScoped.class));
					activationDTO.onClose = a -> {
						instance.beanManager.fireEvent(object, BeforeDestroyed.Literal.of(ComponentScoped.class));
						containerState.componentContext().destroy(a);
						instance.beanManager.fireEvent(instance.properties, Destroyed.Literal.of(ComponentScoped.class));
					};
				}
				catch (Throwable t) {
					_log.error(l -> l.error("CCR Error single activator create for {} on {}", instance, bundle(), t));

					activationDTO.errors.add(Throw.asString(t));
				}
			}
			instance.activations.add(activationDTO);
		}
		else if (activationTemplate.scope == ServiceScope.BUNDLE) {
			serviceRegistration = containerState.bundleContext().registerService(
				activationTemplate.serviceClasses.toArray(new String[0]),
				new ServiceFactory() {

					@Override
					public Object getService(Bundle bundle, ServiceRegistration registration) {
						ExtendedActivationDTO activationDTO = new ExtendedActivationDTO();
						try (With with = new With(activationDTO)) {
							activationDTO.errors = new CopyOnWriteArrayList<>();
							activationDTO.template = activationTemplate;
							try {
								Object object = containerState.componentContext().get(
									(Bean)extended.bean,
									(CreationalContext)instance.beanManager.createCreationalContext(extended.bean));
								activationDTO.service = SRs.from(serviceRegistration.getReference());
								instance.beanManager.fireEvent(object, Initialized.Literal.of(ComponentScoped.class));
								activationDTO.onClose = a -> {
									instance.beanManager.fireEvent(object, BeforeDestroyed.Literal.of(ComponentScoped.class));
									containerState.componentContext().destroy(a);
									instance.beanManager.fireEvent(instance.properties, Destroyed.Literal.of(ComponentScoped.class));
								};
								_locals.put(object, activationDTO);
								return object;
							}
							catch (Throwable t) {
								_log.error(l -> l.error("CCR Error single activator create for {} on {}", instance, bundle(), t));
								activationDTO.errors.add(Throw.asString(t));
								return null;
							}
						}
					}

					@Override
					public void ungetService(Bundle bundle, ServiceRegistration registration, Object object) {
						ExtendedActivationDTO activationDTO = _locals.remove(object);

						if (activationDTO != null) {
							instance.activations.remove(activationDTO);
							activationDTO.onClose.accept(activationDTO);
						}
					}

					final Map<Object, ExtendedActivationDTO> _locals = new ConcurrentHashMap<>();

				},
				Maps.dict(instance.properties)
			);
		}
		else if (activationTemplate.scope == ServiceScope.PROTOTYPE) {
			serviceRegistration = containerState.bundleContext().registerService(
				activationTemplate.serviceClasses.toArray(new String[0]),
				new PrototypeServiceFactory() {

					@Override
					public Object getService(Bundle bundle, ServiceRegistration registration) {
						ExtendedActivationDTO activationDTO = new ExtendedActivationDTO();
						try (With with = new With(activationDTO)) {
							activationDTO.errors = new CopyOnWriteArrayList<>();
							activationDTO.template = activationTemplate;
							try {
								Object object = containerState.componentContext().get(
									(Bean)extended.bean,
									(CreationalContext)instance.beanManager.createCreationalContext(extended.bean));
								activationDTO.service = SRs.from(serviceRegistration.getReference());
								instance.beanManager.fireEvent(object, Initialized.Literal.of(ComponentScoped.class));
								activationDTO.onClose = a -> {
									instance.beanManager.fireEvent(object, BeforeDestroyed.Literal.of(ComponentScoped.class));
									containerState.componentContext().destroy(a);
									instance.beanManager.fireEvent(instance.properties, Destroyed.Literal.of(ComponentScoped.class));
								};
								_locals.put(object, activationDTO);
								return object;
							}
							catch (Throwable t) {
								_log.error(l -> l.error("CCR Error single activator create for {} on {}", instance, bundle(), t));
								activationDTO.errors.add(Throw.asString(t));
								return null;
							}
						}
					}

					@Override
					public void ungetService(Bundle bundle, ServiceRegistration registration, Object object) {
						ExtendedActivationDTO activationDTO = _locals.remove(object);

						if (activationDTO != null) {
							instance.activations.remove(activationDTO);
							activationDTO.onClose.accept(activationDTO);
						}
					}

					final Map<Object, ExtendedActivationDTO> _locals = new ConcurrentHashMap<>();

				},
				Maps.dict(instance.properties)
			);
		}

		return true;
	}

	@Override
	public Op closeOp() {
		return Op.of(Mode.CLOSE, Op.Type.SINGLE_INSTANCE, instance.template.name);
	}

	@Override
	public Op openOp() {
		return Op.of(Mode.OPEN, Op.Type.SINGLE_INSTANCE, instance.template.name);
	}

	private static final Logger _log = Logs.getLogger(SingleActivator.class);

	private volatile ServiceRegistration<?> serviceRegistration;

}
