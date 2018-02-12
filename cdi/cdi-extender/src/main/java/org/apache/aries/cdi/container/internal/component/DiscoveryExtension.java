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

package org.apache.aries.cdi.container.internal.component;

import java.lang.reflect.Executable;
import java.lang.reflect.Parameter;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.AfterBeanDiscovery;
import javax.enterprise.inject.spi.Annotated;
import javax.enterprise.inject.spi.AnnotatedField;
import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.AnnotatedParameter;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.DefinitionException;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.inject.spi.ObserverMethod;
import javax.enterprise.inject.spi.ProcessAnnotatedType;
import javax.enterprise.inject.spi.ProcessBean;
import javax.enterprise.inject.spi.ProcessInjectionPoint;
import javax.enterprise.inject.spi.ProcessManagedBean;
import javax.enterprise.inject.spi.ProcessObserverMethod;
import javax.enterprise.inject.spi.ProcessProducerField;
import javax.enterprise.inject.spi.ProcessProducerMethod;
import javax.enterprise.inject.spi.ProcessSessionBean;
import javax.enterprise.inject.spi.ProcessSyntheticBean;

import org.apache.aries.cdi.container.internal.configuration.ConfigurationModel;
import org.apache.aries.cdi.container.internal.container.ContainerState;
import org.apache.aries.cdi.container.internal.model.BeansModel;
import org.apache.aries.cdi.container.internal.model.ExtendedActivationTemplateDTO;
import org.apache.aries.cdi.container.internal.model.ExtendedConfigurationTemplateDTO;
import org.apache.aries.cdi.container.internal.reference.ReferenceModel;
import org.apache.aries.cdi.container.internal.reference.ReferenceModel.Builder;
import org.apache.aries.cdi.container.internal.util.Maps;
import org.apache.aries.cdi.container.internal.util.Types;
import org.osgi.service.cdi.annotations.Bundle;
import org.osgi.service.cdi.annotations.Configuration;
import org.osgi.service.cdi.annotations.FactoryComponent;
import org.osgi.service.cdi.annotations.PID;
import org.osgi.service.cdi.annotations.PID.Policy;
import org.osgi.service.cdi.annotations.Prototype;
import org.osgi.service.cdi.annotations.Reference;
import org.osgi.service.cdi.annotations.SingleComponent;
import org.osgi.service.cdi.reference.ReferenceEvent;
import org.osgi.service.cdi.runtime.dto.template.ActivationTemplateDTO.Scope;
import org.osgi.service.cdi.runtime.dto.template.ComponentTemplateDTO;
import org.osgi.service.cdi.runtime.dto.template.ConfigurationPolicy;
import org.osgi.service.cdi.runtime.dto.template.MaximumCardinality;

public class DiscoveryExtension implements Extension {

	public DiscoveryExtension(ContainerState containerState) {
		_containerState = containerState;
		_beansModel = _containerState.beansModel();
		_containerTemplate = _containerState.containerDTO().template.components.get(0);
	}

	void afterBeanDiscovery(@Observes AfterBeanDiscovery abd) {
		_beansModel.getOSGiBeans().stream().forEach(
			osgiBean -> {
				if (!osgiBean.found()) {
					abd.addDefinitionError(
						new DefinitionException(
							String.format(
								"Did not find bean for <cdi:bean class=\"%s\">",
								osgiBean.getBeanClass())));
				}
			}
		);

		_beansModel.getErrors().stream().forEach(err ->
			abd.addDefinitionError(err)
		);
	}

	<X> void processAnnotatedType(@Observes ProcessAnnotatedType<X> pat, BeanManager beanManager) {
		final AnnotatedType<X> at = pat.getAnnotatedType();

		Class<X> annotatedClass = at.getJavaClass();

		final String className = annotatedClass.getName();

		OSGiBean osgiBean = _beansModel.getOSGiBean(className);

		if (osgiBean == null) {
			return;
		}

		osgiBean.found(true);
	}

