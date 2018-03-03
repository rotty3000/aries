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
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.enterprise.context.spi.Context;
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
import javax.enterprise.inject.spi.BeforeShutdown;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.inject.spi.ProcessInjectionPoint;

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
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cdi.ComponentType;
import org.osgi.service.cdi.annotations.Configuration;
import org.osgi.service.cdi.annotations.Reference;
import org.osgi.service.cdi.runtime.dto.ActivationDTO;
import org.osgi.service.cdi.runtime.dto.template.ConfigurationTemplateDTO;
import org.osgi.service.cdi.runtime.dto.template.ReferenceTemplateDTO;

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
			comp -> {
				comp.references.stream().map(
					r -> (ExtendedReferenceDTO)r
				).forEach(
					r -> {
						ExtendedReferenceTemplateDTO template = (ExtendedReferenceTemplateDTO)r.template;
						template.bean.setBeanManager(bm);
						template.bean.setSnapshot(r);
						abd.addBean(template.bean);
					}
				);
				comp.configurations.stream().map(
					c -> (ExtendedConfigurationDTO)c
				).forEach(
					c -> {
						ExtendedConfigurationTemplateDTO template = (ExtendedConfigurationTemplateDTO)c.template;
						if (template.injectionPointType != null) {
							if (template.pid == null) {
								template.bean.setProperties(comp.properties);
							}
							else {
								template.bean.setProperties(c.properties);
							}
							abd.addBean(template.bean);
						}
					}
				);
			}
		);
	}

	void afterDeploymentValidation(@Observes AfterDeploymentValidation adv, BeanManager bm) {
		// TODO create & publish service activations
		_containerState.containerDTO().components.stream().filter(
			c -> c.template.type == ComponentType.CONTAINER
		).findFirst().ifPresent(
			c -> {
				ExtendedComponentInstanceDTO instance = (ExtendedComponentInstanceDTO)c.instances.get(0);
				c.template.activations.stream().map(
					a -> (ExtendedActivationTemplateDTO)a
				).forEach(
					a -> {
						Context context = bm.getContext(a.cdiScope);
						Set<Bean<?>> beans = bm.getBeans(a.declaringClass, Any.Literal.INSTANCE);
						Bean<?> bean = bm.resolve(beans);
						Object object = context.get(bean);

						ServiceRegistration<?> serviceRegistration = _containerState.bundleContext().registerService(
							a.serviceClasses.toArray(new String[0]),
							object,
							Maps.dict(instance.properties));

						ActivationDTO activationDTO = new ActivationDTO();
						activationDTO.errors = new CopyOnWriteArrayList<>();
						activationDTO.service = SRs.from(serviceRegistration.getReference());
						activationDTO.template = a;
						instance.activations.add(activationDTO);

						_registrations.add(serviceRegistration);
					}
				);
			}
		);
	}

	void beforeShutdown(@Observes BeforeShutdown bs) {
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

	boolean matchConfiguration(OSGiBean osgiBean, Configuration configuration, ProcessInjectionPoint<?, ?> pip) {
		InjectionPoint injectionPoint = pip.getInjectionPoint();

		Class<?> declaringClass = DiscoveryExtension.getDeclaringClass(injectionPoint);

		ConfigurationTemplateDTO current = new ConfigurationModel.Builder(injectionPoint.getType()).declaringClass(
			declaringClass
		).injectionPoint(
			injectionPoint
		).build().toDTO();

		for (ConfigurationTemplateDTO t : osgiBean.getComponent().configurations) {
			ExtendedConfigurationTemplateDTO template = (ExtendedConfigurationTemplateDTO)t;

			if (current.equals(template)) {
				MarkedInjectionPoint markedInjectionPoint = new MarkedInjectionPoint(injectionPoint);

				pip.setInjectionPoint(markedInjectionPoint);

				template.bean.setMark(markedInjectionPoint.getMark());

				return true;
			}
		}

		return false;
	}

	boolean matchReference(OSGiBean osgiBean, Reference reference, ProcessInjectionPoint<?, ?> pip) {
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

		for (ReferenceTemplateDTO t : osgiBean.getComponent().references) {
			ExtendedReferenceTemplateDTO template = (ExtendedReferenceTemplateDTO)t;

			if (current.equals(template)) {
				MarkedInjectionPoint markedInjectionPoint = new MarkedInjectionPoint(injectionPoint);

				pip.setInjectionPoint(markedInjectionPoint);

				template.bean.setMark(markedInjectionPoint.getMark());

				return true;
			}
		}

		return false;
	}

	/*
	ReferenceModel matchReference(ComponentModel componentModel, ProcessObserverMethod<ServiceEvent<?>, ?> pom) {
		ObserverMethod<ServiceEvent<?>> observerMethod = pom.getObserverMethod();

		Annotated annotated = new ObserverMethodAnnotated(observerMethod);

		for (ReferenceModel referenceModel : componentModel.getReferences()) {
			ReferenceModel tempModel = new ReferenceModel.Builder(
				observerMethod.getObservedQualifiers()
			).annotated(
				annotated
			).policy(
				ReferencePolicy.DYNAMIC
			).build();

			if (referenceModel.equals(tempModel)) {
				return referenceModel;
			}
		}

		return null;
	}


	 * discover if an annotated class is a component

	<X> void processAnnotatedType(@Observes ProcessAnnotatedType<X> pat, BeanManager beanManager) {
		final AnnotatedType<X> at = pat.getAnnotatedType();

		Class<X> annotatedClass = at.getJavaClass();

		ComponentModel componentModel = _containerState.beansModel().getComponentModel(annotatedClass.getName());

		// Is it one of the CDI Bundle's defined beans?

		if (componentModel == null) {

			// No it's not!

			return;
		}

		// If the class is already annotated with @Component, skip it!

		if (at.isAnnotationPresent(Component.class)) {
			return;
		}

		// Since it's not, add @Component to the metadata for completeness.

		AnnotatedType<X> wrapped = new AnnotatedType<X>() {

			// Create an impl of @Component

			private final ComponentLiteral componentLiteral = new ComponentLiteral(
				componentModel.getName(),
				Types.types(componentModel, annotatedClass, _containerState.classLoader()),
				componentModel.getProperties(),
				componentModel.getServiceScope());

			@Override
			public Set<AnnotatedConstructor<X>> getConstructors() {
				return at.getConstructors();
			}

			@Override
			public Set<AnnotatedField<? super X>> getFields() {
				return at.getFields();
			}

			@Override
			public Class<X> getJavaClass() {
				return at.getJavaClass();
			}

			@Override
			public Set<AnnotatedMethod<? super X>> getMethods() {
				return at.getMethods();
			}

			@Override
			@SuppressWarnings("unchecked")
			public <T extends Annotation> T getAnnotation(final Class<T> annType) {
				if (Component.class.equals(annType)) {
					return (T)componentLiteral;
				}
				return at.getAnnotation(annType);
			}

			@Override
			public Set<Annotation> getAnnotations() {
				return Sets.hashSet(at.getAnnotations(), componentLiteral);
			}

			@Override
			public Type getBaseType() {
				return at.getBaseType();
			}

			@Override
			public Set<Type> getTypeClosure() {
				return at.getTypeClosure();
			}

			@Override
			public boolean isAnnotationPresent(Class<? extends Annotation> annType) {
				if (Component.class.equals(annType)) {
					return true;
				}
				return at.isAnnotationPresent(annType);
			}

		};

		pat.setAnnotatedType(wrapped);
	}
*/

//	void processBean(@Observes ProcessBean<?> pb, BeanManager beanManager) {
//		Entry<Class<?>, Annotated> beanClassAndAnnotated = DiscoveryExtension.getBeanClassAndAnnotated(pb);
//
//		final Class<?> annotatedClass = beanClassAndAnnotated.getKey();
//
//		String className = annotatedClass.getName();
//
//		OSGiBean osgiBean = _containerState.beansModel().getOSGiBean(className);
//
//		if (osgiBean == null) {
//			return;
//		}
//
//		Annotated annotated = beanClassAndAnnotated.getValue();
//	}

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

	/*
	void processObserverMethod(@Observes ProcessObserverMethod<ServiceEvent<?>, ?> pom) {
		ObserverMethod<ServiceEvent<?>> observerMethod = pom.getObserverMethod();

		if (_log.isDebugEnabled()) {
			_log.debug("CCR Processing observer method {}", observerMethod);
		}

		Class<?> beanClass = observerMethod.getBeanClass();

		final String className = beanClass.getName();

		ComponentModel componentModel = _containerState.beansModel().getComponentModel(className);

		if (componentModel == null) {
			return;
		}

		ReferenceModel matchingReference = matchReference(componentModel, pom);

		if (matchingReference != null) {
			Map<String, ObserverMethod<ServiceEvent<?>>> map = _containerState.referenceObservers().computeIfAbsent(
				componentModel, k -> new LinkedHashMap<>());

			map.put(matchingReference.getName(), observerMethod);
		}
	}
	 */

	private final ContainerState _containerState;
	private final List<ServiceRegistration<?>> _registrations = new CopyOnWriteArrayList<>();

}
