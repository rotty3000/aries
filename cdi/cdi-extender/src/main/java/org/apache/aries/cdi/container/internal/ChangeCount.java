package org.apache.aries.cdi.container.internal;

import java.util.Observable;
import java.util.Observer;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.aries.cdi.container.internal.util.Syncro;

public class ChangeCount extends Observable implements Observer {

	@Override
	public int hashCode() {
		try (Syncro syncro = _syncro.open()) {
			return _changeCount.hashCode();
		}
	}

	public final long get() {
		try (Syncro syncro = _syncro.open()) {
			return _changeCount.get();
		}
	}

	@Override
	public boolean equals(Object obj) {
		try (Syncro syncro = _syncro.open()) {
			return _changeCount.equals(obj);
		}
	}

	public final long getAndIncrement() {
		try (Syncro syncro = _syncro.open()) {
			return _changeCount.getAndIncrement();
		}
		finally {
			setChanged();
			notifyObservers();
		}
	}

	public final long incrementAndGet() {
		try (Syncro syncro = _syncro.open()) {
			return _changeCount.incrementAndGet();
		}
		finally {
			setChanged();
			notifyObservers();
		}
	}

	@Override
	public String toString() {
		try (Syncro syncro = _syncro.open()) {
			return _changeCount.toString();
		}
	}

	public long longValue() {
		try (Syncro syncro = _syncro.open()) {
			return _changeCount.longValue();
		}
	}

	@Override
	public void update(Observable o, Object arg) {
		if (!(o instanceof ChangeCount)) {
			return;
		}

		incrementAndGet();
	}

	private final AtomicLong _changeCount = new AtomicLong(1);
	private final Syncro _syncro = new Syncro(true);

}