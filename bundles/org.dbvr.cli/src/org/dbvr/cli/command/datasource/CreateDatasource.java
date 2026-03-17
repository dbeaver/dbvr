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
import org.jkiss.dbeaver.model.cli.CLIException;
import org.jkiss.dbeaver.model.cli.CLIProcessResult;
import org.jkiss.dbeaver.model.cli.CLIUtils;
import org.jkiss.dbeaver.model.cli.model.DataSourceUpdater;
import org.jkiss.dbeaver.model.cli.model.option.CreateDataSourceOptions;
import picocli.CommandLine;

import java.util.List;

@CommandLine.Command(name = "create", description = "Create datasource")
public class CreateDatasource extends AbstractDataSourceEditCommand {
    @CommandLine.Mixin
    private CreateDataSourceOptions createOptions;

    @Override
    public void run() throws CLIException {
        super.run();
        DBPProject project = getProject();
        DBPDataSourceContainer dataSourceContainer = CLIUtils.createDataSource(
            project,
            createOptions.getDriver(),
            createOptions.getDataSourceOptions(),
            getDataSourceUpdaters(),
            false
        );
        context().setPostAction(CLIProcessResult.PostAction.SHUTDOWN);
        context().addResult(dataSourceContainer.getId());
    }

    @NotNull
    @Override
    protected List<DataSourceUpdater> getDataSourceUpdaters() {
        var updaters = super.getDataSourceUpdaters();
        updaters.add(dataSource -> CLIUtils.updateConnectionConfiguration(
            createOptions.getDataSourceOptions(),
            dataSource.getConnectionConfiguration()
        ));
        return updaters;
    }
}
