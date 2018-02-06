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

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.aries.cdi.container.internal.util.Syncro;
import org.osgi.service.cdi.runtime.dto.template.ComponentTemplateDTO;
import org.osgi.service.cdi.runtime.dto.template.ConfigurationTemplateDTO;
import org.osgi.service.cdi.runtime.dto.template.ReferenceTemplateDTO;

public class OSGiBean implements Comparable<OSGiBean> {

	public static class Builder {

		public Builder(Class<?> beanClass) {
			Objects.requireNonNull(beanClass);
			_beanClass = beanClass;
		}

		public OSGiBean build() {
			return new OSGiBean(_beanClass);
		}

		private Class<?> _beanClass;

	}

	private OSGiBean(
		Class<?> beanClass) {

		_beanClass = beanClass;
	}

	public synchronized void addConfiguration(ConfigurationTemplateDTO dto) {
		try (Syncro syncro = _lock.open()) {
			if (_ctDTO == null) {
				_configurationsQueue.add(dto);
			}
			else {
				_ctDTO.configurations.add(dto);
			}
		}
	}

	public synchronized void addReference(ReferenceTemplateDTO dto) {
		try (Syncro syncro = _lock.open()) {
			if (_ctDTO == null) {
				_referencesQueue.add(dto);
			}
			else {
				_ctDTO.references.add(dto);
			}
		}
	}

	@Override
	public int compareTo(OSGiBean other) {
		return _beanClass.getName().compareTo(other._beanClass.getName());
	}

	public boolean found() {
		return _found.get();
	}

	public void found(boolean found) {
		_found.set(found);
	}

	public Class<?> getBeanClass() {
		return _beanClass;
	}

	public synchronized ComponentTemplateDTO geComponentTemplateDTO() {
		try (Syncro syncro = _lock.open()) {
			return _ctDTO;
		}
	}

	public void setComponent(ComponentTemplateDTO componentDTO) {
		try (Syncro syncro = _lock.open()) {
			_ctDTO = componentDTO;
			_configurationsQueue.removeIf(
				dto -> {
					_ctDTO.configurations.add(dto);
					return true;
				}
			);
			_referencesQueue.removeIf(
				dto -> {
					_ctDTO.references.add(dto);
					return true;
				}
			);
		}
	}

	@Override
	public String toString() {
		if (_string == null) {
			_string = String.format("OSGiBean[%s]", _beanClass.getName());
		}
		return _string;
	}

	private final Syncro _lock = new Syncro(true);
	private final Class<?> _beanClass;
	private final List<ConfigurationTemplateDTO> _configurationsQueue = new CopyOnWriteArrayList<>();
	private final List<ReferenceTemplateDTO> _referencesQueue = new CopyOnWriteArrayList<>();
	private volatile ComponentTemplateDTO _ctDTO;
	private final AtomicBoolean _found = new AtomicBoolean();
	private volatile String _string;
}
