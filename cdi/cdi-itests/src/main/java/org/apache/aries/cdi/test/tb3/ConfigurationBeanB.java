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

package org.apache.aries.cdi.test.tb3;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.*;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.Map;
import java.util.concurrent.Callable;

import javax.inject.Inject;

import org.apache.aries.cdi.test.interfaces.BeanService;
import org.osgi.service.cdi.ConfigurationPolicy;
import org.osgi.service.cdi.annotations.ComponentPropertyType;
import org.osgi.service.cdi.annotations.Configuration;
import org.osgi.service.cdi.annotations.PID;
import org.osgi.service.cdi.annotations.Service;
import org.osgi.service.cdi.annotations.SingleComponent;

@ConfigurationBeanB.Props
@PID(value = "configurationBeanA", policy = ConfigurationPolicy.REQUIRED)
@PID(policy = ConfigurationPolicy.REQUIRED)
@Service(BeanService.class)
@SingleComponent
public class ConfigurationBeanB implements BeanService<Callable<int[]>> {

	@Retention(RUNTIME) @Target(TYPE )
	@ComponentPropertyType
	public @interface Props {
		String bean() default "B";
	}

	@Override
	public String doSomething() {
		return (String)config.get("color");
	}

	@Override
	public Callable<int[]> get() {
		return new Callable<int[]>() {
			@Override
			public int[] call() throws Exception {
				return (int[])config.get("ports");
			}
		};
	}

	@Configuration
	@Inject
	Map<String, Object> config;

}
