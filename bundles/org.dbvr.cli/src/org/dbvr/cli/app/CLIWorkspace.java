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
package org.dbvr.cli.app;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.app.DBPPlatform;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.impl.app.BaseProjectImpl;
import org.jkiss.dbeaver.model.impl.app.BaseWorkspaceImpl;
import org.jkiss.dbeaver.registry.project.LocalProjectImpl;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.utils.ContentUtils;
import org.jkiss.dbeaver.utils.GeneralUtils;
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
        return projects;
    }

    @NotNull
    @Override
    public DBPProject createProject(@NotNull String name, @Nullable String description) throws DBException {
        GeneralUtils.validateResourceNameUnconditionally(name);
        Path projectPath = getAbsolutePath().resolve(name);
        if (Files.exists(projectPath)) {
            throw new DBException("Project '" + name + "' already exists");
        }
        try {
            Files.createDirectories(projectPath);
            LocalProjectImpl project = createProject(projectPath);
            project.getMetadataFolder(true);
            project.updateProject(null, description);

            // Create .project file for desktop compatibility
            BaseProjectImpl.updateProjectFile(projectPath, name);

            projects.add(project);
            return project;
        } catch (IOException e) {
            throw new DBException("Error creating project directory", e);
        }
    }

    @Override
    public void deleteProject(@NotNull DBPProject project) throws DBException {
        if (!projects.contains(project)) {
            throw new DBException("Project '" + project.getName() + "' not found in workspace");
        }
        try {
            projects.remove(project);
            Path projectPath = project.getAbsolutePath();
            if (Files.exists(projectPath)) {
                if (!ContentUtils.deleteFileRecursive(projectPath)) {
                    throw new IOException("Can't delete directory " + projectPath);
                }
            }
        } catch (IOException e) {
            throw new DBException("Error deleting project directory", e);
        }
    }

    @Override
    public void renameProject(@NotNull DBPProject project, @NotNull String newName) throws DBException {
        if (!projects.contains(project)) {
            throw new DBException("Project '" + project.getName() + "' not found in workspace");
        }
        GeneralUtils.validateResourceNameUnconditionally(newName);
        Path oldPath = project.getAbsolutePath();
        Path newPath = oldPath.getParent().resolve(newName);
        if (Files.exists(newPath)) {
            throw new DBException("Project '" + newName + "' already exists");
        }
        try {
            Files.move(oldPath, newPath);
            ((LocalProjectImpl) project).setAbsolutePath(newPath);
            project.getMetadataFolder(true);

            // Update .project file content
            BaseProjectImpl.updateProjectFile(newPath, newName);
        } catch (IOException e) {
            throw new DBException("Error renaming project directory", e);
        }
    }

    @Nullable
    @Override
    public DBPProject getActiveProject() {
        return activeProject;
    }

    public void setActiveProject(@NotNull DBPProject project) {
        this.activeProject = project;
        setActiveProjectName(project.getName());
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
    public void initializeProjects() throws DBException {
        List<Path> projectPaths = new ArrayList<>();
        try {
            Files.walkFileTree(getAbsolutePath(), new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                    if (attrs.isSymbolicLink()) {
                        return FileVisitResult.SKIP_SUBTREE;
                    }

                    Path fileName = dir.getFileName();
                    if (fileName != null && BaseProjectImpl.isHiddenProjectName(fileName.toString())) {
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

        String activeProjectName = getActiveProjectName();
        var defaultProject = CommonUtils.isEmpty(activeProjectName) ? null : getProject(activeProjectName);
        if (defaultProject == null) {
            activeProjectName = platform.getApplication().getDefaultProjectName();
            defaultProject = getProject(activeProjectName);
        }
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
