/**
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.aries.cdi.container.internal.container;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.spi.Context;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.spi.AfterBeanDiscovery;
import javax.enterprise.inject.spi.AfterDeploymentValidation;
import javax.enterprise.inject.spi.Annotated;
import javax.enterprise.inject.spi.AnnotatedField;
import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.AnnotatedParameter;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.BeforeBeanDiscovery;
import javax.enterprise.inject.spi.BeforeShutdown;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.inject.spi.ProcessInjectionPoint;
import javax.enterprise.inject.spi.Producer;
import javax.enterprise.inject.spi.ProducerFactory;

import org.apache.aries.cdi.container.internal.bean.ConfigurationBean;
import org.apache.aries.cdi.container.internal.bean.ReferenceBean;
import org.apache.aries.cdi.container.internal.container.Op.Mode;
import org.apache.aries.cdi.container.internal.container.Op.Type;
import org.apache.aries.cdi.container.internal.model.CollectionType;
import org.apache.aries.cdi.container.internal.model.ConfigurationModel;
import org.apache.aries.cdi.container.internal.model.ExtendedActivationDTO;
import org.apache.aries.cdi.container.internal.model.ExtendedActivationTemplateDTO;
import org.apache.aries.cdi.container.internal.model.ExtendedComponentInstanceDTO;
import org.apache.aries.cdi.container.internal.model.ExtendedComponentTemplateDTO;
import org.apache.aries.cdi.container.internal.model.ExtendedConfigurationTemplateDTO;
import org.apache.aries.cdi.container.internal.model.ExtendedReferenceDTO;
import org.apache.aries.cdi.container.internal.model.ExtendedReferenceTemplateDTO;
import org.apache.aries.cdi.container.internal.model.FactoryComponent;
import org.apache.aries.cdi.container.internal.model.OSGiBean;
import org.apache.aries.cdi.container.internal.model.ReferenceModel;
import org.apache.aries.cdi.container.internal.model.SingleComponent;
import org.apache.aries.cdi.container.internal.util.Conversions;
import org.apache.aries.cdi.container.internal.util.Maps;
import org.apache.aries.cdi.container.internal.util.SRs;
import org.osgi.framework.Bundle;
import org.osgi.framework.PrototypeServiceFactory;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cdi.CDIConstants;
import org.osgi.service.cdi.ComponentType;
import org.osgi.service.cdi.ServiceScope;
import org.osgi.service.cdi.annotations.ComponentScoped;
import org.osgi.service.cdi.annotations.Configuration;
import org.osgi.service.cdi.annotations.Reference;
import org.osgi.service.cdi.runtime.dto.ComponentDTO;
import org.osgi.service.cdi.runtime.dto.template.ComponentTemplateDTO;
import org.osgi.service.cdi.runtime.dto.template.ConfigurationTemplateDTO;
import org.osgi.service.log.Logger;
import org.osgi.util.promise.Promise;

public class RuntimeExtension implements Extension {

	public RuntimeExtension(
		ContainerState containerState,
		ConfigurationListener.Builder configurationBuilder,
		SingleComponent.Builder singleBuilder,
		FactoryComponent.Builder factoryBuilder) {

		_containerState = containerState;

		_log = _containerState.containerLogs().getLogger(getClass());
		_log.debug(l -> l.debug("CCR RuntimeExtension {}", containerState.bundle()));

		_configurationBuilder = configurationBuilder;
		_singleBuilder = singleBuilder;
		_factoryBuilder = factoryBuilder;

		_containerComponentDTO = _containerState.containerDTO().components.stream().filter(
			c -> c.template.type == ComponentType.CONTAINER
		).findFirst().get();

		_containerInstanceDTO = (ExtendedComponentInstanceDTO)_containerComponentDTO.instances.get(0);
	}

	void beforeBeanDiscovery(@Observes BeforeBeanDiscovery bbd) {
		bbd.addQualifier(org.osgi.service.cdi.annotations.Bundle.class);
		bbd.addQualifier(org.osgi.service.cdi.annotations.Configuration.class);
		bbd.addQualifier(org.osgi.service.cdi.annotations.Greedy.class);
		bbd.addQualifier(org.osgi.service.cdi.annotations.PID.class);
		bbd.addQualifier(org.osgi.service.cdi.annotations.Prototype.class);
		bbd.addQualifier(org.osgi.service.cdi.annotations.Reference.class);
		bbd.addQualifier(org.osgi.service.cdi.annotations.Service.class);
		bbd.addScope(ComponentScoped.class, false, false);
		bbd.addStereotype(org.osgi.service.cdi.annotations.FactoryComponent.class);
		bbd.addStereotype(org.osgi.service.cdi.annotations.SingleComponent.class);
	}

	void afterBeanDiscovery(@Observes AfterBeanDiscovery abd, BeanManager bm) {
		abd.addContext(_containerState.componentContext());

		_containerState.containerDTO().template.components.forEach(
			ct -> addBeans(ct, abd, bm)
		);
	}

	void afterDeploymentValidation(@Observes AfterDeploymentValidation adv, BeanManager bm) {
		_log.debug(l -> l.debug("CCR AfterDeploymentValidation on {}", _containerState.bundle()));

		registerService(
			new String[] {BeanManager.class.getName()}, bm,
			Maps.of(CDIConstants.CDI_CONTAINER_ID, _containerState.id()));

		_containerState.submit(
			Op.of(Mode.OPEN, Type.CONTAINER_FIRE_EVENTS, _containerState.id()),
			() -> fireEvents(_containerComponentDTO, _containerInstanceDTO, bm)
		).then(
			s-> {
				return _containerState.submit(
					Op.of(Mode.OPEN, Type.CONTAINER_PUBLISH_SERVICES, _containerState.id()),
					() -> registerServices(_containerComponentDTO, _containerInstanceDTO, bm)
				);
			}
		).then(
			s -> initComponents(bm)
		);
	}

	void beforeShutdown(@Observes BeforeShutdown bs) {
		_log.debug(l -> l.debug("CCR BeforeShutdown on {}", _containerState.bundle()));

		_configurationListeners.removeIf(
			cl -> {
				_containerState.submit(cl.closeOp(), cl::close).onFailure(
					f -> {
						_log.error(l -> l.error("CCR Error while closing configuration listener {} on {}", cl, _containerState.bundle(), f));
					}
				);

				return true;
			}
		);

		_containerInstanceDTO.activations.clear();

		_registrations.removeIf(
			r -> {
				try {
					r.unregister();
				}
				catch (Exception e) {
					_log.error(l -> l.error("CCR Error while unregistring {} on {}", r, _containerState.bundle(), e));
				}
				return true;
			}
		);
	}

	void processInjectionPoint(@Observes ProcessInjectionPoint<?, ?> pip, BeanManager beanManager) {
		InjectionPoint injectionPoint = pip.getInjectionPoint();

		Class<?> declaringClass = DiscoveryExtension.getDeclaringClass(injectionPoint);

		String className = declaringClass.getName();

		OSGiBean osgiBean = _containerState.beansModel().getOSGiBean(className);

		if (osgiBean == null) {
			return;
		}

		Annotated annotated = injectionPoint.getAnnotated();
		Configuration configuration = annotated.getAnnotation(Configuration.class);
		Reference reference = annotated.getAnnotation(Reference.class);

		if ((reference != null) && matchReference(osgiBean, reference, pip)) {
			return;
		}

		if (configuration != null) {
			matchConfiguration(osgiBean, configuration, pip);
		}
	}

	private void addBeans(ComponentTemplateDTO componentTemplate, AfterBeanDiscovery abd, BeanManager bm) {
		componentTemplate.references.stream().map(ExtendedReferenceTemplateDTO.class::cast).forEach(
			t -> {
				ReferenceBean bean = t.bean;
				bean.setBeanManager(bm);
				if (componentTemplate.type == ComponentType.CONTAINER) {
					_containerInstanceDTO.references.stream().filter(
						r -> r.template == t
					).findFirst().map(
						ExtendedReferenceDTO.class::cast
					).ifPresent(
						r -> bean.setReferenceDTO(r)
					);
				}
				if (t.collectionType != CollectionType.OBSERVER) {
					_log.debug(l -> l.debug("CCR Adding synthetic bean {} on {}", bean, _containerState.bundle()));

					abd.addBean(bean);
				}
			}
		);

		componentTemplate.configurations.stream().map(ExtendedConfigurationTemplateDTO.class::cast).filter(
			t -> Objects.nonNull(t.injectionPointType)
		).forEach(
			t -> {
				ConfigurationBean bean = t.bean;
				if (componentTemplate.type == ComponentType.CONTAINER) {
					if (t.pid == null) {
						bean.setProperties(componentTemplate.properties);
					}
					else {
						bean.setProperties(_containerInstanceDTO.properties);
					}
				}

				_log.debug(l -> l.debug("CCR Adding synthetic bean {} on {}", bean, _containerState.bundle()));

				abd.addBean(bean);
			}
		);
	}

	@SuppressWarnings("unchecked")
	private Producer<Object> createProducer(Object producerObject, Bean<Object> bean, BeanManager bm) {
		ProducerFactory<Object> producerFactory = null;
		if (producerObject instanceof AnnotatedField)
			producerFactory = bm.getProducerFactory((AnnotatedField<Object>)producerObject, bean);
		else if (producerObject instanceof AnnotatedMethod)
			producerFactory = bm.getProducerFactory((AnnotatedMethod<Object>)producerObject, bean);

		if (producerFactory == null)
			return null;

		return producerFactory.createProducer(bean);
	}

	private boolean fireEvents(ComponentDTO componentDTO, ExtendedComponentInstanceDTO instance, BeanManager bm) {
		// TODO Check the logic of firing all the queued service events.
		instance.references.stream().map(ExtendedReferenceDTO.class::cast).filter(
			r -> ((ExtendedReferenceTemplateDTO)r.template).collectionType == CollectionType.OBSERVER
		).map(
			r -> (ExtendedReferenceTemplateDTO)r.template
		).forEach(
			t -> {
				t.bean.fireEvents();
			}
		);

		return true;
	}

	private Promise<List<Boolean>> initComponents(BeanManager bm) {
		List<Promise<Boolean>> promises = _containerState.containerDTO().template.components.stream().filter(
			t -> t.type != ComponentType.CONTAINER
		).map(ExtendedComponentTemplateDTO.class::cast).map(
			t -> initComponent(t, bm)
		).collect(Collectors.toList());

		return _containerState.promiseFactory().all(promises);
	}

	private Promise<Boolean> initComponent(ExtendedComponentTemplateDTO componentTemplateDTO, BeanManager bm) {
		Boolean enabled = _containerInstanceDTO.configurations.stream().filter(
			c -> c.template.pid.equals(_containerInstanceDTO.template.name)
		).findFirst().map(
			c -> Conversions.convert(
				c.properties.get(componentTemplateDTO.name.concat(".enabled"))
			).defaultValue(Boolean.TRUE).to(Boolean.class)
		).orElse(Boolean.TRUE);

		if (!enabled) {
			_containerState.containerDTO().components.stream().filter(
				c -> c.template == componentTemplateDTO
			).findFirst().ifPresent(
				c -> c.enabled = false
			);

			return _containerState.promiseFactory().resolved(Boolean.TRUE);
		}

		if (componentTemplateDTO.type == ComponentType.FACTORY) {
			return initFactoryComponent(componentTemplateDTO, bm);
		}

		return initSingleComponent(componentTemplateDTO, bm);
	}

	private Promise<Boolean> initFactoryComponent(ExtendedComponentTemplateDTO componentTemplateDTO, BeanManager bm) {
		ConfigurationListener cl = _configurationBuilder.component(
			_factoryBuilder.beanManager(bm).template(componentTemplateDTO).build()
		).build();

		_configurationListeners.add(cl);

		return _containerState.submit(cl.openOp(), cl::open);
	}

	private Promise<Boolean> initSingleComponent(ExtendedComponentTemplateDTO componentTemplateDTO, BeanManager bm) {
		ConfigurationListener cl = _configurationBuilder.component(
			_singleBuilder.beanManager(bm).template(componentTemplateDTO).build()
		).build();

		_configurationListeners.add(cl);

		return _containerState.submit(cl.openOp(), cl::open);
	}

	private boolean matchConfiguration(OSGiBean osgiBean, Configuration configuration, ProcessInjectionPoint<?, ?> pip) {
		InjectionPoint injectionPoint = pip.getInjectionPoint();

		Class<?> declaringClass = DiscoveryExtension.getDeclaringClass(injectionPoint);

		ConfigurationTemplateDTO current = new ConfigurationModel.Builder(injectionPoint.getType()).declaringClass(
			declaringClass
		).injectionPoint(
			injectionPoint
		).build().toDTO();

		return osgiBean.getComponent().configurations.stream().map(
			t -> (ExtendedConfigurationTemplateDTO)t
		).filter(
			t -> current.equals(t)
		).findFirst().map(
			t -> {
				MarkedInjectionPoint markedInjectionPoint = new MarkedInjectionPoint(injectionPoint);

				pip.setInjectionPoint(markedInjectionPoint);

				t.bean.setMark(markedInjectionPoint.getMark());

				return true;
			}
		).orElse(false);
	}

	private boolean matchReference(OSGiBean osgiBean, Reference reference, ProcessInjectionPoint<?, ?> pip) {
		InjectionPoint injectionPoint = pip.getInjectionPoint();

		Annotated annotated = injectionPoint.getAnnotated();

		ReferenceModel.Builder builder = null;

		if (annotated instanceof AnnotatedField) {
			builder = new ReferenceModel.Builder((AnnotatedField<?>)annotated);
		}
		else if (annotated instanceof AnnotatedMethod) {
			builder = new ReferenceModel.Builder((AnnotatedMethod<?>)annotated);
		}
		else {
			builder = new ReferenceModel.Builder((AnnotatedParameter<?>)annotated);
		}

		ReferenceModel referenceModel = builder.injectionPoint(injectionPoint).build();

		ExtendedReferenceTemplateDTO current = referenceModel.toDTO();

		return osgiBean.getComponent().references.stream().map(
			t -> (ExtendedReferenceTemplateDTO)t
		).filter(
			t -> current.equals(t)
		).findFirst().map(
			t -> {
				MarkedInjectionPoint markedInjectionPoint = new MarkedInjectionPoint(injectionPoint);

				pip.setInjectionPoint(markedInjectionPoint);

				t.bean.setMark(markedInjectionPoint.getMark());

				return true;
			}
		).orElse(false);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private void registerService(
		ExtendedComponentInstanceDTO componentInstance,
		ExtendedActivationTemplateDTO activationTemplate,
		BeanManager bm) {

		ServiceScope scope = activationTemplate.scope;

		if (activationTemplate.cdiScope == ApplicationScoped.class) {
			scope = ServiceScope.SINGLETON;
		}

		final Context context = bm.getContext(activationTemplate.cdiScope);
		final Bean<Object> bean = (Bean<Object>)bm.resolve(
			bm.getBeans(activationTemplate.declaringClass, Any.Literal.INSTANCE));
		final Producer producer = createProducer(activationTemplate.producer, bean, bm);

		Object serviceObject;

		if (scope == ServiceScope.PROTOTYPE) {
			serviceObject = new PrototypeServiceFactory<Object>() {
				@Override
				public Object getService(Bundle bundle, ServiceRegistration<Object> registration) {
					CreationalContext<Object> cc = bm.createCreationalContext(bean);
					if (producer != null) {
						return producer.produce(cc);
					}
					return context.get(bean, cc);
				}

				@Override
				public void ungetService(Bundle bundle, ServiceRegistration<Object> registration, Object service) {
				}
			};
		}
		else if (scope == ServiceScope.BUNDLE) {
			serviceObject = new ServiceFactory<Object>() {
				@Override
				public Object getService(Bundle bundle, ServiceRegistration<Object> registration) {
					CreationalContext<Object> cc = bm.createCreationalContext(bean);
					if (producer != null) {
						return producer.produce(cc);
					}
					return context.get(bean, cc);
				}

				@Override
				public void ungetService(Bundle bundle, ServiceRegistration<Object> registration, Object service) {
				}
			};
		}
		else {
			CreationalContext<Object> cc = bm.createCreationalContext(bean);
			if (producer != null) {
				serviceObject = producer.produce(cc);
			}
			else {
				serviceObject = context.get(bean, cc);
			}
		}

		Objects.requireNonNull(serviceObject, "The service object is somehow null on " + this);

		ServiceRegistration<?> serviceRegistration = registerService(
			activationTemplate.serviceClasses.toArray(new String[0]),
			serviceObject,
			componentInstance.componentProperties(activationTemplate.properties));

		ExtendedActivationDTO activationDTO = new ExtendedActivationDTO();
		activationDTO.errors = new CopyOnWriteArrayList<>();
		activationDTO.service = SRs.from(serviceRegistration.getReference());
		activationDTO.template = activationTemplate;
		componentInstance.activations.add(activationDTO);
	}

	private ServiceRegistration<?> registerService(String[] serviceTypes, Object serviceObject, Map<String, Object> properties) {
		ServiceRegistration<?> serviceRegistration = _containerState.bundleContext().registerService(
			serviceTypes, serviceObject, Maps.dict(properties));

		_registrations.add(serviceRegistration);

		return serviceRegistration;
	}

	private boolean registerServices(ComponentDTO componentDTO, ExtendedComponentInstanceDTO instance, BeanManager bm) {
		componentDTO.template.activations.stream().map(
			ExtendedActivationTemplateDTO.class::cast
		).forEach(
			a -> registerService(instance, a, bm)
		);

		return true;
	}

	private final ComponentDTO _containerComponentDTO;
	private final ConfigurationListener.Builder _configurationBuilder;
	private final List<ConfigurationListener> _configurationListeners = new CopyOnWriteArrayList<>();
	private final ContainerState _containerState;
	private final FactoryComponent.Builder _factoryBuilder;
	private final ExtendedComponentInstanceDTO _containerInstanceDTO;
	private final Logger _log;
	private final List<ServiceRegistration<?>> _registrations = new CopyOnWriteArrayList<>();
	private final SingleComponent.Builder _singleBuilder;

}
