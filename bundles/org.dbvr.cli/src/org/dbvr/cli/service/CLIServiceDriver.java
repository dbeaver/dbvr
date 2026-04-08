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
package org.dbvr.cli.service;

import org.dbvr.cli.app.CLIApplicationBase;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.connection.DBPDriver;
import org.jkiss.dbeaver.model.connection.DBPDriverDependencies;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.registry.driver.DriverDescriptor;
import org.jkiss.dbeaver.registry.driver.DriverUtils;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.runtime.ui.UIServiceDrivers;

public class CLIServiceDriver implements UIServiceDrivers {
    private static final Log log = Log.getLog(CLIServiceDriver.class);

    @Override
    public boolean downloadDriverFiles(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBPDriver driver,
        @NotNull DBPDriverDependencies dependencies,
        boolean isShowExpanded
    ) {
        if (DBWorkbench.getPlatform().getApplication() instanceof CLIApplicationBase cliApp) {
            if (cliApp.isStateless()) {
                log.error("Cannot download driver in stateless mode");
                return false;
            }
        }
        if (driver instanceof DriverDescriptor driverDescriptor) {
            return DriverUtils.downloadDriverFiles(
                monitor,
                driverDescriptor,
                dependencies
            );
        }
        return false;
    }
}
