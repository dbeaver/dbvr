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
package org.dbvr.cli.sql.meta.ddl;

import org.dbvr.cli.sql.meta.*;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.cli.help.CLIExample;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObjectContainer;
import picocli.CommandLine;

@CommandLine.Command(name = AbstractDDLCommand.COMMAND_NAME, description = "Get table DDL")
@CLIExample(examples = {
    TableDDLCommand.EXAMPLE_EXPLICIT,
    TableDDLCommand.EXAMPLE_FULL_NAME
})
public class TableDDLCommand extends AbstractDDLCommand {

    static final String EXAMPLE_EXPLICIT = MetaCommand.COMMAND_NAME + " " + TableCommand.COMMAND_NAME + " "
        + AbstractDDLCommand.COMMAND_NAME + " -ds my-datasource-id --database-name my_database --schema-name my_schema --table-name my_table";
    static final String EXAMPLE_FULL_NAME = MetaCommand.COMMAND_NAME + " " + TableCommand.COMMAND_NAME + " "
        + AbstractDDLCommand.COMMAND_NAME + " -ds my-datasource-id --full-name my_database.my_schema.my_table";

    @CommandLine.ParentCommand
    private TableCommand parent;

    @CommandLine.Mixin
    private MetaSchemaOptions containerOptions;

    @CommandLine.Mixin
    private MetaFullNameOptions fullNameOptions;

    @Nullable
    @CommandLine.Option(names = {"--table-name", "-tn"}, description = "Table name")
    protected String tableName;

    private MetaFullNameOptions.Resolved resolved;

    @NotNull
    private MetaFullNameOptions.Resolved resolved(@NotNull DBPDataSource dataSource) throws DBException {
        if (resolved == null) {
            resolved = fullNameOptions.resolve(
                dataSource,
                containerOptions.getDatabaseName(),
                containerOptions.getSchemaName(),
                tableName,
                2,
                true
            );
        }
        return resolved;
    }

    @NotNull
    @Override
    protected AbstractMetaObjectCommand getParentCommand() {
        return parent;
    }

    @NotNull
    @Override
    protected String getObjectTypeName() {
        return "Table";
    }

    @Nullable
    @Override
    protected String getTargetObjectName(@NotNull DBPDataSource dataSource) throws DBException {
        return resolved(dataSource).objectName();
    }

    @Nullable
    @Override
    protected DBSObjectContainer getBaseContainer(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBPDataSource dataSource
    ) throws DBException {
        MetaFullNameOptions.Resolved r = resolved(dataSource);
        if (r.fromFullName()) {
            return parent.resolveContainerByPath(monitor, dataSource, r.containerPath());
        }
        return parent.getBaseContainer(monitor, dataSource, r.databaseName(), r.schemaName());
    }
}
