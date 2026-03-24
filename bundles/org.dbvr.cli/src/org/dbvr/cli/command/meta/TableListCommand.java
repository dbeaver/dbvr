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
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.model.struct.DBSEntityType;
import org.jkiss.dbeaver.model.struct.DBSObject;
import picocli.CommandLine;

@CommandLine.Command(name = "table", description = "Table meta operations")
public class TableListCommand extends AbstractMetaObjectCommand {

    @NotNull
    @Override
    public String getObjectTypeName() {
        return "table";
    }

    @Override
    public boolean isRelevantObject(@NotNull DBSObject object) {
        if (object instanceof DBSEntity dbsEntity) {
            DBSEntityType entityType = dbsEntity.getEntityType();
            return entityType == DBSEntityType.TABLE || entityType == DBSEntityType.VIEW;
        }
        return false;
    }
}
