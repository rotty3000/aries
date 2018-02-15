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

import java.util.Arrays;
import java.util.List;
import java.util.SortedSet;
import java.util.concurrent.ConcurrentSkipListSet;

import javax.enterprise.inject.spi.Extension;

import org.apache.aries.cdi.container.internal.container.ContainerState;
import org.apache.aries.cdi.container.internal.log.Logs;
import org.apache.aries.cdi.container.internal.model.ExtendedExtensionDTO;
import org.apache.aries.cdi.container.internal.model.ExtendedExtensionTemplateDTO;
import org.apache.aries.cdi.container.internal.util.Throw;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.dto.ServiceReferenceDTO;
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
		if (_extensionTracker != null) {
			_extensionTracker.close();

			_log.debug(l -> l.debug("CDIe - Ended extension TRACKING on {}", bundle()));

			_extensionTracker = null;
		}
		else {
			_log.debug(l -> l.debug("CDIe - Begin extension CLOSE on {}", bundle()));

			next.ifPresent(
				next -> submit(next::close).then(
					s -> {
						_log.debug(l -> l.debug("CDIe - Ended extension CLOSE on {}", bundle()));

						return s;
					},
					f -> {
						_log.error(l -> l.error("CDIe - Error in extension CLOSE on {}", bundle(), f.getFailure()));

						error(f.getFailure());
					}
				)
			);
		}

		return true;
	}

	@Override
	public boolean open() {
		if (!templates().isEmpty()) {
			_log.debug(l -> l.debug("CDIe - Begin extension TRACKING on {}", bundle()));

			_extensionTracker = new ServiceTracker<>(
				containerState.bundleContext(), createExtensionFilter(), new ExtensionPhaseCustomizer());

			_extensionTracker.open();
		}
		else {
			_log.debug(l -> l.debug("CDIe - Begin extension OPEN on {}", bundle()));

			next.ifPresent(
				next -> submit(next::open).then(
					s -> {
						_log.debug(l -> l.debug("CDIe - Ended extension OPEN on {}", bundle()));

						return s;
					},
					f -> {
						_log.error(l -> l.error("CDIe - Error in extension OPEN on {}", bundle(), f.getFailure()));

						error(f.getFailure());
					}
				)
			);
		}

		return true;
	}

	Filter createExtensionFilter() {
		final List<ExtensionTemplateDTO> templates = templates();

		try {
			StringBuilder sb = new StringBuilder("(&(objectClass=" + Extension.class.getName() + ")");

			if (templates.size() > 1) sb.append("(|");

			for (ExtensionTemplateDTO tmpl : templates) {
				sb.append(tmpl.serviceFilter);
			}

			if (templates.size() > 1) sb.append(")");

			sb.append(")");

			return FrameworkUtil.createFilter(sb.toString());
		}
		catch (InvalidSyntaxException ise) {
			return Throw.exception(ise);
		}
	}

	List<ExtensionTemplateDTO> templates() {
		return containerState.containerDTO().template.extensions;
	}

	List<ExtensionDTO> snapshots() {
		return containerState.containerDTO().extensions;
	}

	private static final Logger _log = Logs.getLogger(ExtensionPhase.class);

	private ServiceTracker<Extension, ExtendedExtensionDTO> _extensionTracker;
	private final SortedSet<ServiceReference<Extension>> _references = new ConcurrentSkipListSet<>();

	private class ExtensionPhaseCustomizer implements ServiceTrackerCustomizer<Extension, ExtendedExtensionDTO> {

		@Override
		public ExtendedExtensionDTO addingService(ServiceReference<Extension> reference) {
			_references.add(reference);

			ExtendedExtensionTemplateDTO template = templates().stream().map(
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

				snapshots().remove(snapshot);
				containerState.bundleContext().ungetService(snapshot.serviceReference);
			}

			ExtendedExtensionDTO extensionDTO = new ExtendedExtensionDTO();

			extensionDTO.extension = containerState.bundleContext().getService(reference);
			extensionDTO.service = refDTO(reference);
			extensionDTO.serviceReference = reference;
			extensionDTO.template = template;

			snapshots().add(extensionDTO);
			containerState.incrementChangeCount();

			if (snapshots().size() == templates().size()) {
				next.ifPresent(
					next -> submit(next::close).then(
						s -> {
							return submit(next::open).then(
								s2 -> {
									_log.debug(l -> l.debug("CDIe - Extension open TRACKING {} on {}", reference, bundle()));

									return s2;
								},
								f -> {
									_log.error(l -> l.error("CDIe - Error in extension open TRACKING {} on {}", reference, bundle()));

									error(f.getFailure());
								}
							);
						},
						f -> {
							_log.error(l -> l.error("CDIe - Error extension close TRACKING {} on {}", reference, bundle()));

							error(f.getFailure());
						}
					)
				);
			}

			return extensionDTO;
		}

		private ServiceReferenceDTO refDTO(ServiceReference<Extension> reference) {
			ServiceReferenceDTO[] refDTOs = reference.getBundle().adapt(ServiceReferenceDTO[].class);

			return Arrays.stream(refDTOs).filter(
				dto -> dto.id == id(reference)
			).findFirst().get();
		}

		private long id(ServiceReference<Extension> reference) {
			return (Long)reference.getProperty(Constants.SERVICE_ID);
		}

		@Override
		public void modifiedService(ServiceReference<Extension> reference, ExtendedExtensionDTO extentionDTO) {
			removedService(reference, extentionDTO);
			addingService(reference);
		}

		@Override
		public void removedService(ServiceReference<Extension> reference, final ExtendedExtensionDTO extensionDTO) {
			_references.remove(reference);
			containerState.bundleContext().ungetService(reference);

			if (!snapshots().removeIf(snap -> ((ExtendedExtensionDTO)snap).serviceReference.equals(reference))) {
				return;
			}

			_references.stream().filter(
				ref -> ((ExtendedExtensionTemplateDTO)extensionDTO.template).filter.match(ref)
			).findFirst().ifPresent(
				ref -> {
					ExtendedExtensionDTO replacement = new ExtendedExtensionDTO();

					replacement.extension = containerState.bundleContext().getService(ref);
					replacement.service = refDTO(ref);
					replacement.serviceReference = ref;
					replacement.template = extensionDTO.template;

					snapshots().add(replacement);
				}
			);

			containerState.incrementChangeCount();

			next.ifPresent(
				next -> submit(next::close).then(
					s -> {
						if (snapshots().size() == templates().size()) {
							return submit(next::open).then(
								s2 -> {
									_log.debug(l -> l.debug("CDIe - Extension open TRACKING {} on {}", reference, bundle()));

									return s2;
								},
								f -> {
									_log.error(l -> l.error("CDIe - Error in extension open TRACKING {} on {}", reference, bundle()));

									error(f.getFailure());
								}
							);
						}

						return s;
					},
					f -> {
						_log.error(l -> l.error("CDIe - Error extension close TRACKING {} on {}", reference, bundle()));

						error(f.getFailure());
					}
				)
			);
		}

	}

}
