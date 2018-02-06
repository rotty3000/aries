package org.apache.aries.cdi.container.test;

import static org.apache.aries.cdi.container.test.AnnotatedCache.collectTypes;
import static org.apache.aries.cdi.container.test.AnnotatedCache.getAnnotatedParameter;
import static org.apache.aries.cdi.container.test.AnnotatedCache.getAnnotatedType;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.enterprise.inject.spi.AnnotatedConstructor;
import javax.enterprise.inject.spi.AnnotatedParameter;
import javax.enterprise.inject.spi.AnnotatedType;

class MockAnnotatedConstructor<X> implements AnnotatedConstructor<X> {

	private final Constructor<X> _constructor;
	private final AnnotatedType<X> _declaringType;
	private final Set<Type> _types;

	public MockAnnotatedConstructor(Constructor<X> constructor) {
		_constructor = constructor;
		_declaringType = getAnnotatedType(constructor.getDeclaringClass());
		_types = collectTypes(constructor.getDeclaringClass());
	}

	@Override
	public <T extends Annotation> T getAnnotation(Class<T> annotationType) {
		return _constructor.getAnnotation(annotationType);
	}

	@Override
	public Set<Annotation> getAnnotations() {
		return Arrays.stream(_constructor.getAnnotations()).collect(Collectors.toSet());
	}

	@Override
	public Type getBaseType() {
		return _constructor.getDeclaringClass();
	}

	@Override
	public AnnotatedType<X> getDeclaringType() {
		return _declaringType;
	}

	@Override
	public Constructor<X> getJavaMember() {
		return _constructor;
	}

	@Override
	@SuppressWarnings("unchecked")
	public List<AnnotatedParameter<X>> getParameters() {
		return Arrays.stream(_constructor.getParameters()).map(
			p -> (AnnotatedParameter<X>)getAnnotatedParameter(p)
		).collect(Collectors.toList());
	}

	@Override
	public Set<Type> getTypeClosure() {
		return _types;
	}

	@Override
	public boolean isAnnotationPresent(Class<? extends Annotation> annotationType) {
		return Arrays.stream(_constructor.getAnnotations()).filter(
			ann -> ann.annotationType().equals(annotationType)
		).findFirst().isPresent();
	}

	@Override
	public boolean isStatic() {
		return false;
	}

}