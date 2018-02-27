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

package org.apache.aries.cdi.container.internal.model;

import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Consumer;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cdi.reference.ReferenceEvent;
import org.osgi.service.cdi.reference.ReferenceServiceObjects;

public class ReferenceEventImpl<T> implements ReferenceEvent<T> {

	private final ServiceReference<T> _reference;
	private final BundleContext _bundleContext;

	public ReferenceEventImpl(ServiceReference<T> reference, BundleContext bundleContext) {
		_reference = reference;
		_bundleContext = bundleContext;
	}

	public ReferenceEventImpl addingService() {
		// TODO fire this sometime
		return this;
	}

	public ReferenceEventImpl modifiedService() {
		// TODO fire this sometime
		return this;
	}

	public ReferenceEventImpl removedService() {
		// TODO fire this sometime
		return this;
	}

	@Override
	public void onAdding(Consumer<T> action) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onAddingServiceReference(Consumer<ServiceReference<T>> consumer) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onAddingServiceObjects(Consumer<ReferenceServiceObjects<T>> consumer) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onAddingProperties(Consumer<Map<String, ?>> consumer) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onAddingTuple(Consumer<Entry<Map<String, ?>, T>> consumer) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onUpdate(Consumer<T> action) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onUpdateServiceReference(Consumer<ServiceReference<T>> consumer) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onUpdateServiceObjects(Consumer<ReferenceServiceObjects<T>> consumer) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onUpdateProperties(Consumer<Map<String, ?>> consumer) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onUpdateTuple(Consumer<Entry<Map<String, ?>, T>> consumer) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onRemove(Consumer<T> consumer) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onRemoveServiceReference(Consumer<ServiceReference<T>> consumer) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onRemoveServiceObjects(Consumer<ReferenceServiceObjects<T>> consumer) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onRemoveProperties(Consumer<Map<String, ?>> consumer) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onRemoveTuple(Consumer<Entry<Map<String, ?>, T>> consumer) {
		// TODO Auto-generated method stub

	}

}