	void processBean(@Observes ProcessBean<?> pb) {
		Annotated annotated = null;
		Class<?> annotatedClass = null;

		if (pb instanceof ProcessManagedBean) {
			ProcessManagedBean<?> bean = (ProcessManagedBean<?>)pb;

			annotated = bean.getAnnotated();
			annotatedClass = bean.getAnnotatedBeanClass().getJavaClass();
		}
		else if (pb instanceof ProcessSessionBean) {
			ProcessSessionBean<?> bean = (ProcessSessionBean<?>)pb;

			annotated = bean.getAnnotated();
			annotatedClass = bean.getAnnotatedBeanClass().getJavaClass();
		}
		else if (pb instanceof ProcessProducerMethod) {
			ProcessProducerMethod<?, ?> producer = (ProcessProducerMethod<?, ?>)pb;

			annotated = producer.getAnnotated();
			annotatedClass = producer.getAnnotatedProducerMethod().getDeclaringType().getJavaClass();
		}
		else if (pb instanceof ProcessProducerField) {
			ProcessProducerField<?, ?> producer = (ProcessProducerField<?, ?>)pb;

			annotated = producer.getAnnotated();
			annotatedClass = producer.getAnnotatedProducerField().getDeclaringType().getJavaClass();
		}
		else if (pb instanceof ProcessSyntheticBean) {
			ProcessSyntheticBean<?> synthetic = (ProcessSyntheticBean<?>)pb;

			annotated = synthetic.getAnnotated();
			annotatedClass = synthetic.getBean().getBeanClass();
		}
		else {
			return;
		}

		final Class<?> annotatedClassFinal = annotatedClass;

		String className = annotatedClass.getName();

		OSGiBean osgiBean = _beansModel.getOSGiBean(className);

		if (osgiBean == null) {
			return;
		}

		osgiBean.found(true);

		try {
			List<Class<?>> serviceTypes = Types.collectServiceTypes(annotated);

			if ((annotated instanceof AnnotatedType) &&
				Optional.ofNullable(
					annotated.getAnnotation(SingleComponent.class)).isPresent()) {

				ComponentTemplateDTO componentTemplate = new ComponentTemplateDTO();
				componentTemplate.activations = new CopyOnWriteArrayList<>();

				if (!serviceTypes.isEmpty()) {
					ExtendedActivationTemplateDTO activationTemplate = new ExtendedActivationTemplateDTO();
					activationTemplate.declaringClass = annotatedClass;
					activationTemplate.properties = Collections.emptyMap();
					activationTemplate.scope = getScope(annotated);
					activationTemplate.serviceClasses = serviceTypes.stream().map(
						st -> st.getName()
					).collect(Collectors.toList());

					componentTemplate.activations.add(activationTemplate);
				}

				componentTemplate.beans = new CopyOnWriteArrayList<>();
				componentTemplate.configurations = new CopyOnWriteArrayList<>();
				componentTemplate.name = pb.getBean().getName();
				componentTemplate.properties = Maps.componentProperties(annotated);
				componentTemplate.references = new CopyOnWriteArrayList<>();
				componentTemplate.type = ComponentTemplateDTO.Type.SINGLE;

				annotated.getAnnotations(PID.class).stream().forEach(
					PID -> {
						ExtendedConfigurationTemplateDTO configurationTemplate = new ExtendedConfigurationTemplateDTO();

						configurationTemplate.componentConfiguration = true;
						configurationTemplate.declaringClass = annotatedClassFinal;
						configurationTemplate.maximumCardinality = MaximumCardinality.ONE;
						configurationTemplate.pid = Optional.of(PID.value()).map(
							s -> {
								if (s.equals("$") || s.equals("")) {
									return componentTemplate.name;
								}
								return s;
							}
						).orElse(componentTemplate.name);

						if (PID.value().equals("$") || PID.value().equals("")) {
							configurationTemplate.pid = componentTemplate.name;
						}
						else {
							configurationTemplate.pid = PID.value();
						}

						configurationTemplate.policy =
							PID.policy() == Policy.REQUIRED ?
								ConfigurationPolicy.REQUIRED :
								ConfigurationPolicy.OPTIONAL;

						componentTemplate.configurations.add(configurationTemplate);
					}
				);

				if (componentTemplate.configurations.isEmpty()) {
					ExtendedConfigurationTemplateDTO configurationTemplate = new ExtendedConfigurationTemplateDTO();

					configurationTemplate.componentConfiguration = true;
					configurationTemplate.declaringClass = annotatedClass;
					configurationTemplate.maximumCardinality = MaximumCardinality.ONE;
					configurationTemplate.pid = componentTemplate.name;
					configurationTemplate.policy = ConfigurationPolicy.OPTIONAL;

					componentTemplate.configurations.add(configurationTemplate);
				}

				componentTemplate.beans.add(className);

				_containerState.containerDTO().template.components.add(componentTemplate);

				osgiBean.setComponent(componentTemplate);
			}
			else if ((annotated instanceof AnnotatedType) &&
					Optional.ofNullable(
					annotated.getAnnotation(FactoryComponent.class)).isPresent()) {

				ComponentTemplateDTO componentTemplate = new ComponentTemplateDTO();
				componentTemplate.activations = new CopyOnWriteArrayList<>();

				if (!serviceTypes.isEmpty()) {
					ExtendedActivationTemplateDTO activationTemplate = new ExtendedActivationTemplateDTO();
					activationTemplate.declaringClass = annotatedClass;
					activationTemplate.properties = Collections.emptyMap();
					activationTemplate.scope = getScope(annotated);
					activationTemplate.serviceClasses = serviceTypes.stream().map(
						st -> st.getName()
					).collect(Collectors.toList());

					componentTemplate.activations.add(activationTemplate);
				}

				componentTemplate.beans = new CopyOnWriteArrayList<>();
				componentTemplate.configurations = new CopyOnWriteArrayList<>();
				componentTemplate.name = pb.getBean().getName();
				componentTemplate.properties = Maps.componentProperties(annotated);
				componentTemplate.references = new CopyOnWriteArrayList<>();
				componentTemplate.type = ComponentTemplateDTO.Type.FACTORY;

				annotated.getAnnotations(PID.class).stream().forEach(
					PID -> {
						ExtendedConfigurationTemplateDTO configurationTemplate = new ExtendedConfigurationTemplateDTO();

						configurationTemplate.componentConfiguration = true;
						configurationTemplate.declaringClass = annotatedClassFinal;
						configurationTemplate.maximumCardinality = MaximumCardinality.ONE;
						configurationTemplate.pid = Optional.of(PID.value()).map(
							s -> {
								if (s.equals("$") || s.equals("")) {
									return componentTemplate.name;
								}
								return s;
							}
						).orElse(componentTemplate.name);

						configurationTemplate.policy =
							PID.policy() == Policy.REQUIRED ?
								ConfigurationPolicy.REQUIRED :
								ConfigurationPolicy.OPTIONAL;

						componentTemplate.configurations.add(configurationTemplate);
					}
				);

				ExtendedConfigurationTemplateDTO configurationTemplate = new ExtendedConfigurationTemplateDTO();

				configurationTemplate.componentConfiguration = true;
				configurationTemplate.declaringClass = annotatedClass;
				configurationTemplate.maximumCardinality = MaximumCardinality.MANY;
				configurationTemplate.pid = Optional.ofNullable(
					annotated.getAnnotation(FactoryComponent.class)
				).map(fc -> {
					if (fc.value().equals("$") || fc.value().equals("")) {
						return componentTemplate.name;
					}
					return fc.value();
				}).orElse(componentTemplate.name);
				configurationTemplate.policy = ConfigurationPolicy.REQUIRED;

				componentTemplate.configurations.add(configurationTemplate);
				componentTemplate.beans.add(className);

				_containerState.containerDTO().template.components.add(componentTemplate);

				osgiBean.setComponent(componentTemplate);
			}
			else {
				if (!_containerTemplate.beans.contains(className)) {
					_containerTemplate.beans.add(className);
				}

				if (!serviceTypes.isEmpty()) {
					ExtendedActivationTemplateDTO activationTemplate = new ExtendedActivationTemplateDTO();
					activationTemplate.declaringClass = annotatedClass;
					activationTemplate.properties = Maps.componentProperties(annotated);
					activationTemplate.scope = getScope(annotated);
					activationTemplate.serviceClasses = serviceTypes.stream().map(
						st -> st.getName()
					).collect(Collectors.toList());

					_containerTemplate.activations.add(activationTemplate);
				}

				osgiBean.setComponent(_containerTemplate);
			}
		}
		catch (Exception e) {
			pb.addDefinitionError(e);
		}
	}

