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
package org.dbvr.cli.command.datasource;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.app.DBPDataSourceRegistry;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.cli.*;
import org.jkiss.dbeaver.model.cli.model.option.ProjectOption;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.data.json.JSONUtils;
import org.jkiss.dbeaver.model.net.DBWHandlerConfiguration;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.registry.DataSourceConfigurationManagerBuffer;
import org.jkiss.dbeaver.registry.DataSourceDescriptor;
import org.jkiss.dbeaver.registry.DataSourceRegistry;
import picocli.CommandLine;

import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

public abstract class AbstractDataSourceCommand extends AbstractCommandLineParameterHandler {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    @CommandLine.Mixin
    protected ProjectOption projectOption;
    /**
     * Root command uses as context
     */
    @CommandLine.ParentCommand
    private DataSourceManagementHandler parent;

    protected DBPProject getProject() throws CLIException {
        return CLIUtils.findProject(projectOption.getProjectIdOrName(), context());
    }

    @NotNull
    protected String serializeDataSourceList(@NotNull DBPProject project) {
        List<Map<String, String>> tableData = new ArrayList<>();
        for (DBPDataSourceContainer dataSource : project.getDataSourceRegistry().getDataSources()) {
            Map<String, String> row = new LinkedHashMap<>();
            row.put("ID", dataSource.getId());
            row.put("NAME", dataSource.getName());
            row.put("DRIVER", dataSource.getDriver().getId());
            tableData.add(row);
        }
        return CLIUtils.formatAsTable(tableData);
    }

    @NotNull
    protected String serializeDataSourceToJson(@NotNull DBPProject project, @NotNull String dsId) throws CLIException {
        DBPDataSourceRegistry registry = project.getDataSourceRegistry();
        if (!(registry instanceof DataSourceRegistry<?> dataSourceRegistry)) {
            throw new CLIException(
                "Incorrect data source registry: " + registry.getClass().getName(),
                CLIConstants.EXIT_CODE_ERROR
            );
        }
        DBPDataSourceContainer dataSource = dataSourceRegistry.getDataSource(dsId);
        if (!(dataSource instanceof DataSourceDescriptor dataSourceDescriptor)) {
            throw new CLIException("Datasource with id " + dsId + " is not found", CLIConstants.EXIT_CODE_ERROR);
        }

        DataSourceDescriptor copy = (DataSourceDescriptor) dataSourceDescriptor.createCopy(dataSourceRegistry);
        wipeCredentials(copy);

        try {
            dataSourceRegistry.addDataSourceToList(copy);
            try {
                DataSourceConfigurationManagerBuffer buffer = new DataSourceConfigurationManagerBuffer();
                dataSourceRegistry.saveConfigurationToManager(
                    new VoidProgressMonitor(),
                    buffer,
                    (container) -> container == copy || container.getId().equals(copy.getId())
                );
                String json = new String(buffer.getData(), StandardCharsets.UTF_8);
                try (StringReader reader = new StringReader(json)) {
                    Map<String, Object> map = JSONUtils.parseMap(GSON, reader);
                    Map<String, Object> connections = JSONUtils.deserializeProperties(map, "connections");
                    Object dsData = connections != null ? connections.get(copy.getId()) : null;

                    if (dsData instanceof Map) {
                        return GSON.toJson(dsData);
                    }
                }
                throw new CLIException("Datasource data is not found in serialized output", CLIConstants.EXIT_CODE_ERROR);
            } finally {
                dataSourceRegistry.removeDataSourceFromList(copy);
            }
        } catch (Exception e) {
            throw new CLIException("Error serializing datasource: " + e.getMessage(), e, CLIConstants.EXIT_CODE_ERROR);
        }
    }

    private void wipeCredentials(@NotNull DataSourceDescriptor dataSource) {
        DBPConnectionConfiguration connectionConfig = dataSource.getConnectionConfiguration();
        connectionConfig.setUserName(null);
        connectionConfig.setUserPassword(null);
        connectionConfig.setAuthProperties(Collections.emptyMap());

        for (DBWHandlerConfiguration handler : connectionConfig.getHandlers()) {
            handler.setUserName(null);
            handler.setPassword(null);
            handler.setSecureProperties(Collections.emptyMap());
        }
    }

    @Override
    public void run() throws CLIException {
        
    }

    @NotNull
    protected CLIContext context() {
        return parent.context();
    }
}
