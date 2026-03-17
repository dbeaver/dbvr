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

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.cli.CLIConstants;
import org.jkiss.dbeaver.model.cli.CLIException;
import org.jkiss.dbeaver.model.cli.CLIProcessResult;
import org.jkiss.dbeaver.model.cli.CLIUtils;
import org.jkiss.dbeaver.model.cli.model.DataSourceUpdater;
import org.jkiss.dbeaver.model.cli.model.option.DataSourceOptions;
import org.jkiss.utils.CommonUtils;
import picocli.CommandLine;

import java.util.List;

@CommandLine.Command(name = "update", description = "Update datasource")
public class UpdateDataSource extends AbstractDataSourceEditCommand {
    @CommandLine.Parameters(index = "0", description = "Datasource id or name", arity = "1")
    private String datasourceIdOrName;

    @CommandLine.Mixin
    private DataSourceOptions dataSourceOptions;

    @CommandLine.Option(
        names = {"-net-delete", "--network-handler-delete"},
        arity = "1",
        description = "Network handler id for deletion"
    )
    private List<String> handlersToDelete;

    @Override
    public void run() throws CLIException {
        super.run();
        DBPProject project = getProject();
        DBPDataSourceContainer dataSourceContainer = CLIUtils.findDataSource(
            project,
            datasourceIdOrName
        );

        CLIUtils.updateDataSource(
            dataSourceContainer,
            getDataSourceUpdaters()
        );

        if (!CommonUtils.isEmpty(handlersToDelete)) {
            var connectionConfig = dataSourceContainer.getConnectionConfiguration();
            for (String handlerId : handlersToDelete) {
                connectionConfig.removeHandler(handlerId);
            }
        }
        try {
            var registry = project.getDataSourceRegistry();
            registry.updateDataSource(dataSourceContainer);
            registry.checkForErrors();
        } catch (Exception e) {
            throw new CLIException(
                "Error updating datasource: " + e.getMessage(),
                e,
                CLIConstants.EXIT_CODE_ERROR
            );
        }

        context().setPostAction(CLIProcessResult.PostAction.SHUTDOWN);
        context().addResult(serializeDataSourceToJson(project, dataSourceContainer.getId()).trim());
    }

    @NotNull
    @Override
    protected List<DataSourceUpdater> getDataSourceUpdaters() {
        var updaters = super.getDataSourceUpdaters();
        updaters.add(dataSource -> CLIUtils.updateConnectionConfiguration(
            dataSourceOptions,
            dataSource.getConnectionConfiguration()
        ));
        return updaters;
    }
}
