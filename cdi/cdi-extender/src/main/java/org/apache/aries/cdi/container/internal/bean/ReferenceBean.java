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

package org.apache.aries.cdi.container.internal.bean;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.InjectionPoint;

import org.apache.aries.cdi.container.internal.model.CollectionType;
import org.apache.aries.cdi.container.internal.model.ExtendedReferenceTemplateDTO;
import org.apache.aries.cdi.container.internal.util.Sets;
import org.osgi.service.cdi.annotations.ComponentScoped;
import org.osgi.service.cdi.runtime.dto.template.ComponentTemplateDTO;
import org.osgi.service.cdi.runtime.dto.template.MaximumCardinality;
import org.osgi.service.cdi.runtime.dto.template.ReferenceTemplateDTO.Policy;

public class ReferenceBean implements Bean<Object> {

	public ReferenceBean(
		ComponentTemplateDTO componentTemplateDTO,
		ExtendedReferenceTemplateDTO templateDTO) {

		_componentTemplateDTO = componentTemplateDTO;
		_template = templateDTO;

		_types = Sets.hashSet(_template.injectionPointType, Object.class);
	}

	@Override
	public Object create(CreationalContext<Object> creationalContext) {
		if (_template.maximumCardinality == MaximumCardinality.MANY) {
			// Collection, Iterable, List
			if (_template.policy == Policy.DYNAMIC) {
				// Provider
				// PolicyOption.GREEDY is IGNORED
				if (_template.collectionType == CollectionType.OBSERVER) {

				}
				else if (_template.collectionType == CollectionType.PROPERTIES) {

				}
				else if (_template.collectionType == CollectionType.REFERENCE) {

				}
				else if (_template.collectionType == CollectionType.SERVICEOBJECTS) {

				}
				else if (_template.collectionType == CollectionType.TUPLE) {

				}
				else { // (_template.collectionType == CollectionType.SERVICE)

				}
			}
			else {

			}
		}
		else {

		}

//		List<Decorator<?>> decorators = beanManager.resolveDecorators(
//			Collections.singleton(Class.forName(_template.serviceType)),
//			new Annotation[0]);
//		if (!decorators.isEmpty()) {
//			instance = Decorators.getOuterDelegate(
//				cast(this), instance, creationalContext, cast(getBeanClass()), _injectionPoint, _beanManager, decorators);
//		}

//		Map<String, ReferenceCallback> map = _containerState.referenceCallbacks().get(_componentModel);

//		ReferenceCallback referenceCallback = map.get(_referenceModel.getName());

		return null; // TODO referenceCallback.tracked().values().iterator().next();
	}

	@Override
	public void destroy(Object instance, CreationalContext<Object> creationalContext) {
	}

	@Override
	public Class<?> getBeanClass() {
		return _template.beanClass;
	}

	@Override
	public Set<InjectionPoint> getInjectionPoints() {
		return Collections.emptySet();
	}

	@Override
	public String getName() {
		return _template.name;
	}

	@Override
	public Set<Annotation> getQualifiers() {
		return Collections.singleton(Default.Literal.INSTANCE);
	}

	@Override
	public Class<? extends Annotation> getScope() {
		if (_componentTemplateDTO.type == ComponentTemplateDTO.Type.CONTAINER) {
			return ApplicationScoped.class;
		}
		return ComponentScoped.class;
	}

	@Override
	public Set<Class<? extends Annotation>> getStereotypes() {
		return Collections.emptySet();
	}

	@Override
	public Set<Type> getTypes() {
		return _types;
	}

	@Override
	public boolean isAlternative() {
		return false;
	}

	@Override
	public boolean isNullable() {
		return false;
	}

	@Override
	public String toString() {
		return "ReferenceBean[" + _template.name + "]";
	}

	private final ComponentTemplateDTO _componentTemplateDTO;
	private final ExtendedReferenceTemplateDTO _template;
	private final Set<Type> _types;

}
