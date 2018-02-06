package org.apache.aries.cdi.container.test;

import static org.apache.aries.cdi.container.test.AnnotatedCache.collectTypes;
import static org.apache.aries.cdi.container.test.AnnotatedCache.getAnnotatedParameter;
import static org.apache.aries.cdi.container.test.AnnotatedCache.getAnnotatedType;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.AnnotatedParameter;
import javax.enterprise.inject.spi.AnnotatedType;

public class MockAnnotatedMethod<X> implements AnnotatedMethod<X> {

	private final Method _method;
	private AnnotatedType<X> _declaringType;
	private Set<Type> _types;

	@SuppressWarnings("unchecked")
	public MockAnnotatedMethod(Method method) {
		_method = method;
		_declaringType = (AnnotatedType<X>) getAnnotatedType(_method.getDeclaringClass());
		_types = collectTypes(_method.getDeclaringClass());
	}

	@Override
	public <T extends Annotation> T getAnnotation(Class<T> annotationType) {
		return _method.getAnnotation(annotationType);
	}

	@Override
	public Set<Annotation> getAnnotations() {
		return Arrays.stream(_method.getAnnotations()).collect(Collectors.toSet());
	}

	@Override
	public Type getBaseType() {
		return _method.getDeclaringClass();
	}

	@Override
	public AnnotatedType<X> getDeclaringType() {
		return _declaringType;
	}

	@Override
	public Method getJavaMember() {
		return _method;
	}

	@Override
	@SuppressWarnings("unchecked")
	public List<AnnotatedParameter<X>> getParameters() {
		return Arrays.stream(_method.getParameters()).map(
			p -> (AnnotatedParameter<X>)getAnnotatedParameter(p)
		).collect(Collectors.toList());
	}

	@Override
	public Set<Type> getTypeClosure() {
		return _types;
	}

	@Override
	public boolean isAnnotationPresent(Class<? extends Annotation> annotationType) {
		return Arrays.stream(_method.getAnnotations()).filter(
			ann -> ann.annotationType().equals(annotationType)
		).findFirst().isPresent();
	}

	@Override
	public boolean isStatic() {
		return false;
	}

}
