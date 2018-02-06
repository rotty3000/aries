package org.apache.aries.cdi.container.internal.util;

import java.util.concurrent.locks.ReentrantLock;

public class Syncro extends ReentrantLock implements AutoCloseable {

	private static final long serialVersionUID = 1L;

	public Syncro() {
		super();
	}

	public Syncro(boolean fair) {
		super(fair);
	}

	public Syncro open() {
		lock();
		return this;
	}


	@Override
	public void close() {
		unlock();
	}

}
