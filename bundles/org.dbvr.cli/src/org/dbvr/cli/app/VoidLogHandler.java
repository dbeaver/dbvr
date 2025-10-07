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
package org.dbvr.cli.app;

import org.jkiss.dbeaver.LogHandler;

// log nothing
public class VoidLogHandler implements LogHandler {
    @Override
    public String getName(String name) {
        return "";
    }

    @Override
    public boolean isDebugEnabled(String name) {
        return false;
    }

    @Override
    public boolean isErrorEnabled(String name) {
        return false;
    }

    @Override
    public boolean isFatalEnabled(String name) {
        return false;
    }

    @Override
    public boolean isInfoEnabled(String name) {
        return false;
    }

    @Override
    public boolean isTraceEnabled(String name) {
        return false;
    }

    @Override
    public boolean isWarnEnabled(String name) {
        return false;
    }

    @Override
    public void trace(String name, Object message) {

    }

    @Override
    public void trace(String name, Object message, Throwable t) {

    }

    @Override
    public void debug(String name, Object message) {

    }

    @Override
    public void debug(String name, Object message, Throwable t) {

    }

    @Override
    public void info(String name, Object message) {

    }

    @Override
    public void info(String name, Object message, Throwable t) {

    }

    @Override
    public void warn(String name, Object message) {

    }

    @Override
    public void warn(String name, Object message, Throwable t) {

    }

    @Override
    public void error(String name, Object message) {

    }

    @Override
    public void error(String name, Object message, Throwable t) {

    }

    @Override
    public void fatal(String name, Object message) {

    }

    @Override
    public void fatal(String name, Object message, Throwable t) {

    }
}
