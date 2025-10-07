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

import org.apache.commons.cli.CommandLine;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.cli.*;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.exec.DBCStatistics;
import org.jkiss.dbeaver.model.exec.output.DBCOutputSeverity;
import org.jkiss.dbeaver.model.exec.output.DBCOutputWriter;
import org.jkiss.dbeaver.model.impl.DataSourceContextProvider;
import org.jkiss.dbeaver.model.preferences.DBPPropertyDescriptor;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.LoggingProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLQuery;
import org.jkiss.dbeaver.model.sql.SQLScriptContext;
import org.jkiss.dbeaver.model.sql.SQLScriptElement;
import org.jkiss.dbeaver.model.sql.data.SQLQueryDataContainer;
import org.jkiss.dbeaver.model.sql.exec.SQLScriptProcessor;
import org.jkiss.dbeaver.model.sql.parser.SQLScriptParser;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.tools.transfer.IDataTransferConsumer;
import org.jkiss.dbeaver.tools.transfer.IDataTransferProcessor;
import org.jkiss.dbeaver.tools.transfer.registry.DataTransferProcessorDescriptor;
import org.jkiss.dbeaver.tools.transfer.registry.DataTransferRegistry;
import org.jkiss.dbeaver.tools.transfer.stream.IStreamDataExporter;
import org.jkiss.dbeaver.tools.transfer.stream.StreamConsumerSettings;
import org.jkiss.dbeaver.tools.transfer.stream.StreamTransferConsumer;
import org.jkiss.utils.CommonUtils;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.*;

public class SQLParameterHandler implements ICommandLineParameterHandler {
    private static final Log log = Log.getLog(SQLParameterHandler.class);
    private static final String DEFAULT_FORMAT = "csv";

    private static final String CONTEXT_PARAM_OUTPUT_FORMAT_PARAMETERS = "output-format-parameters";
    private static final String CONTEXT_PARAM_OUTPUT_FORMAT = "outputFormat";
    private static final String CONTEXT_PARAM_LIMIT = "limit";

    @Override
    public void handleParameter(
        @NotNull CommandLine commandLine,
        @NotNull String name,
        @Nullable String value,
        @NotNull CommandLineContext context
    ) throws DBException {
        String sqlQuery = value;
        if (CommonUtils.isEmpty(sqlQuery)) {
            sqlQuery = CLIUtils.readValueFromFileOrSystemIn(context);
        }
        if (CommonUtils.isEmpty(sqlQuery)) {
            throw new CLIException("SQL query is empty", CLIConstants.EXIT_CODE_ILLEGAL_ARGUMENTS);
        }

        DBPDataSourceContainer dataSourceContainer = context.getContextParameter(DBPDataSourceContainer.class.getName());
        if (dataSourceContainer == null) {
            throw new CLIException(
                "No connection specified",
                CLIConstants.EXIT_CODE_ILLEGAL_ARGUMENTS
            );
        }

        executeScript(commandLine, context, dataSourceContainer, sqlQuery);
    }

