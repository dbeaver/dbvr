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
package org.dbvr.cli.sql;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.cli.model.DataSourceUpdater;
import picocli.CommandLine;

public class DefaultSchemaOption implements DataSourceUpdater {
    @Nullable
    @CommandLine.Option(names = "--default-catalog", arity = "1", description = "Default catalog name")
    private String defaultCatalog;

    @Nullable
    @CommandLine.Option(names = "--default-schema", arity = "1", description = "Default schema name")
    private String defaultSchema;

    @Override
    public void updateDataSource(@NotNull DBPDataSourceContainer dataSource) {
        if (defaultCatalog != null) {
            dataSource.getConnectionConfiguration().getBootstrap().setDefaultCatalogName(defaultCatalog);
        }
        if (defaultSchema != null) {
            dataSource.getConnectionConfiguration().getBootstrap().setDefaultSchemaName(defaultSchema);
        }
    }
}
