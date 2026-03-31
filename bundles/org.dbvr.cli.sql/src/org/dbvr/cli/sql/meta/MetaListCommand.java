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
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPNamedObject;
import org.jkiss.dbeaver.model.cli.CLIAbstractSubcommand;
import org.jkiss.dbeaver.model.cli.CLIException;
import org.jkiss.dbeaver.model.cli.CLIProcessResult;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectContainer;
import picocli.CommandLine;

import java.util.Collection;
import java.util.stream.Collectors;

@CommandLine.Command(name = "list", description = "List metadata objects")
public class MetaListCommand extends CLIAbstractSubcommand {

    @CommandLine.Mixin
    private AbstractMetaObjectCommand.CreateOrFindDataSource dataSourceOptions;

    @NotNull
    @CommandLine.ParentCommand
    private AbstractMetaObjectCommand parent;

    @Override
    public void run() throws CLIException {
        parent.executeWithMonitor("List " + parent.getObjectTypeName() + "s", this::execute);
    }

    private void execute(@NotNull DBRProgressMonitor monitor) throws DBException {
        DBPDataSource dataSource = parent.connectDataSource(monitor, dataSourceOptions);
        DBSObjectContainer container = parent.getBaseContainer(monitor, dataSource);
        if (container == null) {
            return;
        }
        Collection<? extends DBSObject> children = container.getChildren(monitor);
        if (children != null) {
            String result = children.stream()
                .filter(parent::isRelevantObject)
                .map(DBPNamedObject::getName)
                .collect(Collectors.joining("\n"));
            context().addResult(result);
            context().setPostAction(CLIProcessResult.PostAction.SHUTDOWN);
        }
    }
}
