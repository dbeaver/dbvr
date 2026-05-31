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
import org.jkiss.dbeaver.model.cli.CLIConstants;
import org.jkiss.dbeaver.model.cli.CLIException;
import org.jkiss.dbeaver.model.cli.help.CLIExample;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectContainer;
import org.jkiss.dbeaver.model.struct.rdb.DBSCatalog;
import picocli.CommandLine;

import java.util.Collection;

@CommandLine.Command(name = AbstractDDLCommand.COMMAND_NAME, description = "Get database DDL")
@CLIExample(examples = {
    DatabaseDDLCommand.EXAMPLE_EXPLICIT,
    DatabaseDDLCommand.EXAMPLE_FULL_NAME
})
public class DatabaseDDLCommand extends AbstractDDLCommand {

    static final String EXAMPLE_EXPLICIT = MetaCommand.COMMAND_NAME + " " + DatabaseCommand.COMMAND_NAME + " "
        + AbstractDDLCommand.COMMAND_NAME + " -ds my-datasource-id --database-name my_database";
    static final String EXAMPLE_FULL_NAME = MetaCommand.COMMAND_NAME + " " + DatabaseCommand.COMMAND_NAME + " "
        + AbstractDDLCommand.COMMAND_NAME + " -ds my-datasource-id --full-name my_database";

    private static final String UNSUPPORTED_MESSAGE = "Database DDL is not supported for this database type. " +
        "Try a lower-level DDL command, such as '"
        + MetaCommand.COMMAND_NAME + " " + SchemaCommand.COMMAND_NAME + " " + AbstractDDLCommand.COMMAND_NAME
        + "' or '"
        + MetaCommand.COMMAND_NAME + " " + TableCommand.COMMAND_NAME + " " + AbstractDDLCommand.COMMAND_NAME
        + "'.";

    @CommandLine.ParentCommand
    private DatabaseCommand parent;

    @CommandLine.Mixin
    private MetaDatabaseOptions containerOptions;

    @CommandLine.Mixin
    private MetaFullNameOptions fullNameOptions;

    @NotNull
    @Override
    protected AbstractMetaObjectCommand getParentCommand() {
        return parent;
    }

    @NotNull
    @Override
    protected String getObjectTypeName() {
        return "Database";
    }

    @Nullable
    @Override
    protected String getTargetObjectName(@NotNull DBPDataSource dataSource) throws DBException {
        return fullNameOptions.resolve(
            dataSource,
            null,
            null,
            containerOptions.getDatabaseName(),
            0,
            true
        ).objectName();
    }

    @Nullable
    @Override
    protected DBSObjectContainer getBaseContainer(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBPDataSource dataSource
    ) throws DBException {
        return parent.getBaseContainer(monitor, dataSource, null, null);
    }

    @Override
    protected void checkDDLSupported(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBPDataSource dataSource
    ) throws DBException {
        if (!hasDatabases(monitor, dataSource)) {
            throw new CLIException(UNSUPPORTED_MESSAGE, CLIConstants.EXIT_CODE_ERROR);
        }
    }

    @NotNull
    @Override
    protected String getUnsupportedDDLMessage(@NotNull String objectName) {
        return UNSUPPORTED_MESSAGE;
    }

    private boolean hasDatabases(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBPDataSource dataSource
    ) throws DBException {
        if (!(dataSource instanceof DBSObjectContainer container)) {
            return false;
        }
        Collection<? extends DBSObject> children = container.getChildren(monitor);
        if (children == null) {
            return false;
        }
        for (DBSObject child : children) {
            if (child instanceof DBSCatalog) {
                return true;
            }
        }
        return false;
    }
}
