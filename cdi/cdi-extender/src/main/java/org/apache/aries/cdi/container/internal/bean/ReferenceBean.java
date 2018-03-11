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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.Decorator;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.inject.Provider;

import org.apache.aries.cdi.container.internal.container.Mark;
import org.apache.aries.cdi.container.internal.model.CollectionType;
import org.apache.aries.cdi.container.internal.model.ExtendedReferenceDTO;
import org.apache.aries.cdi.container.internal.model.ExtendedReferenceTemplateDTO;
import org.apache.aries.cdi.container.internal.model.ReferenceEventImpl;
import org.apache.aries.cdi.container.internal.util.Logs;
import org.apache.aries.cdi.container.internal.util.Sets;
import org.jboss.weld.bean.builtin.BeanManagerProxy;
import org.jboss.weld.injection.CurrentInjectionPoint;
import org.jboss.weld.injection.EmptyInjectionPoint;
import org.jboss.weld.manager.BeanManagerImpl;
import org.jboss.weld.util.Decorators;
import org.osgi.service.cdi.ComponentType;
import org.osgi.service.cdi.MaximumCardinality;
import org.osgi.service.cdi.ReferencePolicy;
import org.osgi.service.cdi.annotations.ComponentScoped;
import org.osgi.service.cdi.annotations.Reference;
import org.osgi.service.cdi.runtime.dto.template.ComponentTemplateDTO;
import org.osgi.service.log.Logger;

public class ReferenceBean implements Bean<Object> {

	public ReferenceBean(
		ComponentTemplateDTO component,
		ExtendedReferenceTemplateDTO template) {

		_component = component;
		_template = template;

		_qualifiers = Sets.hashSet(Reference.Literal.of(Object.class, ""), Default.Literal.INSTANCE);
		_types = Sets.hashSet(_template.injectionPointType, Object.class);
	}

	@Override
	public Object create(CreationalContext<Object> c) {
		if (_template.collectionType != CollectionType.OBSERVER) return null;

		Objects.requireNonNull(_bm);
		Objects.requireNonNull(_snapshot);

		_log.debug(l -> l.debug("Creating {}", this));

		if (_template.policy == ReferencePolicy.DYNAMIC) {
			if (_template.maximumCardinality == MaximumCardinality.MANY) {
				return new Provider<List<Object>>() {
					@Override
					public List<Object> get() {
						return Arrays.stream(
							_snapshot.serviceTracker.getServices()
						).map(
							s -> decorate(c, s)
						).collect(Collectors.toList());
					}

				};
			}
			else if (_template.minimumCardinality == 0) {
				return new Provider<Optional<Object>>() {
					@Override
					public Optional<Object> get() {
						return Optional.ofNullable(decorate(c, _snapshot.serviceTracker.getService()));
					}
				};
			}
			else {
				return new Provider<Object>() {
					@Override
					public Object get() {
						return decorate(c, _snapshot.serviceTracker.getService());
					}
				};
			}
		}
		else {
			if (_template.maximumCardinality == MaximumCardinality.MANY) {
				return Arrays.stream(
					_snapshot.serviceTracker.getServices()
				).map(
					s -> decorate(c, s)
				).collect(Collectors.toList());
			}
			else if (_template.minimumCardinality == 0) {
				return Optional.ofNullable(decorate(c, _snapshot.serviceTracker.getService()));
			}
			else {
				return decorate(c, _snapshot.serviceTracker.getService());
			}
		}
	}

	@SuppressWarnings("unchecked")
	private <S> S decorate(CreationalContext<S> c, S s) {
		if (s == null) return null;

		List<Decorator<?>> decorators = _bm.resolveDecorators(
			Collections.singleton(_template.serviceClass),
			new Annotation[0]);

		if (decorators.isEmpty()) {
			return s;
		}

		BeanManagerImpl bmi = ((BeanManagerProxy)_bm).delegate();
		CurrentInjectionPoint cip = bmi.getServices().get(CurrentInjectionPoint.class);

		return Decorators.getOuterDelegate(
			(Bean<S>)this, s, c, (Class<S>)_template.serviceClass, getIP(cip), bmi, decorators);
	}

	private InjectionPoint getIP(CurrentInjectionPoint cip) {
		InjectionPoint ip = cip.peek();
		return EmptyInjectionPoint.INSTANCE.equals(ip) ? null : ip;
	}

	@Override
	public void destroy(Object instance, CreationalContext<Object> creationalContext) {
	}

	public void fireEvents() {
		if (_template.collectionType != CollectionType.OBSERVER) return;

		Arrays.stream(
			_snapshot.serviceTracker.getServices()
		).map(
			o -> (ReferenceEventImpl<?>)o
		).forEach(
			event -> {
				try {
					_bm.getEvent().select(
						Reference.Literal.of(_template.serviceClass, _template.targetFilter)
					).fire(event);
				}
				catch (Exception e) {
					_log.error(l -> l.error("CCR observer method error on {}", _snapshot, e));
				}

				event.flush();
			}
		);
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
		return null;//_template.name;
	}

	@Override
	public Set<Annotation> getQualifiers() {
		return _qualifiers;
	}

	@Override
	public Class<? extends Annotation> getScope() {
		if (_component.type == ComponentType.CONTAINER) {
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

	public void setMark(Mark mark) {
		_qualifiers.add(mark);
	}

	public void prepare(ExtendedReferenceDTO snapshot, BeanManager bm) {
		_snapshot = snapshot;
		_bm = bm;
	}

	@Override
	public String toString() {
		if (_string == null) {
			_string =  "ReferenceBean[" + _template.name + ", " + _template.injectionPointType + ", " + getScope().getSimpleName() + "]";
		}
		return _string;
	}

	private static final Logger _log = Logs.getLogger(ReferenceBean.class);

	private volatile BeanManager _bm;
	private final ComponentTemplateDTO _component;
	private final Set<Annotation> _qualifiers;
	private final ExtendedReferenceTemplateDTO _template;
	private final Set<Type> _types;
	private volatile ExtendedReferenceDTO _snapshot;
	private volatile String _string;

}
