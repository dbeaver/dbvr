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
package org.dbvr.cli.command.meta;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DBPNamedObject;
import org.jkiss.dbeaver.model.DBPScriptObject;
import org.jkiss.dbeaver.model.cli.*;
import org.jkiss.dbeaver.model.cli.model.option.DataSourceAuthOptions;
import org.jkiss.dbeaver.model.cli.model.option.DataSourceOptions;
import org.jkiss.dbeaver.model.cli.model.option.ProjectOption;
import org.jkiss.dbeaver.model.runtime.AbstractJob;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectContainer;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.utils.CommonUtils;
import picocli.CommandLine;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

@CommandLine.Command(
    subcommands = {
        MetaListCommand.class,
        MetaDdlCommand.class
    }
)
public abstract class AbstractMetaObjectCommand extends CLIAbstractSubcommand {

    @CommandLine.Mixin
    private ProjectOption projectOption;

    @CommandLine.Option(names = {"--datasource"}, description = "Datasource ID or name", required = true,
        scope = CommandLine.ScopeType.INHERIT)
    private String datasourceId;

    @CommandLine.Option(names = {"--catalog"}, description = "Database (catalog) name", scope = CommandLine.ScopeType.INHERIT)
    private String databaseName;

    @CommandLine.Option(names = {"--schema"}, description = "Schema name", scope = CommandLine.ScopeType.INHERIT)
    private String schemaName;

    @CommandLine.Mixin
    private DataSourceOptions dataSourceOptions;

    @CommandLine.Mixin
    private DataSourceAuthOptions authOptions;

    @CommandLine.Option(names = {"--table"}, description = "Table name", scope = CommandLine.ScopeType.INHERIT)
    private String tableName;

    /**
     * @return type of object this command manages (e.g "table", "schhema", "database")
     */
    public abstract String getObjectTypeName();

    public abstract boolean isRelevantObject(@NotNull DBSObject object);

    public DBPDataSourceContainer getDataSourceContainer() throws DBException {
        DBPDataSourceContainer container = CLIUtils.findDataSource(
            CLIUtils.findProject(projectOption.getProjectIdOrName(), context()),
            datasourceId
        );

        dataSourceOptions.updateDataSource(container);
        authOptions.updateDataSource(container);

        return container;
    }

    public DBSObjectContainer getBaseContainer(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBPDataSource dataSource
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
                throw new CLIException("Datasource '" + datasourceId + "' does not support databases", CLIConstants.EXIT_CODE_ERROR);
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

        if (container == null) {
            throw new CLIException("Datasource '" + datasourceId + "' does not support metadata", CLIConstants.EXIT_CODE_ERROR);
        }

        return container;
    }

    public void list() throws Exception {
        executeOperation("List " + getObjectTypeName() + "s", this::internalList);
    }

    public void internalList(@NotNull DBRProgressMonitor monitor) throws DBException {
        DBPDataSourceContainer dataSourceContainer = getDataSourceContainer();
        dataSourceContainer.connect(monitor, true, false);
        DBPDataSource dataSource = dataSourceContainer.getDataSource();
        if (dataSource == null) {
            throw new CLIException("Can't connect to datasource '" + datasourceId + "'", CLIConstants.EXIT_CODE_ERROR);
        }

        DBSObjectContainer container = getBaseContainer(monitor, dataSource);
        Collection<? extends DBSObject> children = container.getChildren(monitor);

        if (children != null) {
            String result = children.stream()
                .filter(this::isRelevantObject)
                .map(DBPNamedObject::getName)
                .collect(Collectors.joining("\n"));
            if (CommonUtils.isEmpty(result)) {
                throw new CLIException("No " + getObjectTypeName() + "s found", CLIConstants.EXIT_CODE_ERROR);
            }
            context().addResult(result);
            context().setPostAction(CLIProcessResult.PostAction.SHUTDOWN);
        } else {
            throw new CLIException("No " + getObjectTypeName() + "s found", CLIConstants.EXIT_CODE_ERROR);
        }
    }

