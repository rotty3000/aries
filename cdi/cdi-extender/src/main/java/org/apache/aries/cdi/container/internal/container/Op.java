package org.apache.aries.cdi.container.internal.container;

import java.util.Arrays;

public class Op {

	public static enum Mode {CLOSE, OPEN}

	public static enum Type {
		CONFIGURATION_LISTENER,
		CONTAINER_BOOTSTRAP,
		CONTAINER_COMPONENT,
		CONTAINER_FIRE_EVENTS,
		CONTAINER_INSTANCE,
		CONTAINER_PUBLISH_SERVICES,
		REFERENCES,
		EXTENSION,
		FACTORY_COMPONENT,
		FACTORY_INSTANCE,
		INIT,
		SINGLE_COMPONENT,
		SINGLE_INSTANCE,
	}

	public static Op of(Mode mode, Type type, String name) {
		return new Op(mode, type, name);
	}

	private Op(Mode mode, Type type, String name) {
		this.mode = mode;
		this.type = type;
		this.name = name;
	}

	public final Mode mode;
	public final Type type;
	public final String name;

	@Override
	public String toString() {
		return Arrays.asList(getClass().getSimpleName(), mode, type, name).toString();
	}

}