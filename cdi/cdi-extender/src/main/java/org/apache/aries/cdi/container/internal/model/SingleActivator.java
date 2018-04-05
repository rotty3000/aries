package org.apache.aries.cdi.container.internal.model;

import java.lang.annotation.Annotation;
import java.util.AbstractMap;
import java.util.Arrays;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.enterprise.context.BeforeDestroyed;
import javax.enterprise.context.Destroyed;
import javax.enterprise.context.Initialized;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.Bean;

import org.apache.aries.cdi.container.internal.bean.ReferenceBean;
import org.apache.aries.cdi.container.internal.container.ComponentContext.With;
import org.apache.aries.cdi.container.internal.container.ContainerState;
import org.apache.aries.cdi.container.internal.container.Op;
import org.apache.aries.cdi.container.internal.container.Op.Mode;
import org.apache.aries.cdi.container.internal.util.Logs;
import org.apache.aries.cdi.container.internal.util.Maps;
import org.apache.aries.cdi.container.internal.util.SRs;
import org.apache.aries.cdi.container.internal.util.Sets;
import org.apache.aries.cdi.container.internal.util.Syncro;
import org.apache.aries.cdi.container.internal.util.Throw;
import org.osgi.framework.Bundle;
import org.osgi.framework.PrototypeServiceFactory;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cdi.ServiceScope;
import org.osgi.service.cdi.annotations.ComponentScoped;
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
		try (Syncro synchro = _lock.open()) {
			if (serviceRegistration != null) {
				_log.debug(l -> l.debug("CCR Unregistering service {} on {}", serviceRegistration.getReference(), bundle()));

				serviceRegistration.unregister();
			}

			instance.activations.removeIf(
				a -> {
					ExtendedActivationDTO extended = (ExtendedActivationDTO)a;
					extended.onClose.accept(extended);
					return true;
				}
			);

			instance.active = false;

			return true;
		}
	}

	@Override
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public boolean open() {
		try (Syncro synchro = _lock.open()) {
			if (!instance.referencesResolved() || instance.active) {
				return false;
			}

			ExtendedActivationTemplateDTO activationTemplate =
				(ExtendedActivationTemplateDTO)instance.template.activations.get(0);

			instance.template.references.stream().map(ExtendedReferenceTemplateDTO.class::cast).forEach(
				t -> {
					instance.references.stream().filter(
						r -> r.template == t
					).findFirst().map(
						ExtendedReferenceDTO.class::cast
					).ifPresent(
						r -> {
							ReferenceBean bean = t.bean;
							bean.setBeanManager(instance.beanManager);
							bean.setReferenceDTO(r);
						}
					);
				}
			);
			instance.template.configurations.stream().map(ExtendedConfigurationTemplateDTO.class::cast).filter(
				t -> Objects.nonNull(t.injectionPointType)
			).forEach(
				t -> {
					t.bean.setProperties(instance.properties);
				}
			);

			ExtendedComponentTemplateDTO extended = (ExtendedComponentTemplateDTO)instance.template;

			Set<Bean<?>> beans = instance.beanManager.getBeans(
				extended.bean.getBeanClass(), extended.bean.getQualifiers().toArray(new Annotation[0]));
			Bean<? extends Object> bean = instance.beanManager.resolve(beans);

			if (activationTemplate.serviceClasses.isEmpty() /* immediate */) {
				activate(bean, activationTemplate);
			}
			else if (activationTemplate.scope == ServiceScope.SINGLETON) {
				Entry<ExtendedActivationDTO, Object> entry = activate(
					bean, activationTemplate);
				serviceRegistration = containerState.bundleContext().registerService(
					activationTemplate.serviceClasses.toArray(new String[0]),
					entry.getValue(),
					Maps.dict(instance.properties));
				entry.getKey().service = SRs.from(serviceRegistration.getReference());
			}
			else if (activationTemplate.scope == ServiceScope.BUNDLE) {
				serviceRegistration = containerState.bundleContext().registerService(
					activationTemplate.serviceClasses.toArray(new String[0]),
					new ServiceFactory() {

						@Override
						public Object getService(Bundle bundle, ServiceRegistration registration) {
							Entry<ExtendedActivationDTO, Object> entry = activate(
								bean, activationTemplate);
							entry.getKey().service = SRs.from(registration.getReference());
							_locals.put(entry.getValue(), entry.getKey());
							return entry.getValue();
						}

						@Override
						public void ungetService(Bundle bundle, ServiceRegistration registration, Object object) {
							ExtendedActivationDTO activationDTO = _locals.remove(object);

							if (activationDTO != null) {
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
							Entry<ExtendedActivationDTO, Object> entry = activate(
								bean, activationTemplate);
							entry.getKey().service = SRs.from(registration.getReference());
							_locals.put(entry.getValue(), entry.getKey());
							return entry.getValue();
						}

						@Override
						public void ungetService(Bundle bundle, ServiceRegistration registration, Object object) {
							ExtendedActivationDTO activationDTO = _locals.remove(object);

							if (activationDTO != null) {
								activationDTO.onClose.accept(activationDTO);
							}
						}

						final Map<Object, ExtendedActivationDTO> _locals = new ConcurrentHashMap<>();

					},
					Maps.dict(instance.properties)
				);
			}

			instance.active = true;

			return true;
		}
	}

	@Override
	public Op closeOp() {
		return Op.of(Mode.CLOSE, Op.Type.SINGLE_INSTANCE, instance.template.name);
	}

	@Override
	public Op openOp() {
		return Op.of(Mode.OPEN, Op.Type.SINGLE_INSTANCE, instance.template.name);
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	Entry<ExtendedActivationDTO, Object> activate(
		Bean<? extends Object> bean,
		ExtendedActivationTemplateDTO activationTemplate) {

		ExtendedActivationDTO activationDTO = new ExtendedActivationDTO();
		activationDTO.errors = new CopyOnWriteArrayList<>();
		activationDTO.template = activationTemplate;
		instance.activations.add(activationDTO);

		try (With with = new With(activationDTO)) {
			try {
				final Object object = containerState.componentContext().get(
					(Bean)bean,
					(CreationalContext)instance.beanManager.createCreationalContext(bean));
				final Set<Annotation> qualifiers = bean.getQualifiers();
				instance.beanManager.fireEvent(object, Sets.hashSet(qualifiers, Initialized.Literal.of(ComponentScoped.class)).toArray(new Annotation[0]));
				activationDTO.onClose = a -> {
					try (With with2 = new With(a)) {
						instance.beanManager.fireEvent(object, Sets.hashSet(qualifiers, BeforeDestroyed.Literal.of(ComponentScoped.class)).toArray(new Annotation[0]));
						containerState.componentContext().destroy();
						instance.beanManager.fireEvent(object, Sets.hashSet(qualifiers, Destroyed.Literal.of(ComponentScoped.class)).toArray(new Annotation[0]));
						instance.activations.remove(a);
					}
				};
				return new AbstractMap.SimpleImmutableEntry<>(activationDTO, object);
			}
			catch (Throwable t) {
				_log.error(l -> l.error("CCR Error single activator create for {} on {}", instance, bundle(), t));
				activationDTO.errors.add(Throw.asString(t));
				return new AbstractMap.SimpleImmutableEntry<>(activationDTO, null);
			}
		}
	}

	@Override
	public String toString() {
		return Arrays.asList(getClass().getSimpleName(), instance.ident()).toString();
	}

	private static final Logger _log = Logs.getLogger(SingleActivator.class);

	private volatile ServiceRegistration<?> serviceRegistration;
	private final Syncro _lock = new Syncro(true);

}
