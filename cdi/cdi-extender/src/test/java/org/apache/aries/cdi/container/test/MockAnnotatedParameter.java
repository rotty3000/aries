package org.apache.aries.cdi.container.test;

import static org.apache.aries.cdi.container.test.AnnotatedCache.collectTypes;
import static org.apache.aries.cdi.container.test.AnnotatedCache.getAnnotatedCallable;

import java.lang.annotation.Annotation;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import javax.enterprise.inject.spi.AnnotatedCallable;
import javax.enterprise.inject.spi.AnnotatedParameter;

public class MockAnnotatedParameter<X> implements AnnotatedParameter<X> {

	private final Parameter _parameter;
	private Set<Type> _types;

	public MockAnnotatedParameter(Parameter parameter) {
		_parameter = parameter;
		_types = collectTypes(_parameter.getType());
	}

	@Override
	public <T extends Annotation> T getAnnotation(Class<T> annotationType) {
		return _parameter.getAnnotation(annotationType);
	}

	@Override
	public Set<Annotation> getAnnotations() {
		return Arrays.stream(_parameter.getAnnotations()).collect(Collectors.toSet());
	}

	@Override
	public Type getBaseType() {
		return _parameter.getType();
	}

	@Override
	public AnnotatedCallable<X> getDeclaringCallable() {
		return getAnnotatedCallable(_parameter.getDeclaringExecutable());
	}

	@Override
	public int getPosition() {
		Parameter[] parameters = _parameter.getDeclaringExecutable().getParameters();
		for (int i = 0; i < parameters.length; i++) {
			if (_parameter.equals(parameters[i])) {
				return i + 1;
			}
		}
		return -1;
	}

	@Override
	public Set<Type> getTypeClosure() {
		return _types;
	}

	@Override
	public boolean isAnnotationPresent(Class<? extends Annotation> annotationType) {
		return Arrays.stream(_parameter.getAnnotations()).filter(
			ann -> ann.annotationType().equals(annotationType)
		).findFirst().isPresent();
	}

}