    public void ddl(String objectName, boolean fullDDL) throws Exception {
        if (CommonUtils.isEmpty(objectName)) {
            objectName = tableName;
        }
        if (CommonUtils.isEmpty(objectName)) {
            throw new CLIException("Object name is not specified", CLIConstants.EXIT_CODE_ILLEGAL_ARGUMENTS);
        }
        String finalObjectName = objectName;
        String operationName = "Get " + getObjectTypeName() + " DDL";
        executeOperation(operationName, monitor -> internalDdl(monitor, finalObjectName, fullDDL));
    }

    public void internalDdl(@NotNull DBRProgressMonitor monitor, String objectName, boolean fullDDL) throws DBException {
        DBPDataSourceContainer dataSourceContainer = getDataSourceContainer();
        dataSourceContainer.connect(monitor, true, false);
        DBPDataSource dataSource = dataSourceContainer.getDataSource();
        if (dataSource == null) {
            throw new CLIException("Can't connect to datasource '" + datasourceId + "'", CLIConstants.EXIT_CODE_ERROR);
        }

        DBSObjectContainer container = getBaseContainer(monitor, dataSource);
        DBSObject object = container.getChild(monitor, objectName);

        if (object == null || !isRelevantObject(object)) {
            throw new CLIException(getObjectTypeName() + " '" + objectName + "' not found", CLIConstants.EXIT_CODE_ERROR);
        }

        if (!(object instanceof DBPScriptObject)) {
            throw new CLIException(
                getObjectTypeName() + " '" + objectName + "' does not support DDL",
                CLIConstants.EXIT_CODE_ERROR
            );
        }

        Map<String, Object> options = new HashMap<>();
        if (fullDDL) {
            options.put(DBPScriptObject.OPTION_INCLUDE_NESTED_OBJECTS, true);
            options.put(DBPScriptObject.OPTION_INCLUDE_COMMENTS, true);
            options.put(DBPScriptObject.OPTION_INCLUDE_PERMISSIONS, true);
        }

        String ddl = ((DBPScriptObject) object).getObjectDefinitionText(monitor, options);
        context().addResult(ddl.trim());
        context().setPostAction(CLIProcessResult.PostAction.SHUTDOWN);
    }

    protected void executeOperation(@NotNull String name, @NotNull MetaOperation operation) throws Exception {
        AtomicReference<Exception> error = new AtomicReference<>();
        AbstractJob job = new AbstractJob(name) {
            @NotNull
            @Override
            protected IStatus run(@NotNull DBRProgressMonitor monitor) {
                try {
                    operation.run(monitor);
                } catch (Exception e) {
                    error.set(e);
                    return GeneralUtils.makeExceptionStatus(e);
                }
                return Status.OK_STATUS;
            }
        };
        job.schedule();
        job.join();
        if (error.get() != null) {
            throw error.get();
        }
    }

    @CommandLine.Spec
    CommandLine.Model.CommandSpec commandSpec;

    @Override
    public void run() throws CLIException {
        if (commandSpec.commandLine().getParseResult().subcommand() == null) {
            String helpMessage = CLIUtils.getHelpFromCommand(commandSpec);
            context().addResult(helpMessage);
            context().setPostAction(CLIProcessResult.PostAction.SHUTDOWN);
        }
    }

    public String getDatasourceId() {
        return datasourceId;
    }

    public String getDatabaseName() {
        return databaseName;
    }

    public String getSchemaName() {
        return schemaName;
    }

    public String getTableName() {
        return tableName;
    }

    public ProjectOption getProjectOption() {
        return projectOption;
    }

    public DataSourceOptions getDataSourceOptions() {
        return dataSourceOptions;
    }

    public DataSourceAuthOptions getAuthOptions() {
        return authOptions;
    }
}
