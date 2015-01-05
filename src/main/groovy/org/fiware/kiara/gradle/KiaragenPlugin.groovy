/*
 * Copyright 2003-2012 the original author or authors.
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
package org.fiware.kiara.gradle

import java.io.File;
import java.util.List;

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputDirectory;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.OutputDirectory;

class KiaragenPlugin implements Plugin<Project> {

    public static final String KIARAGEN_TASK_NAME = 'kiaragen'
    static final String KIARAGEN_EXTENSION_NAME = 'kiaragen';
    static final String GROUP_SOURCE_GENERATION = "Source Generation";

    @Override
    void apply(Project project) {
        
        project.configurations {
            kiaragen
        }
        project.dependencies {
            kiaragen 'org.fiware.kiara:kiaragen:0.1.0'
        }
        
        project.extensions.create(KIARAGEN_EXTENSION_NAME, KiaragenExtension)
        project.afterEvaluate {
            project.task(KIARAGEN_TASK_NAME, type:KiaragenTask) { kiaragenTask ->
                description = "Generate Java source files from KIARA idl files"
                group = GROUP_SOURCE_GENERATION
                
                if (project.kiaragen.sourceDir) sourceDir = project.kiaragen.sourceDir
                if (project.kiaragen.outputDir) outputDir = project.kiaragen.outputDir
                if (project.kiaragen.platform) platform = project.kiaragen.platform
                if (project.kiaragen.javaPackage) javaPackage = project.kiaragen.javaPackage
                if (project.kiaragen.example) example = project.kiaragen.example
                if (project.kiaragen.ppDisable) ppDisable = project.kiaragen.ppDisable
                if (project.kiaragen.tempDir) tempDir = project.kiaragen.tempDir
                if (project.kiaragen.includePaths) includePaths = project.kiaragen.includePaths
                if (project.kiaragen.idlExtension) idlExtension = project.kiaragen.idlExtension
                
                // add kiaragen as a dependency to the java plugin
                if (project.plugins.hasPlugin(JavaPlugin.class))
                    this.configureJavaTaskDependency(project, kiaragenTask)
                else {
                    project.plugins.whenPluginAdded { plugin ->
                        if (plugin instanceof JavaPlugin)
                            this.configureJavaTaskDependency(project, kiaragenTask)
                    }
                }
            }
        }
    }

    private void configureJavaTaskDependency(Project project, KiaragenTask kiaragen) {
        Task compileJava = project.tasks.getByName(JavaPlugin.COMPILE_JAVA_TASK_NAME);
        if (compileJava == null)
            return;
        project.sourceSets.main.java.srcDirs += kiaragen.outputDir
        compileJava.dependsOn kiaragen
    }
}

class KiaragenExtension {
    def sourceDir
    def outputDir
    String platform 
    String srcPath
    String javaPackage
    boolean example
    boolean ppDisable
    String ppPath
    def tempDir
    List<String> includePaths = []
    String idlExtension
}