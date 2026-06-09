/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2026 DBeaver Corp and others
 *
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
package org.dbvr.test;

import org.jkiss.junit.osgi.annotation.RunWithApplication;
import org.jkiss.junit.osgi.annotation.RunWithProduct;
import org.jkiss.junit.osgi.behaviors.IAsyncApplication;
import org.jkiss.junit.osgi.extension.OSGITestExtension;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.extension.ExtendWith;

@RunWithProduct("dbvr-unittest.product")
@ExtendWith(OSGITestExtension.class)
@RunWithApplication(
    bundleName = "org.dbvr.app.ce",
    registryName = "org.dbvr.app.ce.application",
    waitForWorkbench = false,
    properties = {
        @RunWithApplication.Property(name = "osgi.instance.area", value = "./target/workpsace")
    }
)
public abstract class DBVRTest implements IAsyncApplication {

    @BeforeAll
    public static void setUpApplication() throws Exception {
        DBVRTestSuite.initApplication();
    }

    @Override
    public boolean verifyLaunched() {
        try {
            return DBVRTestSuite.getApplication().isStarted();
        } catch (Exception e) {
            return false;
        }
    }
}
