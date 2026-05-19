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

import org.dbvr.cli.sql.meta.ddl.DatabaseDDLCommand;
import org.dbvr.cli.sql.meta.list.DatabaseListCommand;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.cli.CLIProcessResult;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectContainer;
import org.jkiss.dbeaver.model.struct.rdb.DBSCatalog;
import picocli.CommandLine;


@CommandLine.Command(name = DatabaseCommand.COMMAND_NAME, description = "Database (catalog) operations",
    subcommands = {
        DatabaseListCommand.class,
        DatabaseDDLCommand.class
    })
public class DatabaseCommand extends AbstractMetaObjectCommand {
    public static final String COMMAND_NAME = "database";

    @Override
    public boolean isRelevantObject(@NotNull DBSObject object) {
        return object instanceof DBSCatalog;
    }

    @Nullable
    public DBSObjectContainer getBaseContainer(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBPDataSource dataSource,
        @Nullable String databaseName,
        @Nullable String schemaName
    ) throws DBException {
        // for databases which itself is the container
        if (dataSource instanceof DBSObjectContainer dbsObjectContainer) {
            DBPDataSource ds = dbsObjectContainer.getDataSource();
            if (ds != null) {
                boolean embedded = ds.getContainer().getDriver().isEmbedded();
                if (embedded) {
                    context().addResult("Database doesn't support databases/catalogs");
                    context().setPostAction(CLIProcessResult.PostAction.SHUTDOWN);
                    return null;
                }
            }
        }

        return resolveContainer(monitor, dataSource, databaseName, null);
    }
}
