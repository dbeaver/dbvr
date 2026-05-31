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
package org.dbvr.cli.sql.meta;

import org.dbvr.cli.sql.CLIConnectionUtils;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.cli.CLIAbstractSubcommand;
import org.jkiss.dbeaver.model.cli.CLIConstants;
import org.jkiss.dbeaver.model.cli.CLIException;
import org.jkiss.dbeaver.model.cli.model.DataSourceUpdater;
import org.jkiss.dbeaver.model.cli.model.option.CreateDataSourceOptions;
import org.jkiss.dbeaver.model.cli.model.option.DataSourceAuthOptions;
import org.jkiss.dbeaver.model.cli.model.option.ProjectOption;
import org.jkiss.utils.CommonUtils;
import picocli.CommandLine;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractMetaCommand extends CLIAbstractSubcommand {

    private final Log log = Log.getLog(AbstractMetaCommand.class);

    @CommandLine.Mixin
    private ProjectOption projectOption;

    @CommandLine.Mixin
    private DataSourceAuthOptions authOptions;

    @CommandLine.ArgGroup(exclusive = true, multiplicity = "1")
    protected CreateOrFindDataSource dataSourceOptions;

    protected static class CreateOrFindDataSource {
        @CommandLine.ArgGroup(
            exclusive = false
        )
        protected CreateDataSourceOptions tempDataSourceOptions;

        @CommandLine.Option(names = {"-ds", "--datasource"}, arity = "1", description = "DataSource ID or name")
        protected String existDataSourceIdOrName;

        @CommandLine.Option(names = {"-con", "-connect", "-ds-spec", "--datasource-specification"}, arity = "1")
        protected String connectionSpec;
    }

    @NotNull
    public DBPDataSource connectDataSource() throws DBException {
        if (dataSourceOptions == null) {
            throw new CLIException("No datasource options provided", CLIConstants.EXIT_CODE_ILLEGAL_ARGUMENTS);
        }
        CLIConnectionUtils.connect(
            dataSourceOptions.existDataSourceIdOrName,
            dataSourceOptions.tempDataSourceOptions,
            dataSourceOptions.connectionSpec,
            getDataSourceUpdaters(),
            projectOption.getProjectIdOrName(),
            context(),
            log
        );

        DBPDataSourceContainer container = context().getContextParameter(DBPDataSourceContainer.class.getName());
        if (container == null) {
            throw new CLIException("Can't connect to datasource", CLIConstants.EXIT_CODE_ERROR);
        }
        DBPDataSource dataSource = container.getDataSource();
        if (dataSource == null) {
            throw new CLIException("Can't connect to datasource", CLIConstants.EXIT_CODE_ERROR);
        }
        return dataSource;
    }

    @NotNull
    protected List<DataSourceUpdater> getDataSourceUpdaters() {
        List<DataSourceUpdater> updaters = new ArrayList<>();
        if (!CommonUtils.isEmpty(spec.mixins())) {
            for (CommandLine.Model.CommandSpec mixin : spec.mixins().values()) {
                if (mixin.userObject() instanceof DataSourceUpdater mixinUpdater) {
                    updaters.add(mixinUpdater);
                }
            }
        }
        if (dataSourceOptions != null && dataSourceOptions.tempDataSourceOptions != null) {
            updaters.add(dataSourceOptions.tempDataSourceOptions);
        }
        return updaters;
    }
}