	private Scope getScope(Annotated annotated) {
		Prototype prototype = annotated.getAnnotation(Prototype.class);
		Bundle bundle = annotated.getAnnotation(Bundle.class);

		if (prototype != null) {
			if (bundle != null) {
				throw new IllegalArgumentException(
					String.format(
						"@Prototype and @Bundle must not be used to gether: %s",
						annotated));
			}

			return Scope.PROTOTYPE;
		}

		if (bundle != null) {
			return Scope.BUNDLE;
		}

		return Scope.SINGLETON;
	}

	void processInjectionPoint(@Observes ProcessInjectionPoint<?, ?> pip) {
		final InjectionPoint injectionPoint = pip.getInjectionPoint();

		Annotated annotated = injectionPoint.getAnnotated();

		Class<?> injectionPointClass = null;

		if (annotated instanceof AnnotatedParameter) {
			AnnotatedParameter<?> ap = (AnnotatedParameter<?>)annotated;

			Parameter javaParameter = ap.getJavaParameter();

			Executable executable = javaParameter.getDeclaringExecutable();

			injectionPointClass = executable.getDeclaringClass();
		}
		else {
			AnnotatedField<?> af = (AnnotatedField<?>)annotated;

			injectionPointClass = af.getDeclaringType().getJavaClass();
		}

		String className = injectionPointClass.getName();

		OSGiBean osgiBean = _beansModel.getOSGiBean(className);

		if (osgiBean == null) {
			return;
		}

		Reference reference = annotated.getAnnotation(Reference.class);
		Configuration configuration = annotated.getAnnotation(Configuration.class);

		if (reference != null) {
			if (configuration != null) {
				pip.addDefinitionError(
					new IllegalArgumentException(
						String.format(
							"Cannot use @Reference and @Configuration on the same injection point {}",
							injectionPoint))
				);

				return;
			}

			Builder builder = null;

			if (annotated instanceof AnnotatedParameter) {
				builder = new ReferenceModel.Builder((AnnotatedParameter<?>)annotated);
			}
			else {
				builder = new ReferenceModel.Builder((AnnotatedField<?>)annotated);
			}

			try {
				ReferenceModel referenceModel = builder.type(injectionPoint.getType()).build();

				osgiBean.addReference(referenceModel.toDTO());
			}
			catch (Exception e) {
				pip.addDefinitionError(e);
			}
		}
		else if (configuration != null) {
			try {
				ConfigurationModel configurationModel = new ConfigurationModel.Builder(
					injectionPoint.getType()
				).declaringClass(
					injectionPointClass
				).injectionPoint(
					injectionPoint
				).build();

				osgiBean.addConfiguration(configurationModel.toDTO());
			}
			catch (Exception e) {
				pip.addDefinitionError(e);
			}
		}
	}

