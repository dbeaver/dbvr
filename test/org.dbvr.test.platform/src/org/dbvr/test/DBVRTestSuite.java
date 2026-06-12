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

import org.dbvr.cli.app.ce.CLIApplicationCE;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;


@Suite
@SelectClasses({
    HelpArgTest.class,
    DataSourceManagementTest.class,
    ProjectManagementTest.class,
    AuthModelsTest.class,
    InjectTest.class,
    MetaLocalCommandTest.class
})
public class DBVRTestSuite extends DBVRTest {
    private static CLIApplicationCE applicationCE;

    public static void initApplication() throws Exception {
        System.out.println("Start CLI Application");
        if (DBWorkbench.isPlatformStarted()) {
            applicationCE = (CLIApplicationCE) DBWorkbench.getPlatform().getApplication();
            return;
        }
        applicationCE = new CLIApplicationCE();
        Thread thread = new Thread(
            () -> {
                try {
                    applicationCE.start(null);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        );
        thread.start();

        long startTime = System.currentTimeMillis();
        long endTime = 0;
        boolean setUpIsDone = false;
        while (!setUpIsDone && endTime < 300000) {
            setUpIsDone = applicationCE.isStarted();
            endTime = System.currentTimeMillis() - startTime;
            Thread.sleep(100);
        }
        if (!setUpIsDone) {
            throw new Exception("Application is not running");
        }
    }

    @NotNull
    public static CLIApplicationCE getApplication() throws DBException {
        if (applicationCE == null) {
            // running inside OSGi: the runner already started the app, read it from the platform
            if (DBWorkbench.isPlatformStarted()
                && DBWorkbench.getPlatform().getApplication() instanceof CLIApplicationCE ce) {
                applicationCE = ce;
            }
            if (applicationCE == null) {
                throw new DBException("Application is not running");
            }
        }

        return applicationCE;
    }
}
