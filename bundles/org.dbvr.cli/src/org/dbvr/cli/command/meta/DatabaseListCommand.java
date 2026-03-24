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

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.cli.CLIProcessResult;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectContainer;
import org.jkiss.dbeaver.model.struct.rdb.DBSCatalog;
import picocli.CommandLine;

@CommandLine.Command(name = "database", description = "Database (catalog) operations")
public class DatabaseListCommand extends AbstractMetaObjectCommand {

    @Override
    public String getObjectTypeName() {
        return "database";
    }

    @Override
    public boolean isRelevantObject(@NotNull DBSObject object) {
        return object instanceof DBSCatalog;
    }

    @Override
    public DBSObjectContainer getBaseContainer(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBPDataSource dataSource
    ) throws DBException {
        // for databases which itself is the container
        if (dataSource instanceof DBSObjectContainer dbsObjectContainer) {
            var container = dbsObjectContainer.getDataSource();
            if (container != null) {
                boolean embedded = container.getContainer().getDriver().isEmbedded();
                if (embedded) {
                    context().addResult("Database doesn't support databases/catalogs");
                    context().setPostAction(CLIProcessResult.PostAction.SHUTDOWN);
                    return null;
                }
            }
        }

        return super.getBaseContainer(monitor, dataSource);
    }
}
