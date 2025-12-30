/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2025 DBeaver Corp and others
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
package org.dbvr.cli.sql;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.cli.CLIConstants;
import org.jkiss.dbeaver.model.cli.CLIException;
import org.jkiss.dbeaver.model.cli.CLIUtils;
import org.jkiss.dbeaver.model.cli.CommandLineContext;
import org.jkiss.dbeaver.model.cli.model.option.ConnectionAuthOptions;
import org.jkiss.dbeaver.model.cli.model.option.ConnectionOptions;
import org.jkiss.dbeaver.model.runtime.LoggingProgressMonitor;
import org.jkiss.utils.CommonUtils;

public class CLIConnectionUtils {

    public static void connect(
        @Nullable String existConnectionIdOrName,
        @Nullable ConnectionOptions tempConnectionOptions,
        @NotNull ConnectionAuthOptions authOptions,
        @Nullable String projectIdOrName,
        @NotNull CommandLineContext context,
        @NotNull Log parentLog
    )
    throws CLIException {
        DBPDataSourceContainer dataSourceContainer;
        DBPProject project = CLIUtils.findProject(projectIdOrName, context);
        if (CommonUtils.isNotEmpty(existConnectionIdOrName)) {
            dataSourceContainer = CLIUtils.findDataSource(
                project,
                existConnectionIdOrName
            );
            CLIUtils.processDataSourceAuthOptions(dataSourceContainer, authOptions);
        } else if (tempConnectionOptions != null) {
            dataSourceContainer = CLIUtils.createTempDataSource(
                project,
                tempConnectionOptions,
                authOptions
            );
        } else {
            throw new CLIException("No connection options provided", CLIConstants.EXIT_CODE_ILLEGAL_ARGUMENTS);
        }

        var monitor = new LoggingProgressMonitor(parentLog);
        connectDatasource(dataSourceContainer, parentLog);
        context.setContextParameter(DBPDataSourceContainer.class.getName(), dataSourceContainer);
        context.addCloseHandler(() -> {
            if (dataSourceContainer.isConnected()) {
                try {
                    dataSourceContainer.disconnect(monitor);
                } catch (Exception e) {
                    parentLog.error("Error disconnecting datasource", e);
                }
            }
        });
    }


    protected static void connectDatasource(
        @NotNull DBPDataSourceContainer dataSource,
        @NotNull Log log
    ) throws CLIException {
        if (!dataSource.isConnected()) {
            try {
                dataSource.connect(new LoggingProgressMonitor(log), true, true);
            } catch (DBException e) {
                throw new CLIException(
                    "Failed to connect to database '" + dataSource.getName() + "': " + e.getMessage(),
                    e,
                    CLIConstants.EXIT_CODE_ERROR
                );
            }
        }
    }

    @NotNull
    public static DBPDataSourceContainer findDataSource(
        @Nullable String projectIdOrName,
        @NotNull String connectionIdOrName,

        @NotNull CommandLineContext context
    ) throws CLIException {
        DBPProject project = CLIUtils.findProject(projectIdOrName, context);
        return CLIUtils.findDataSource(
            project,
            connectionIdOrName
        );
    }

}
