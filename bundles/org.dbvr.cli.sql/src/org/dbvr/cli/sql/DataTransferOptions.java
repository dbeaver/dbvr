/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2025 DBeaver Corp and others
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
import picocli.CommandLine;

public class DataTransferOptions {

    @CommandLine.Option(names = {"-limit"},
        arity = "1",
        description = "Limits number of lines in an executed sql. Default: unlimited",
        defaultValue = "0")
    private int limit = 0;

    @NotNull
    @CommandLine.Option(names = {"-format", "-output-format"},
        arity = "1",
        description = "Write the execution result in a specific format (csv, xml, etc. csv by default)",
        defaultValue = "csv")
    private String outputFormat = "csv";

    @Nullable
    @CommandLine.Option(names = {"-op", "-output-format-parameters"},
        arity = "1",
        description = "Parameters (exporter options as list of props prop1=value1,prop2=value2 coma and space are delimiters).")
    private String outputFormatParameters;


    public int getLimit() {
        return limit;
    }

    @NotNull
    public String getOutputFormat() {
        return outputFormat;
    }

    @Nullable
    public String getOutputFormatParameters() {
        return outputFormatParameters;
    }
}
