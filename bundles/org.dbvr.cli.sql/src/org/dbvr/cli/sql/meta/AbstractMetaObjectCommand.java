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
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.cli.*;
import org.jkiss.dbeaver.model.cli.model.DataSourceUpdater;
import org.jkiss.dbeaver.model.cli.model.option.CreateDataSourceOptions;
import org.jkiss.dbeaver.model.cli.model.option.DataSourceAuthOptions;
import org.jkiss.dbeaver.model.cli.model.option.ProjectOption;
import org.jkiss.dbeaver.model.cli.runtime.CLIMonitor;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectContainer;
import org.jkiss.utils.CommonUtils;
import picocli.CommandLine;

import java.util.ArrayList;
import java.util.List;

@CommandLine.Command(
    subcommands = {
        MetaListCommand.class,
        MetaDDLCommand.class
    }
)
public abstract class AbstractMetaObjectCommand extends CLIAbstractSubcommand {

    private static final Log log = Log.getLog(AbstractMetaObjectCommand.class);

    @CommandLine.Mixin
    private ProjectOption projectOption;

    @CommandLine.Mixin
    private DataSourceAuthOptions authOptions;

    @CommandLine.ArgGroup(exclusive = true, multiplicity = "0..1")
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

    /**
     * Gets the type of object this command manages (e.g. "table", "schema", "database").
     */
    @NotNull
    public abstract String getObjectTypeName();

    public abstract boolean isRelevantObject(@NotNull DBSObject object);

    @Nullable
    public abstract String getTargetObjectName();

    @NotNull
    public DBPDataSource connectDataSource(
        @NotNull DBRProgressMonitor monitor,
        @Nullable CreateOrFindDataSource options
    ) throws DBException {
        CLIConnectionUtils.connect(
            options.existDataSourceIdOrName,
            options.tempDataSourceOptions,
            options.connectionSpec,
            getDataSourceUpdaters(options),
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
    protected List<DataSourceUpdater> getDataSourceUpdaters(@NotNull CreateOrFindDataSource options) {
        List<DataSourceUpdater> updaters = new ArrayList<>();
        if (!CommonUtils.isEmpty(spec.mixins())) {
            for (CommandLine.Model.CommandSpec mixin : spec.mixins().values()) {
                if (mixin.userObject() instanceof DataSourceUpdater mixinUpdater) {
                    updaters.add(mixinUpdater);
                }
            }
        }
        if (options.tempDataSourceOptions != null) {
            updaters.add(options.tempDataSourceOptions);
        }
        return updaters;
    }

    @Nullable
    public DBSObject findObject(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBSObjectContainer container,
        @NotNull String objectName
    ) throws DBException {
        if (container.getName().equals(objectName) && isRelevantObject(container)) {
            return container;
        }
        return container.getChild(monitor, objectName);
    }

    public void executeWithMonitor(@NotNull String operationName, @NotNull MetaOperation operation) throws CLIException {
        CLIMonitor monitor = new CLIMonitor();
        monitor.beginTask(operationName, 1);
        try {
            operation.run(monitor);
        } catch (CLIException e) {
            throw e;
        } catch (DBException e) {
            throw new CLIException(operationName + " failed: " + e.getMessage(), e, CLIConstants.EXIT_CODE_ERROR);
        } finally {
            monitor.done();
        }
    }

    @Nullable
    public DBSObjectContainer getBaseContainer(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBPDataSource dataSource
    ) throws DBException {
        return resolveContainer(monitor, dataSource, null, null);
    }

    @Nullable
    protected DBSObjectContainer resolveContainer(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBPDataSource dataSource,
        @Nullable String databaseName,
        @Nullable String schemaName
    ) throws DBException {
        DBSObjectContainer container = null;
        if (dataSource instanceof DBSObjectContainer dbsObjectContainer) {
            container = dbsObjectContainer;
        }

        if (CommonUtils.isNotEmpty(databaseName)) {
            if (container != null) {
                DBSObject child = container.getChild(monitor, databaseName);
                if (child instanceof DBSObjectContainer dbsObjectContainer) {
                    container = dbsObjectContainer;
                } else {
                    throw new CLIException("Database '" + databaseName + "' not found", CLIConstants.EXIT_CODE_ERROR);
                }
            } else {
                throw new CLIException(
                    "Datasource '" + dataSource.getContainer().getName() + "' does not support databases",
                    CLIConstants.EXIT_CODE_ERROR
                );
            }
        }

        if (CommonUtils.isNotEmpty(schemaName)) {
            if (container != null) {
                DBSObject child = container.getChild(monitor, schemaName);
                if (child instanceof DBSObjectContainer dbsObjectContainer) {
                    container = dbsObjectContainer;
                } else {
                    throw new CLIException("Schema '" + schemaName + "' not found", CLIConstants.EXIT_CODE_ERROR);
                }
            } else {
                throw new CLIException("Container does not support schemas", CLIConstants.EXIT_CODE_ERROR);
            }
        }

        return container;
    }


    @Override
    public void run() throws CLIException {
        if (spec.commandLine().getParseResult().subcommand() == null) {
            String helpMessage = CLIUtils.getHelpFromCommand(spec);
            context().addResult(helpMessage);
            context().setPostAction(CLIProcessResult.PostAction.SHUTDOWN);
        }
    }
}
