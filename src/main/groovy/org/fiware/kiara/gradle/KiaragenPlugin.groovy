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

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.plugins.JavaPlugin

class KiaragenPlugin implements Plugin<Project> {

    public static final String KIARAGEN_TASK = 'kiaragen'

    @Override
    void apply(Project project) {
        
        project.configurations {
            kiaragen
        }
        project.dependencies {
            kiaragen 'org.fiware.kiara:kiaragen:0.1.0'
        }
        
        project.task(KIARAGEN_TASK, type:KiaragenTask) {
             // add kiaragen as a dependency to the java plugin
            if (project.plugins.hasPlugin('java'))
                makeAsDependency(project, this)
            else {
                project.plugins.whenPluginAdded { plugin ->
                    if (plugin instanceof JavaPlugin)
                        makeAsDependency(project, this)
                }
            }
        }
    }

    private void makeAsDependency(Project project, KiaragenTask kiaragen) {
        Task compileJava = project.tasks.getByName(JavaPlugin.COMPILE_JAVA_TASK_NAME);
        if (compileJava == null)
            return;
        project.sourceSets.main.java.srcDirs += kiaragen.outputDir
        compileJava.dependsOn kiaragen
    }
}