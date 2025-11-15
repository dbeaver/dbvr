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

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.app.DBPPlatform;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.impl.app.BaseProjectImpl;
import org.jkiss.dbeaver.model.impl.app.BaseWorkspaceImpl;
import org.jkiss.dbeaver.registry.project.LocalProjectImpl;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.utils.CommonUtils;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

/**
 * CLI workspace
 */
public class CLIWorkspace extends BaseWorkspaceImpl {
    private static final Log log = Log.getLog(CLIWorkspace.class);

    private final List<DBPProject> projects = new ArrayList<>();

    public CLIWorkspace(@NotNull DBPPlatform platform, @NotNull Path workspacePath) {
        super(platform, workspacePath);
        this.activeProject = null;
    }

    @NotNull
    @Override
    public DBPPlatform getPlatform() {
        return DBWorkbench.getPlatform();
    }

    @NotNull
    @Override
    public String getWorkspaceId() {
        return "CLI";
    }

    @NotNull
    @Override
    public List<? extends DBPProject> getProjects() {
        return List.of();
    }

    @Nullable
    @Override
    public DBPProject getActiveProject() {
        return activeProject;
    }

    @Nullable
    @Override
    public DBPProject getProject(@NotNull String projectName) {
        return projects.stream()
            .filter(
                project -> project.getName().equals(projectName)
            )
            .findFirst()
            .orElse(null);
    }

    @Nullable
    @Override
    public DBPProject getProjectById(@NotNull String projectId) {
        return projects.stream()
            .filter(
                project -> project.getId().equals(projectId)
            )
            .findFirst()
            .orElse(null);
    }

    @Override
    public void initializeProjects() {
        List<Path> projectPaths = new ArrayList<>();
        try {
            Files.walkFileTree(getAbsolutePath(), new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                    if (attrs.isSymbolicLink()) {
                        return FileVisitResult.SKIP_SUBTREE;
                    }

                    boolean hasDbeaver = Files.exists(dir.resolve(DBPProject.METADATA_FOLDER));
                    boolean hasProjectFile = Files.exists(dir.resolve(BaseProjectImpl.PROJECT_FILE));

                    if (hasDbeaver || hasProjectFile) {
                        projectPaths.add(dir.toAbsolutePath().normalize());
                        // folder already marked as project - skip subfolders
                        return FileVisitResult.SKIP_SUBTREE;
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (Exception e) {
            log.error("Can't load cli workspace projects", e);
        }
        for (Path projectPath : projectPaths) {
            projects.add(createProject(projectPath));
        }
        var defaultProject = getProject(platform.getApplication().getDefaultProjectName());
        if (defaultProject == null) {
            defaultProject = createDefaultProject();
        }

        activeProject = defaultProject;

        initializeWorkspaceSession();
    }

    @Nullable
    private DBPProject createDefaultProject() {
        try {
            if (CommonUtils.isEmpty(platform.getApplication().getDefaultProjectName())) {
                return null;
            }
            Path defaultProjectPath = getAbsolutePath().resolve(
                platform.getApplication().getDefaultProjectName()
            );
            if (!Files.exists(defaultProjectPath)) {
                Files.createDirectories(defaultProjectPath);
            }
            var defaultProject = createProject(defaultProjectPath);
            projects.add(defaultProject);
            return defaultProject;
        } catch (IOException e) {
            log.error("Error creating default project", e);
            return null;
        }
    }

    @NotNull
    protected LocalProjectImpl createProject(@NotNull Path projectPath) {
        return new LocalProjectImpl(this, getAuthContext(), projectPath);
    }

    @Override
    public void dispose() {
    }

}
