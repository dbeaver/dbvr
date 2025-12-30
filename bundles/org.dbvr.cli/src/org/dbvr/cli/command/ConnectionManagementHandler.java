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
package org.dbvr.cli.command;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.app.DBPDataSourceRegistry;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.cli.CLIConstants;
import org.jkiss.dbeaver.model.cli.CLIException;
import org.jkiss.dbeaver.model.cli.CLIProcessResult;
import org.jkiss.dbeaver.model.cli.CLIUtils;
import org.jkiss.dbeaver.model.cli.model.CommandLineWithAuth;
import org.jkiss.dbeaver.model.cli.model.option.ConnectionAuthOptions;
import org.jkiss.dbeaver.model.cli.model.option.ConnectionOptions;
import org.jkiss.dbeaver.model.cli.model.option.ProjectOption;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.registry.DataSourceConfigurationManagerBuffer;
import org.jkiss.dbeaver.registry.DataSourceRegistry;
import org.jkiss.utils.CommonUtils;
import picocli.CommandLine;

import java.nio.charset.StandardCharsets;

@CommandLine.Command(name = "connections", description = "Connection management")
public class ConnectionManagementHandler extends CommandLineWithAuth {
    @CommandLine.Mixin
    private ProjectOption projectOption;

    @CommandLine.ArgGroup(exclusive = true, multiplicity = "1")
    private Actions actions;

    private static class UpdateAction {
        @CommandLine.Option(names = "--update", required = true, arity = "1", description = "Connection ID or name")
        private String datasourceIdOrName;
    }

    private static class UpsertMode {
        @CommandLine.Option(names = "--create", description = "Create connection")
        private boolean create;

        @CommandLine.Option(names = "--update", description = "Update connection by ID or name")
        private String updateDataSourceIdOrName;
    }

    private static class UpsertAction {
        @CommandLine.ArgGroup(exclusive = true, multiplicity = "1")
        private UpsertMode mode;
        @CommandLine.ArgGroup(exclusive = false)
        private ConnectionOptions connectionOptions;
        @CommandLine.ArgGroup(exclusive = false)
        private ConnectionAuthOptions authOptions;
    }


    private static class Actions {
        @CommandLine.ArgGroup(exclusive = false)
        private UpsertAction upsert;

        @CommandLine.Option(names = "--list", description = "List connections")
        private boolean list;

        @CommandLine.Option(names = "--delete", arity = "1", description = "Delete connection by ID or name")
        private String deleteDataSourceIdOrName;
    }

    @Override
    public void run() throws CLIException {
        super.run();
        DBPProject project = CLIUtils.findProject(projectOption == null ? null : projectOption.getProjectIdOrName(), context());
        if (actions.list) {
            listConnections(project);
        } else if (actions.upsert != null) {
            if (actions.upsert.mode.create) {
                createConnection(project);
            } else if (CommonUtils.isNotEmpty(actions.upsert.mode.updateDataSourceIdOrName)) {
                updateConnection(project);
            }
        } else if (CommonUtils.isNotEmpty(actions.deleteDataSourceIdOrName)) {
            deleteConnection(project);
        } else {
            listConnections(project);
        }
    }

    private void listConnections(@NotNull DBPProject project) throws CLIException {
        context().setPostAction(CLIProcessResult.PostAction.SHUTDOWN);
        context().addResult(serializeDataSources(project, null));
    }

    private void createConnection(@NotNull DBPProject project) throws CLIException {
        DBPDataSourceContainer dataSourceContainer = CLIUtils.createDataSource(
            project,
            actions.upsert.connectionOptions,
            actions.upsert.authOptions,
            false
        );


        context().setPostAction(CLIProcessResult.PostAction.SHUTDOWN);
        context().addResult(serializeDataSources(project, dataSourceContainer.getId()));
    }

    private void updateConnection(@NotNull DBPProject project) throws CLIException {
        DBPDataSourceContainer dataSourceContainer = CLIUtils.findDataSource(
            project,
            actions.upsert.mode.updateDataSourceIdOrName
        );

        CLIUtils.updateDataSource(
            actions.upsert.connectionOptions,
            actions.upsert.authOptions,
            dataSourceContainer
        );
        CLIUtils.updateConnectionConfiguration(
            actions.upsert.connectionOptions,
            dataSourceContainer.getConnectionConfiguration()
        );

        try {
            var registry = project.getDataSourceRegistry();
            registry.updateDataSource(dataSourceContainer);
            registry.checkForErrors();
        } catch (Exception e) {
            throw new CLIException(
                "Error updating connection: " + e.getMessage(),
                e,
                CLIConstants.EXIT_CODE_ERROR
            );
        }

        context().setPostAction(CLIProcessResult.PostAction.SHUTDOWN);
        context().addResult(serializeDataSources(project, dataSourceContainer.getId()));
    }

    private void deleteConnection(@NotNull DBPProject project) throws CLIException {
        DBPDataSourceContainer container = CLIUtils.findDataSource(project, actions.deleteDataSourceIdOrName);

        project.getDataSourceRegistry().removeDataSource(
            container
        );
        context().setPostAction(CLIProcessResult.PostAction.SHUTDOWN);
        context().addResult(
            "Connection " + container.getName() + "[" + container.getId() + "] has been deleted");
    }

    private String serializeDataSources(@NotNull DBPProject project, @Nullable String dsId) throws CLIException {
        DataSourceConfigurationManagerBuffer buffer = new DataSourceConfigurationManagerBuffer();
        DBPDataSourceRegistry registry = project.getDataSourceRegistry();
        if (!(registry instanceof DataSourceRegistry<?> dataSourceRegistry)) {
            throw new CLIException(
                "Unsupported data source registry: " + registry.getClass().getName(),
                CLIConstants.EXIT_CODE_ERROR
            );
        }

        dataSourceRegistry.saveConfigurationToManager(
            new VoidProgressMonitor(),
            buffer,
            (container) -> dsId == null || container.getId().equals(dsId)
        );
        try {
            dataSourceRegistry.checkForErrors();
        } catch (Exception e) {
            throw new CLIException("Error reading connections: " + e.getMessage(), e, CLIConstants.EXIT_CODE_ERROR);
        }

        return new String(buffer.getData(), StandardCharsets.UTF_8);
    }
}
