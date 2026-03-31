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
import org.jkiss.dbeaver.model.cli.*;
import org.jkiss.dbeaver.model.cli.runtime.CLIMonitor;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectContainer;
import org.jkiss.utils.CommonUtils;
import picocli.CommandLine;

@CommandLine.Command
public abstract class AbstractMetaObjectCommand extends CLIAbstractSubcommand {


    public abstract boolean isRelevantObject(@NotNull DBSObject object);

    @Nullable
    public abstract String getTargetObjectName();


    @Nullable
    public DBSObject findObject(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBSObjectContainer container,
        @NotNull String objectName
    ) throws DBException {
        if (container.getName().equals(objectName) && isRelevantObject(container)) {
            return container;
        }
        return container.getChild(monitor, objectName);
    }

    public void executeWithMonitor(@NotNull String operationName, @NotNull MetaOperation operation) throws CLIException {
        CLIMonitor monitor = new CLIMonitor();
        monitor.beginTask(operationName, 1);
        try {
            operation.run(monitor);
        } catch (CLIException e) {
            throw e;
        } catch (DBException e) {
            throw new CLIException(operationName + " failed: " + e.getMessage(), e, CLIConstants.EXIT_CODE_ERROR);
        } finally {
            monitor.done();
        }
    }

    @Nullable
    public DBSObjectContainer getBaseContainer(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBPDataSource dataSource
    ) throws DBException {
        return resolveContainer(monitor, dataSource, null, null);
    }

    @Nullable
    protected DBSObjectContainer resolveContainer(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBPDataSource dataSource,
        @Nullable String databaseName,
        @Nullable String schemaName
    ) throws DBException {
        DBSObjectContainer container = null;
        if (dataSource instanceof DBSObjectContainer dbsObjectContainer) {
            container = dbsObjectContainer;
        }

        if (CommonUtils.isNotEmpty(databaseName)) {
            if (container == null) {
                throw new CLIException(
                    "Datasource '" + dataSource.getContainer().getName() + "' does not support databases",
                    CLIConstants.EXIT_CODE_ERROR
                );
            }
            container = getChildContainer(monitor, container, databaseName);
        }

        if (CommonUtils.isNotEmpty(schemaName)) {
            if (container == null) {
                throw new CLIException("Container does not support schemas", CLIConstants.EXIT_CODE_ERROR);
            }
            container = getChildContainer(monitor, container, schemaName);
        }

        return container;
    }

    @NotNull
    protected DBSObjectContainer getChildContainer(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBSObjectContainer parent,
        @NotNull String childName
    ) throws DBException {
        DBSObject child = parent.getChild(monitor, childName);
        if (child instanceof DBSObjectContainer dbsObjectContainer) {
            return dbsObjectContainer;
        } else {
            throw new CLIException(childName + "' not found", CLIConstants.EXIT_CODE_ERROR);
        }
    }
    
    @Override
    public void run() throws CLIException {
        if (spec.commandLine().getParseResult().subcommand() == null) {
            String helpMessage = CLIUtils.getHelpFromCommand(spec);
            context().addResult(helpMessage);
            context().setPostAction(CLIProcessResult.PostAction.SHUTDOWN);
        }
    }
}
