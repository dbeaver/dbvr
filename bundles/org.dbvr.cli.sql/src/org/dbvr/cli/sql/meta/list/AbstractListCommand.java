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

import org.dbvr.cli.sql.meta.AbstractMetaCommand;
import org.dbvr.cli.sql.meta.AbstractMetaObjectCommand;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPNamedObject;
import org.jkiss.dbeaver.model.cli.CLIException;
import org.jkiss.dbeaver.model.cli.CLIProcessResult;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectContainer;

import java.util.Collection;
import java.util.stream.Collectors;

public abstract class AbstractListCommand extends AbstractMetaCommand {

    @NotNull
    protected abstract AbstractMetaObjectCommand getParentCommand();

    @NotNull
    protected abstract String getOperationName();

    @Nullable
    protected abstract DBSObjectContainer getBaseContainer(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBPDataSource dataSource
    ) throws DBException;

    @Override
    public void run() throws CLIException {
        getParentCommand().executeWithMonitor(getOperationName(), this::execute);
    }

    private void execute(@NotNull DBRProgressMonitor monitor) throws DBException {
        DBPDataSource dataSource = connectDataSource();
        DBSObjectContainer container = getBaseContainer(monitor, dataSource);
        if (container == null) {
            return;
        }
        Collection<? extends DBSObject> children = container.getChildren(monitor);
        if (children != null) {
            String result = children.stream()
                .filter(getParentCommand()::isRelevantObject)
                .map(DBPNamedObject::getName)
                .collect(Collectors.joining("\n"));
            context().addResult(result);
            context().setPostAction(CLIProcessResult.PostAction.SHUTDOWN);
        }
    }
}
