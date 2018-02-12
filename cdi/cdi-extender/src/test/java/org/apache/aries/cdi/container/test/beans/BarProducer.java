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

package org.apache.aries.cdi.container.test.beans;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.*;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.math.BigDecimal;

import javax.enterprise.inject.Produces;
import javax.inject.Qualifier;

import org.osgi.service.cdi.annotations.Bundle;
import org.osgi.service.cdi.annotations.ComponentPropertyType;
import org.osgi.service.cdi.annotations.Reference;
import org.osgi.service.cdi.annotations.Service;

public class BarProducer {
	@Produces
	@Service
	public Bar getBar(@Reference Bar bar) {
		return bar;
	}

	@Produces
	@Service(Integer.class)
	@Bundle
	@BarProducer.ServiceRanking(100)
	Number fum = new BigDecimal(25);

	@Qualifier @Retention(RUNTIME) @Target(FIELD)
	@ComponentPropertyType
	public static @interface ServiceRanking {
		int value();
	}

}