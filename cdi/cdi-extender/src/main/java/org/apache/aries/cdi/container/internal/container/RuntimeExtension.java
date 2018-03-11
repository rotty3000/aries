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
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.BeforeDestroyed;
import javax.enterprise.context.Initialized;
import javax.enterprise.context.spi.Context;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.spi.AfterBeanDiscovery;
import javax.enterprise.inject.spi.Annotated;
import javax.enterprise.inject.spi.AnnotatedField;
import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.AnnotatedParameter;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.inject.spi.ProcessInjectionPoint;

import org.apache.aries.cdi.container.internal.model.CollectionType;
import org.apache.aries.cdi.container.internal.model.ConfigurationModel;
import org.apache.aries.cdi.container.internal.model.ExtendedActivationTemplateDTO;
import org.apache.aries.cdi.container.internal.model.ExtendedComponentInstanceDTO;
import org.apache.aries.cdi.container.internal.model.ExtendedConfigurationDTO;
import org.apache.aries.cdi.container.internal.model.ExtendedConfigurationTemplateDTO;
import org.apache.aries.cdi.container.internal.model.ExtendedReferenceDTO;
import org.apache.aries.cdi.container.internal.model.ExtendedReferenceTemplateDTO;
import org.apache.aries.cdi.container.internal.model.OSGiBean;
import org.apache.aries.cdi.container.internal.model.ReferenceModel;
import org.apache.aries.cdi.container.internal.util.Maps;
import org.apache.aries.cdi.container.internal.util.SRs;
import org.osgi.framework.Bundle;
import org.osgi.framework.PrototypeServiceFactory;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cdi.ComponentType;
import org.osgi.service.cdi.ServiceScope;
import org.osgi.service.cdi.annotations.Configuration;
import org.osgi.service.cdi.annotations.Reference;
import org.osgi.service.cdi.runtime.dto.ActivationDTO;
import org.osgi.service.cdi.runtime.dto.ComponentDTO;
import org.osgi.service.cdi.runtime.dto.template.ConfigurationTemplateDTO;

public class RuntimeExtension implements Extension {

	public RuntimeExtension(ContainerState containerState) {
		_containerState = containerState;
	}

	void afterBeanDiscovery(@Observes AfterBeanDiscovery abd, BeanManager bm) {
		_containerState.containerDTO().components.stream().flatMap(
			c -> c.instances.stream()
		).map(
			c -> (ExtendedComponentInstanceDTO)c
		).forEach(
			comp -> addBeans(comp, abd, bm)
		);
	}

	void applicationScopedInitialized(@Observes @Initialized(ApplicationScoped.class) Object o, BeanManager bm) {
		_containerState.containerDTO().components.stream().filter(
			c -> c.template.type == ComponentType.CONTAINER
		).findFirst().ifPresent(
			c -> fireEventsAndRegisterServices(c, (ExtendedComponentInstanceDTO)c.instances.get(0), bm)
		);
	}

	void applicationScopedBeforeDestroyed(@Observes @BeforeDestroyed(ApplicationScoped.class) Object o) {
		_containerState.containerDTO().components.stream().filter(
			c -> c.template.type == ComponentType.CONTAINER
		).findFirst().ifPresent(
			c -> {
				ExtendedComponentInstanceDTO instance = (ExtendedComponentInstanceDTO)c.instances.get(0);

				instance.activations.clear();
			}
		);

		_registrations.removeIf(
			r -> {
				r.unregister();
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

	private void addBeans(ExtendedComponentInstanceDTO comp, AfterBeanDiscovery abd, BeanManager bm) {
		comp.references.stream().map(
			r -> (ExtendedReferenceDTO)r
		).forEach(
			r -> {
				ExtendedReferenceTemplateDTO template = (ExtendedReferenceTemplateDTO)r.template;
				template.bean.prepare(r, bm);
				if (template.collectionType != CollectionType.OBSERVER) {
					abd.addBean(template.bean);
				}
			}
		);
		comp.configurations.stream().map(
			c -> (ExtendedConfigurationDTO)c
		).filter(
			c -> Objects.nonNull(((ExtendedConfigurationTemplateDTO)c.template).injectionPointType)
		).forEach(
			c -> {
				ExtendedConfigurationTemplateDTO template = (ExtendedConfigurationTemplateDTO)c.template;
				if (comp.template.type == ComponentType.CONTAINER) {
					if (template.pid == null) {
						template.bean.setProperties(comp.properties);
					}
					else {
						template.bean.setProperties(c.properties);
					}
				}
				abd.addBean(template.bean);
			}
		);
	}

	private void fireEventsAndRegisterServices(ComponentDTO c, ExtendedComponentInstanceDTO instance, BeanManager bm) {
		instance.references.stream().map(
			r -> (ExtendedReferenceDTO)r
		).filter(
			r -> ((ExtendedReferenceTemplateDTO)r.template).collectionType == CollectionType.OBSERVER
		).map(
			r -> (ExtendedReferenceTemplateDTO)r.template
		).forEach(
			t -> t.bean.fireEvents()
		);

		c.template.activations.stream().map(
			a -> (ExtendedActivationTemplateDTO)a
		).forEach(
			a -> registerServices(instance, a, bm)
		);
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

	private void registerServices(
		ExtendedComponentInstanceDTO componentInstance,
		ExtendedActivationTemplateDTO activationTemplate,
		BeanManager bm) {

		ServiceScope scope = activationTemplate.scope;

		if (activationTemplate.cdiScope == ApplicationScoped.class) {
			scope = ServiceScope.SINGLETON;
		}

		final Context context = bm.getContext(activationTemplate.cdiScope);
		final Bean<?> bean = bm.resolve(bm.getBeans(activationTemplate.declaringClass, Any.Literal.INSTANCE));
		Object serviceObject;

		if (scope == ServiceScope.PROTOTYPE) {
			serviceObject = new PrototypeServiceFactory<Object>() {
				@Override
				public Object getService(Bundle bundle, ServiceRegistration<Object> registration) {
					return context.get(bean);
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
					return context.get(bean);
				}

				@Override
				public void ungetService(Bundle bundle, ServiceRegistration<Object> registration, Object service) {
				}
			};
		}
		else {
			serviceObject = context.get(bean);
		}

		ServiceRegistration<?> serviceRegistration = _containerState.bundleContext().registerService(
			activationTemplate.serviceClasses.toArray(new String[0]),
			serviceObject,
			Maps.dict(componentInstance.properties));

		ActivationDTO activationDTO = new ActivationDTO();
		activationDTO.errors = new CopyOnWriteArrayList<>();
		activationDTO.service = SRs.from(serviceRegistration.getReference());
		activationDTO.template = activationTemplate;
		componentInstance.activations.add(activationDTO);

		_registrations.add(serviceRegistration);
	}

	private final ContainerState _containerState;
	private final List<ServiceRegistration<?>> _registrations = new CopyOnWriteArrayList<>();

}
