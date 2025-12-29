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

import org.dbvr.cli.model.ConnectionOptions;
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
import org.jkiss.dbeaver.model.cli.model.option.ProjectOption;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.registry.DataSourceConfigurationManagerBuffer;
import org.jkiss.dbeaver.registry.DataSourceRegistry;
import org.jkiss.dbeaver.registry.DataSourceUtils;
import picocli.CommandLine;

import java.nio.charset.StandardCharsets;

@CommandLine.Command(name = "connections", description = "Connection management")
public class ConnectionManagementHandler extends CommandLineWithAuth {
    @CommandLine.Mixin
    private ProjectOption projectOption;

    @CommandLine.ArgGroup(exclusive = true, multiplicity = "1")
    private Actions actions;

    private static class DeleteAction {
        @CommandLine.Option(names = "--delete", required = true, arity = "1", description = "Connection name or ID")
        private String datasourceIdOrName;
    }

    private static class CreateAction {
        @CommandLine.Option(names = "--create", required = true, description = "Create connection")
        private boolean create;
        @CommandLine.ArgGroup(exclusive = false)
        private ConnectionOptions connectionOptions;
    }

    private static class UpdateAction {
        @CommandLine.Option(names = "--update", required = true, description = "Update connection")
        private boolean create;
        @CommandLine.ArgGroup(exclusive = false)
        private ConnectionOptions connectionOptions;
    }

    private static class Actions {
        @CommandLine.ArgGroup(exclusive = false)
        private CreateAction create;

        //        @CommandLine.ArgGroup(exclusive = false)
        private UpdateAction updateAction;

        @CommandLine.Option(names = "--list", description = "List connections")
        private boolean list;

        @CommandLine.ArgGroup(exclusive = false)
        private DeleteAction delete;
    }

    @Override
    public void run() throws CLIException {
        super.run();
        DBPProject project = CLIUtils.findProject(projectOption == null ? null : projectOption.getProjectIdOrName(), context());
        if (actions == null || actions.list) {
            listConnections(project);
        } else if (actions.create != null) {
            createConnection(project);
        } else if (actions.updateAction != null) {
            updateConnection(project);
        } else if (actions.delete != null) {
            deleteConnection(project);
        }
    }

    private void listConnections(@NotNull DBPProject project) throws CLIException {
        context().setPostAction(CLIProcessResult.PostAction.SHUTDOWN);
        context().addResult(serializeDataSources(project, null));
    }

    private void createConnection(@NotNull DBPProject project) throws CLIException {
        String spec = actions.create.connectionOptions.getConnectionSpec().trim();
        spec = spec + "|" + DataSourceUtils.PARAM_SAVE + "=true";
        DBPDataSourceContainer container = CLIUtils.findDataSource(
            project,
            spec
        );
        if (container == null) {
            throw new CLIException(
                "Can't create connection by spec: " + spec,
                CLIConstants.EXIT_CODE_ILLEGAL_ARGUMENTS
            );
        }

        context().setPostAction(CLIProcessResult.PostAction.SHUTDOWN);
        context().addResult(serializeDataSources(project, container.getId()));
    }

    private void updateConnection(@NotNull DBPProject project) throws CLIException {
        String spec = actions.updateAction.connectionOptions.getConnectionSpec();
        DBPDataSourceContainer container = CLIUtils.findDataSource(
            project,
            spec
        );
        if (container == null) {
            throw new CLIException(
                "Can't update connection by spec: " + spec,
                CLIConstants.EXIT_CODE_ILLEGAL_ARGUMENTS
            );
        }
        if (container.isTemporary()) {
            throw new CLIException(
                "No existing connection found by spec: " + spec,
                CLIConstants.EXIT_CODE_ILLEGAL_ARGUMENTS
            );
        }
        try {
            var registry = project.getDataSourceRegistry();
            registry.updateDataSource(container);
            registry.checkForErrors();
        } catch (Exception e) {
            throw new CLIException(
                "Error updating connection: " + e.getMessage(),
                e,
                CLIConstants.EXIT_CODE_ERROR
            );
        }

        context().setPostAction(CLIProcessResult.PostAction.SHUTDOWN);
        context().addResult(serializeDataSources(project, container.getId()));
    }

    private void deleteConnection(@NotNull DBPProject project) throws CLIException {
        DataSourceRegistry<?> registry = (DataSourceRegistry<?>) project.getDataSourceRegistry();
        DBPDataSourceContainer container =
            registry.getDataSource(actions.delete.datasourceIdOrName);
        if (container == null) {
            container = registry.findDataSourceByName(actions.delete.datasourceIdOrName);
        }
        if (container == null) {
            throw new CLIException(
                "Can't find connection " + actions.delete.datasourceIdOrName + " in project " + project.getName(),
                CLIConstants.EXIT_CODE_ILLEGAL_ARGUMENTS
            );
        }
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
