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

import java.util.Collections;
import java.util.List;

import javax.enterprise.inject.spi.Extension;

import org.apache.aries.cdi.container.internal.component.DiscoveryExtension;
import org.apache.aries.cdi.container.internal.model.BeansModel;
import org.jboss.weld.bootstrap.WeldBootstrap;
import org.jboss.weld.bootstrap.spi.BeanDeploymentArchive;
import org.jboss.weld.bootstrap.spi.Deployment;
import org.jboss.weld.bootstrap.spi.Metadata;

public class ContainerDiscovery {

	public ContainerDiscovery(ContainerState containerState) {
		String id = containerState.id() + "-discovery";

		BeansModel beansModel = containerState.beansModel();

		BeanDeploymentArchive beanDeploymentArchive = new ContainerDeploymentArchive(
			containerState.loader(), id, beansModel.getBeanClassNames(),
			beansModel.getBeansXml());

		ExtensionMetadata extension = new ExtensionMetadata(
			new DiscoveryExtension(containerState), id);

		List<Metadata<Extension>> extensions = Collections.singletonList(extension);

		Deployment deployment = new ContainerDeployment(
			Collections.singletonList(extension), beanDeploymentArchive);

		WeldBootstrap _bootstrap = new WeldBootstrap();

		_bootstrap.startExtensions(extensions);
		_bootstrap.startContainer(id, new ContainerEnvironment(), deployment);
		_bootstrap.startInitialization();
		_bootstrap.deployBeans();
		_bootstrap.shutdown();
	}

}