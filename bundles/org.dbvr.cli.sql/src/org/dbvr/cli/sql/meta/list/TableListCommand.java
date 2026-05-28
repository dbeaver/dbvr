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
package org.dbvr.cli.sql.meta.list;

import org.dbvr.cli.sql.meta.AbstractMetaObjectCommand;
import org.dbvr.cli.sql.meta.MetaFullNameOptions;
import org.dbvr.cli.sql.meta.MetaSchemaOptions;
import org.dbvr.cli.sql.meta.TableCommand;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObjectContainer;
import picocli.CommandLine;

@CommandLine.Command(name = "list", description = "List tables")
public class TableListCommand extends AbstractListCommand {

    @CommandLine.ParentCommand
    private TableCommand parent;

    @CommandLine.Mixin
    private MetaSchemaOptions containerOptions;

    @CommandLine.Mixin
    private MetaFullNameOptions fullNameOptions;

    @NotNull
    @Override
    protected AbstractMetaObjectCommand getParentCommand() {
        return parent;
    }

    @NotNull
    @Override
    protected String getOperationName() {
        return "List tables";
    }

    @Nullable
    @Override
    protected DBSObjectContainer getBaseContainer(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBPDataSource dataSource
    ) throws DBException {
        MetaFullNameOptions.Resolved r = fullNameOptions.resolve(
            dataSource,
            containerOptions.getDatabaseName(),
            containerOptions.getSchemaName(),
            null,
            2,
            false
        );
        if (r.fromFullName()) {
            return parent.resolveContainerByPath(monitor, dataSource, r.containerPath());
        }
        return parent.getBaseContainer(monitor, dataSource, r.databaseName(), r.schemaName());
    }
}