    private void executeScript(
        @NotNull CommandLine commandLine,
        @NotNull CommandLineContext context,
        @NotNull DBPDataSourceContainer dataSourceContainer,
        @NotNull String sqlQuery
    ) throws CLIException {
        DBPDataSource dataSource = dataSourceContainer.getDataSource();
        if (dataSource == null) {
            throw new CLIException("Can't obtain data source", CLIConstants.EXIT_CODE_ERROR);
        }
        DBRProgressMonitor monitor = new LoggingProgressMonitor(log);
        DBCExecutionContext executionContext = dataSource.getDefaultInstance().getDefaultContext(monitor, false);

        List<SQLScriptElement> scriptElements = SQLScriptParser.parseScript(executionContext.getDataSource(), sqlQuery);
        SQLScriptContext scriptContext = new SQLScriptContext(null, () -> executionContext, null, new LogOutputWriter(), null);

        String outputFormat =
            CommonUtils.isEmpty((String) context.getContextParameter(CONTEXT_PARAM_OUTPUT_FORMAT))
                ? DEFAULT_FORMAT
                : context.getContextParameter(CONTEXT_PARAM_OUTPUT_FORMAT);

        if (CommonUtils.isEmpty(outputFormat)) {
            throw new CLIException("Can't determine output format: '" + outputFormat + "'", CLIConstants.EXIT_CODE_ILLEGAL_ARGUMENTS);
        }
        DataTransferProcessorDescriptor processorDescriptor = DataTransferRegistry.getInstance()
            .getAvailableProcessors(StreamTransferConsumer.class, DBSEntity.class)
            .stream()
            .filter(p -> p.getProcessorFileExtension().equals(outputFormat))
            .findFirst()
            .orElse(null);
        if (processorDescriptor == null) {
            throw new CLIException(
                "Can't find data transfer processor for format '" + outputFormat + "'",
                CLIConstants.EXIT_CODE_ILLEGAL_ARGUMENTS
            );
        }

        IDataTransferProcessor processorInstance = processorDescriptor.getInstance();
        if (!(processorInstance instanceof IStreamDataExporter streamDataExporter)) {
            throw new CLIException(
                "Invalid processor. " + IStreamDataExporter.class.getSimpleName() + " expected",
                CLIConstants.EXIT_CODE_ERROR
            );
        }

        Map<String, Object> processorProperties = new HashMap<>();
        if (commandLine.getOptionValue(CONTEXT_PARAM_OUTPUT_FORMAT_PARAMETERS) != null) {
            String cutomPropsString = commandLine.getOptionValue(CONTEXT_PARAM_OUTPUT_FORMAT_PARAMETERS);
            if (CommonUtils.isNotEmpty(cutomPropsString)) {
                Arrays.stream(cutomPropsString.split(","))
                    .filter(CommonUtils::isNotEmpty)
                    .map(param -> {
                        String[] keyValue = param.split("=", 2);
                        if (keyValue.length == 2
                            && CommonUtils.isNotEmpty(keyValue[0])
                            && CommonUtils.isNotEmpty(keyValue[1])
                        ) {
                            return keyValue;
                        } else {
                            log.warn("Skip invalid output format parameter '" + param + "'");
                            return null;
                        }
                    })
                    .filter(Objects::nonNull)
                    .forEach(keyValue -> processorProperties.put(keyValue[0], keyValue[1]));
            }
        }
        for (DBPPropertyDescriptor prop : processorDescriptor.getProperties()) {
            if (!processorProperties.containsKey(prop.getId())) {
                processorProperties.put(prop.getId(), prop.getDefaultValue());
            }
        }

        Path outputFile = context.getContextParameter(OutputFileParameterHandler.OUTPUT_FILE);

        DataSourceContextProvider dataSourceContextProvider = new DataSourceContextProvider(dataSource);
        StreamConsumerSettings settings = prepareSettings(commandLine, context);

        boolean first = true;
        int limit = 0;
        if (context.getContextParameter(CONTEXT_PARAM_LIMIT) != null) {
            limit = CommonUtils.toInt(context.getContextParameter(CONTEXT_PARAM_LIMIT), 0);
        }
        for (var script : scriptElements) {
            if (!(script instanceof SQLQuery query)) {
                log.debug("Skip non-query script element: " + script.getText());
                continue;
            }

            try (
                var out = outputFile == null ? new ByteArrayOutputStream() : new BufferedOutputStream(
                    Files.newOutputStream(
                        outputFile,
                        first ? StandardOpenOption.CREATE : StandardOpenOption.APPEND
                    ))
            ) {
                first = false;
                StreamTransferConsumer consumer = new StreamTransferConsumer();
                SQLQueryDataContainer sqlQueryDataContainer = new SQLQueryDataContainer(
                    dataSourceContextProvider, query, scriptContext, log
                );
                consumer.initTransfer(
                    sqlQueryDataContainer,
                    settings,
                    new IDataTransferConsumer.TransferParameters(
                        processorDescriptor.isBinaryFormat(),
                        processorDescriptor.isHTMLFormat(),
                        out
                    ),
                    streamDataExporter,
                    processorProperties,
                    dataSourceContainer.getProject()
                );

                SQLScriptProcessor scriptProcessor = new SQLScriptProcessor(
                    executionContext,
                    List.of(query),
                    scriptContext,
                    consumer,
                    log
                );
                if (limit > 0) {
                    scriptProcessor.setMaxRows(limit);
                }
                scriptProcessor.runScript(monitor);

                consumer.finishTransfer(monitor, false);
                DBCStatistics statistics = scriptProcessor.getTotalStatistics();

                if (statistics.getRowsFetched() <= 0 && statistics.getRowsUpdated() > 0) {
                    out.write(("Rows updated: " + statistics.getRowsUpdated() + "\n").getBytes(settings.getOutputEncoding()));
                } else if (statistics.getRowsFetched() <= 0 && statistics.getRowsUpdated() <= 0) {
                    out.write("Success\n".getBytes(settings.getOutputEncoding()));
                }

                if (out instanceof ByteArrayOutputStream byteArrayOutputStream) {
                    String result = byteArrayOutputStream.toString(settings.getOutputEncoding());
                    context.addResult(result);
                }
            } catch (Exception e) {
                throw new CLIException("Failed to execute script", e, CLIConstants.EXIT_CODE_ERROR);
            }
        }

    }

    private StreamConsumerSettings prepareSettings(CommandLine commandLine, CommandLineContext context) {
        StreamConsumerSettings settings = new StreamConsumerSettings();
        settings.setOutputClipboard(false);
        settings.setOutputEncodingBOM(false);
        return settings;
    }

    private static class LogOutputWriter implements DBCOutputWriter {
        @Override
        public void println(@Nullable DBCOutputSeverity severity, @Nullable String message) {
            if (message != null) {
                log.debug("CLI sql execution log: " + message);
            }
        }

        @Override
        public void flush() {

        }
    }

}
