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

package org.apache.aries.cdi.container.internal.phase;

import static org.apache.aries.cdi.container.internal.util.Filters.*;

import java.util.Iterator;
import java.util.List;
import java.util.SortedSet;
import java.util.concurrent.ConcurrentSkipListSet;

import javax.enterprise.inject.spi.Extension;

import org.apache.aries.cdi.container.internal.container.ContainerState;
import org.apache.aries.cdi.container.internal.container.Op;
import org.apache.aries.cdi.container.internal.model.ExtendedExtensionDTO;
import org.apache.aries.cdi.container.internal.model.ExtendedExtensionTemplateDTO;
import org.apache.aries.cdi.container.internal.util.Logs;
import org.apache.aries.cdi.container.internal.util.SRs;
import org.osgi.framework.Filter;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cdi.runtime.dto.ExtensionDTO;
import org.osgi.service.cdi.runtime.dto.template.ExtensionTemplateDTO;
import org.osgi.service.log.Logger;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

public class ExtensionPhase extends Phase {

	public ExtensionPhase(ContainerState containerState, Phase next) {
		super(containerState, next);
	}

	@Override
	public boolean close() {
		if (!extensionTemplates().isEmpty()) {
			if (_extensionTracker != null) {
				_extensionTracker.close();

				_extensionTracker = null;
			}

			return true;
		}
		else {
			return next.map(
				next -> {
					try {
						return next.close();
					}
					catch (Throwable t) {
						_log.error(l -> l.error("CCR Error in extension CLOSE on {}", bundle(), t));

						return false;
					}
				}
			).get();
		}
	}

	@Override
	public boolean open() {
		if (!extensionTemplates().isEmpty()) {
			_extensionTracker = new ServiceTracker<>(
				containerState.bundleContext(), createExtensionFilter(), new ExtensionPhaseCustomizer());

			_extensionTracker.open();
		}
		else {
			next.ifPresent(
				next -> submit(Op.CONFIGURATION_OPEN, next::open).then(
					null,
					f -> {
						_log.error(l -> l.error("CCR Error in extension OPEN on {}", bundle(), f.getFailure()));

						error(f.getFailure());
					}
				)
			);
		}

		return true;
	}

	Filter createExtensionFilter() {
		final List<ExtensionTemplateDTO> templates = extensionTemplates();

		StringBuilder sb = new StringBuilder("(&(objectClass=" + Extension.class.getName() + ")");

		if (templates.size() > 1) sb.append("(|");

		for (ExtensionTemplateDTO tmpl : templates) {
			sb.append(tmpl.serviceFilter);
		}

		if (templates.size() > 1) sb.append(")");

		sb.append(")");

		return asFilter(sb.toString());
	}

	List<ExtensionTemplateDTO> extensionTemplates() {
		return containerState.containerDTO().template.extensions;
	}

	List<ExtensionDTO> snapshots() {
		return containerState.containerDTO().extensions;
	}

	private static final Logger _log = Logs.getLogger(ExtensionPhase.class);

	private ServiceTracker<Extension, ExtendedExtensionDTO> _extensionTracker;
	private final SortedSet<ExtendedExtensionDTO> _references = new ConcurrentSkipListSet<>(
		(e1, e2) -> e1.serviceReference.compareTo(e2.serviceReference)
	);

	private class ExtensionPhaseCustomizer implements ServiceTrackerCustomizer<Extension, ExtendedExtensionDTO> {

		@Override
		public ExtendedExtensionDTO addingService(ServiceReference<Extension> reference) {
			ExtendedExtensionTemplateDTO template = extensionTemplates().stream().map(
				t -> (ExtendedExtensionTemplateDTO)t
			).filter(
				t -> t.filter.match(reference)
			).findFirst().get();

			ExtendedExtensionDTO snapshot = snapshots().stream().map(
				s -> (ExtendedExtensionDTO)s
			).filter(
				s -> s.template == template
			).findFirst().orElse(null);

			if (snapshot != null) {
				if (reference.compareTo(snapshot.serviceReference) <= 0) {
					return null;
				}

				if (snapshots().remove(snapshot)) {
					_references.add(snapshot);
					snapshot.extension = null;
					containerState.bundleContext().ungetService(snapshot.serviceReference);
				}
			}

			ExtendedExtensionDTO extensionDTO = new ExtendedExtensionDTO();

			extensionDTO.extension = containerState.bundleContext().getService(reference);
			extensionDTO.service = SRs.from(reference);
			extensionDTO.serviceReference = reference;
			extensionDTO.template = template;

			snapshots().add(extensionDTO);
			containerState.incrementChangeCount();

			if (snapshots().size() == extensionTemplates().size()) {
				next.ifPresent(
					next -> submit(Op.CONFIGURATION_CLOSE, next::close).then(
						s -> {
							return submit(Op.CONFIGURATION_OPEN, next::open).then(
								null,
								f -> {
									_log.error(l -> l.error("CCR Error in extension open TRACKING {} on {}", reference, bundle()));

									error(f.getFailure());
								}
							);
						},
						f -> {
							_log.error(l -> l.error("CCR Error extension close TRACKING {} on {}", reference, bundle()));

							error(f.getFailure());
						}
					)
				);
			}

			return extensionDTO;
		}

		@Override
		public void modifiedService(ServiceReference<Extension> reference, ExtendedExtensionDTO extentionDTO) {
			removedService(reference, extentionDTO);
			addingService(reference);
		}

		@Override
		public void removedService(ServiceReference<Extension> reference, final ExtendedExtensionDTO extensionDTO) {
			containerState.bundleContext().ungetService(reference);

			if (!snapshots().removeIf(snap -> ((ExtendedExtensionDTO)snap).serviceReference.equals(reference))) {
				return;
			}

			for (Iterator<ExtendedExtensionDTO> itr = _references.iterator();itr.hasNext();) {
				ExtendedExtensionDTO entry = itr.next();
				if (((ExtendedExtensionTemplateDTO)extensionDTO.template).filter.match(entry.serviceReference)) {
					entry.extension = containerState.bundleContext().getService(entry.serviceReference);
					itr.remove();
					snapshots().add(entry);
					break;
				}
			}

			containerState.incrementChangeCount();

			next.ifPresent(
				next -> submit(Op.CONFIGURATION_CLOSE, next::close).then(
					s -> {
						if (snapshots().size() == extensionTemplates().size()) {
							return submit(Op.CONFIGURATION_OPEN, next::open).then(
								null,
								f -> {
									_log.error(l -> l.error("CCR Error in extension open TRACKING {} on {}", reference, bundle()));

									error(f.getFailure());
								}
							);
						}

						return s;
					},
					f -> {
						_log.error(l -> l.error("CCR Error extension close TRACKING {} on {}", reference, bundle()));

						error(f.getFailure());
					}
				)
			);
		}

	}

}
