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
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.app.DBPDataSourceRegistry;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.cli.*;
import org.jkiss.dbeaver.model.cli.model.option.ProjectOption;
import org.jkiss.dbeaver.model.data.json.JSONUtils;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.registry.DataSourceConfigurationManagerBuffer;
import org.jkiss.dbeaver.registry.DataSourceRegistry;
import picocli.CommandLine;

import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public abstract class AbstractDataSourceCommand extends AbstractCommandLineParameterHandler {
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
        String allData = serializeDataSources(project, dsId);
        try {
            Map<String, Object> map = JSONUtils.parseMap(new Gson(), new StringReader(allData));
            Map<String, Object> connections = JSONUtils.deserializeProperties(map, "connections");
            Object dsData = connections != null ? connections.get(dsId) : null;
            if (dsData instanceof Map) {
                return new GsonBuilder().setPrettyPrinting().create().toJson(dsData);
            }
            throw new CLIException("Datasource with id " + dsId + " is not found in serialized data", CLIConstants.EXIT_CODE_ERROR);
        } catch (Exception e) {
            throw new CLIException("Error parsing datasource JSON: " + e.getMessage(), e, CLIConstants.EXIT_CODE_ERROR);
        }
    }

    private String serializeDataSources(@NotNull DBPProject project, @Nullable String dsId) throws CLIException {
        DataSourceConfigurationManagerBuffer buffer = new DataSourceConfigurationManagerBuffer() {
            @Override
            public boolean isTrusted() {
                return false;
            }
        };
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
            throw new CLIException("Error reading datasources: " + e.getMessage(), e, CLIConstants.EXIT_CODE_ERROR);
        }

        return new String(buffer.getData(), StandardCharsets.UTF_8);
    }

    @Override
    public void run() throws CLIException {
        
    }

    @NotNull
    protected CLIContext context() {
        return parent.context();
    }
}
