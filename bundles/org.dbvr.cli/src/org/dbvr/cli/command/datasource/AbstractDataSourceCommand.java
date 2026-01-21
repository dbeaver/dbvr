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
package org.dbvr.cli.command.datasource;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.app.DBPDataSourceRegistry;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.cli.*;
import org.jkiss.dbeaver.model.cli.model.option.ProjectOption;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.registry.DataSourceConfigurationManagerBuffer;
import org.jkiss.dbeaver.registry.DataSourceRegistry;
import picocli.CommandLine;

import java.nio.charset.StandardCharsets;

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
    protected String serializeDataSources(@NotNull DBPProject project, @Nullable String dsId) throws CLIException {
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

    @Override
    public void run() throws CLIException {
        
    }

    @NotNull
    protected CommandLineContext context() {
        return parent.context();
    }
}