	void processObserverMethod(@Observes ProcessObserverMethod<ReferenceEvent<?>, ?> pom) {
		ObserverMethod<ReferenceEvent<?>> observerMethod = pom.getObserverMethod();

		AnnotatedMethod<?> annotatedMethod = pom.getAnnotatedMethod();

		Configuration configuration = annotatedMethod.getAnnotation(Configuration.class);

		if (configuration != null) {
			pom.addDefinitionError(
				new IllegalArgumentException(
					String.format(
						"Cannot use @Configuration on ReferenceEvent observer method {}",
						observerMethod))
			);

			return;
		}

		Class<?> beanClass = observerMethod.getBeanClass();

		final String className = beanClass.getName();

		OSGiBean osgiBean = _beansModel.getOSGiBean(className);

		if (osgiBean == null) {
			pom.addDefinitionError(
				new DefinitionException(
					String.format(
						"The observer method %s was not declared as <cdi:bean class=\"%s\">",
						observerMethod, className))
			);

			return;
		}

		try {
			ReferenceModel referenceModel = new ReferenceModel.Builder(
				pom.getAnnotatedMethod().getParameters().get(0)
			).type(observerMethod.getObservedType()).build();

			osgiBean.addReference(referenceModel.toDTO());
		}
		catch (Exception e) {
			pom.addDefinitionError(e);
		}
	}

	private final BeansModel _beansModel;
	private final ComponentTemplateDTO _containerTemplate;
	private final ContainerState _containerState;

}
