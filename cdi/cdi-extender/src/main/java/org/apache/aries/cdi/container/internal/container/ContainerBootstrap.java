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

package org.apache.aries.cdi.container.internal.container;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.Extension;

import org.apache.aries.cdi.container.internal.model.ExtendedExtensionDTO;
import org.apache.aries.cdi.container.internal.phase.Phase;
import org.apache.aries.cdi.container.internal.util.Logs;
import org.jboss.weld.bootstrap.WeldBootstrap;
import org.jboss.weld.bootstrap.spi.BeanDeploymentArchive;
import org.jboss.weld.bootstrap.spi.Deployment;
import org.jboss.weld.bootstrap.spi.Metadata;
import org.jboss.weld.util.ServiceLoader;
import org.osgi.service.log.Logger;

public class ContainerBootstrap extends Phase {

	public ContainerBootstrap(ContainerState containerState) {
		super(containerState, null);
	}

	@Override
	public boolean close() {
		try {
			if (_bootstrap != null) {
				_bootstrap.shutdown();
			}

			return true;
		}
		catch (Throwable t) {
			_log.error(l -> l.error("CCR Failure in container bootstrap shutdown on {}", _bootstrap, t));

			return false;
		}
	}

	@Override
	public boolean open() {
		_externalExtensions = containerState.containerDTO().extensions.stream().map(
			e -> (ExtendedExtensionDTO)e
		).map(
			e -> new ExtensionMetadata(e.extension, e.template.serviceFilter)
		).collect(Collectors.toList());

		// Add the internal extensions
		_extensions.add(
			new ExtensionMetadata(
				new BundleContextExtension(containerState.bundleContext()),
				containerState.id()));
		_extensions.add(
			new ExtensionMetadata(
				new RuntimeExtension(containerState),
				containerState.id()));
		_extensions.add(
			new ExtensionMetadata(
				new LoggerExtension(containerState),
				containerState.id()));

		// Add extensions found from the bundle's classloader, such as those in the Bundle-ClassPath
		for (Metadata<Extension> meta : ServiceLoader.load(Extension.class, containerState.classLoader())) {
			_extensions.add(meta);
		}

		// Add external extensions
		for (Metadata<Extension> meta : _externalExtensions) {
			_extensions.add(meta);
		}

		_bootstrap = new WeldBootstrap();

		BeanDeploymentArchive beanDeploymentArchive = new ContainerDeploymentArchive(
			containerState.loader(),
			containerState.id(),
			containerState.beansModel().getBeanClassNames(),
			containerState.beansModel().getBeansXml());

		Deployment deployment = new ContainerDeployment(_extensions, beanDeploymentArchive);

		_bootstrap.startExtensions(_extensions);
		_bootstrap.startContainer(containerState.id(), new ContainerEnvironment(), deployment);

		_beanManager = _bootstrap.getManager(beanDeploymentArchive);

		_bootstrap.startInitialization();
		_bootstrap.deployBeans();

		return true;
	}

	public BeanManager getBeanManager() {
		return _beanManager;
	}

	public WeldBootstrap getBootstrap() {
		return _bootstrap;
	}

	private static final Logger _log = Logs.getLogger(ContainerBootstrap.class);

	private volatile BeanManager _beanManager;
	private volatile WeldBootstrap _bootstrap;
	private final List<Metadata<Extension>> _extensions = new CopyOnWriteArrayList<>();
	private volatile Collection<Metadata<Extension>> _externalExtensions;

}