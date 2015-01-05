package org.fiware.kiara.gradle

import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Test

class KiaragenPluginTest {
    @Test
    void pluginIsApplied() {
        Project project = ProjectBuilder.builder().build()
        project.apply plugin: 'org.fiware.kiara.kiaragen'

        project.afterEvaluate {
            def task = project.tasks.findByName(KiaragenPlugin.KIARAGEN_TASK_NAME)
            assert task instanceof KiaragenTask
            assert task.sourceDir == project.file("src/main/idl")
            assert task.outputDir == project.file("$project.buildDir/generated-src/kiara")
        }
    }
}
