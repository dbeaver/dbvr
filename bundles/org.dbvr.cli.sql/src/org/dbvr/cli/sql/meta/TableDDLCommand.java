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
import org.jkiss.dbeaver.model.DBPScriptObject;
import org.jkiss.dbeaver.model.cli.CLIConstants;
import org.jkiss.dbeaver.model.cli.CLIException;
import org.jkiss.dbeaver.model.cli.CLIProcessResult;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectContainer;
import org.jkiss.utils.CommonUtils;
import picocli.CommandLine;

import java.util.HashMap;
import java.util.Map;

@CommandLine.Command(name = "ddl", description = "Get table DDL")
public class TableDDLCommand extends AbstractMetaCommand {

    @NotNull
    @CommandLine.ParentCommand
    private TableCommand parent;

    @CommandLine.Option(names = {"--full"}, description = "Show full DDL")
    private boolean fullDDL;

    @Override
    public void run() throws CLIException {
        String objectName = parent.getTargetObjectName();
        if (CommonUtils.isEmpty(objectName)) {
            throw new CLIException(
                "Object name is not specified",
                CLIConstants.EXIT_CODE_ILLEGAL_ARGUMENTS
            );
        }
        parent.executeWithMonitor("Get table DDL", monitor -> execute(monitor, objectName));
    }

    private void execute(@NotNull DBRProgressMonitor monitor, @NotNull String objectName) throws DBException {
        DBPDataSource dataSource = connectDataSource(monitor);
        DBSObjectContainer container = parent.getBaseContainer(monitor, dataSource);
        if (container == null) {
            return;
        }
        DBSObject object = parent.findObject(monitor, container, objectName);
        if (object == null || !parent.isRelevantObject(object)) {
            throw new CLIException("Table '" + objectName + "' not found", CLIConstants.EXIT_CODE_ERROR);
        }
        if (object instanceof DBPScriptObject dbpScriptObject) {
            Map<String, Object> options = new HashMap<>();
            if (fullDDL) {
                options.put(DBPScriptObject.OPTION_INCLUDE_NESTED_OBJECTS, true);
                options.put(DBPScriptObject.OPTION_INCLUDE_COMMENTS, true);
                options.put(DBPScriptObject.OPTION_INCLUDE_PERMISSIONS, true);
            }
            String ddl = dbpScriptObject.getObjectDefinitionText(monitor, options);
            context().addResult(ddl.trim());
            context().setPostAction(CLIProcessResult.PostAction.SHUTDOWN);
        } else {
            throw new CLIException(
                "Table '" + objectName + "' does not support DDL",
                CLIConstants.EXIT_CODE_ERROR
            );
        }
    }
}
