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

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectContainer;
import org.jkiss.dbeaver.model.struct.rdb.DBSCatalog;
import org.jkiss.dbeaver.model.struct.rdb.DBSSchema;
import picocli.CommandLine;

@CommandLine.Command(name = "schema", description = "Schema operations",
    subcommands = {
        SchemaListCommand.class,
        SchemaDDLCommand.class
    })
public class SchemaCommand extends AbstractMetaObjectCommand {

    @Nullable
    @CommandLine.Option(names = {"--database-name", "-db"}, description = "Database (catalog) name", scope = CommandLine.ScopeType.INHERIT)
    protected String databaseName;

    @Nullable
    @CommandLine.Option(names = {"--schema-name", "-sn"}, description = "Schema name", scope = CommandLine.ScopeType.INHERIT)
    protected String schemaName;

    @NotNull
    @Override
    public String getObjectTypeName() {
        return "schema";
    }

    @Override
    public boolean isRelevantObject(@NotNull DBSObject object) {
        return object instanceof DBSSchema || object instanceof DBSCatalog;
    }

    @Nullable
    @Override
    public DBSObjectContainer getBaseContainer(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBPDataSource dataSource
    ) throws DBException {
        return resolveContainer(monitor, dataSource, this.databaseName, null);
    }

    @Nullable
    @Override
    public String getTargetObjectName() {
        return this.schemaName;
    }
}
