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
import org.jkiss.dbeaver.model.app.DBPWorkspace;
import org.jkiss.dbeaver.model.cli.AbstractCommandLineParameterHandler;
import org.jkiss.dbeaver.model.cli.ApplicationInstanceServer;
import org.jkiss.dbeaver.model.cli.CLIConstants;
import org.jkiss.dbeaver.model.cli.CLIException;
import org.jkiss.dbeaver.model.cli.command.AbstractTopLevelCommand;
import org.jkiss.dbeaver.model.cli.option.FilesOptions;
import org.jkiss.dbeaver.model.runtime.LoggingProgressMonitor;
import org.jkiss.dbeaver.registry.DataSourceUtils;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.dbeaver.utils.SystemVariablesResolver;
import org.jkiss.utils.CommonUtils;
import picocli.CommandLine;

@CommandLine.Command(name = "connection-open", aliases = {"con-open"}, description = "Connects to a specified database.")
public class ConnectionOpenParameterHandler extends AbstractCommandLineParameterHandler {
    private final Log log = Log.getLog(ConnectionOpenParameterHandler.class);

    @CommandLine.Option(names = CLIConstants.PARAM_PROJECT, description = "Project name or ID")
    private String projectIdOrName;

    @CommandLine.Option(names = "-connection-spec", description = "Connection specification", required = true)
    private String connectionSpec;

    @CommandLine.Mixin
    private FilesOptions filesOptions;

    @CommandLine.ParentCommand
    private AbstractTopLevelCommand topLevelCommand;

    @Override
    public void run() {
        if (CommonUtils.isEmpty(connectionSpec)) {
            throw new CLIException("-connection-spec parameter is empty", CLIConstants.EXIT_CODE_ILLEGAL_ARGUMENTS);
        }
        DBPDataSourceContainer dataSource = findDataSource();
        if (dataSource == null) {
            throw new CLIException("Can't find connection '" + connectionSpec + "'", CLIConstants.EXIT_CODE_ILLEGAL_ARGUMENTS);
        }
        connectDatasource(dataSource);
        context().setContextParameter(DBPDataSourceContainer.class.getName(), dataSource);
        context().addCloseHandler(() -> {
            if (dataSource.isConnected()) {
                try {
                    dataSource.disconnect(new LoggingProgressMonitor(log));
                } catch (Exception e) {
                    log.error("Error disconnecting datasource", e);
                }
            }
        });
    }


    protected void connectDatasource(
        @NotNull DBPDataSourceContainer dataSource
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

    @Nullable
    protected DBPDataSourceContainer findDataSource() {
        DBPProject project = findProject();
        ApplicationInstanceServer.InstanceConnectionParameters instanceConParameters
            = new ApplicationInstanceServer.InstanceConnectionParameters();
        return DataSourceUtils.getDataSourceBySpec(
            project,
            GeneralUtils.replaceVariables(connectionSpec, SystemVariablesResolver.INSTANCE),
            instanceConParameters,
            false,
            instanceConParameters.isCreateNewConnection()
        );
    }

    @NotNull
    private DBPProject findProject() throws CLIException {
        DBPProject project;
        DBPWorkspace workspace = context().getContextParameter(DBPWorkspace.class.getName());
        if (workspace == null) {
            workspace = DBWorkbench.getPlatform().getWorkspace();
        }
        if (CommonUtils.isEmpty(projectIdOrName)) {
            project = workspace.getActiveProject();
        } else {
            project = workspace.getProject(projectIdOrName);
            if (project == null) {
                project = workspace.getProjectById(projectIdOrName);
            }
        }
        if (project == null) {
            throw new CLIException("Can't find project '" + projectIdOrName + "'", CLIConstants.EXIT_CODE_ILLEGAL_ARGUMENTS);
        }
        return project;
    }
}
