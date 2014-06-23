/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.aries.blueprint.itests;

import static org.apache.aries.blueprint.itests.Helper.mvnBundle;
import static org.junit.Assert.assertNotNull;

import java.util.Hashtable;

import org.junit.Test;
import org.ops4j.pax.exam.Option;
import org.osgi.framework.Bundle;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

public class BlueprintContainerTest extends AbstractBlueprintIntegrationTest {

    @Test
    public void test() throws Exception {
        // Create a config to check the property placeholder
        ConfigurationAdmin ca = context().getService(ConfigurationAdmin.class);
        Configuration cf = ca.getConfiguration("blueprint-sample-placeholder", null);
        Hashtable<String, String> props = new Hashtable<String, String>();
        props.put("key.b", "10");
        cf.update(props);

        Bundle bundle = context().getBundleByName("org.apache.aries.blueprint.sample");
        assertNotNull(bundle);

        bundle.start();
        
        // do the test
        Helper.testBlueprintContainer(context(), bundle);
    }

    @org.ops4j.pax.exam.Configuration
    public Option[] configuration() {
        return new Option[] {
            baseOptions(),
            Helper.blueprintBundles(),
            mvnBundle("org.apache.aries.blueprint", "org.apache.aries.blueprint.sample", false),
        };
    }

}