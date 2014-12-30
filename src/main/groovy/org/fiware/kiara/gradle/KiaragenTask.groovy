package org.fiware.kiara.gradle


import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.tasks.*;
import org.gradle.api.tasks.incremental.IncrementalTaskInputs;

import org.fiware.kiara.generator.Kiaragen;

class KiaragenTask extends DefaultTask {
    @InputDirectory
    @Optional
    File sourceDir = project.file('src/main/idl')
    
    @OutputDirectory
    @Optional
    File outputDir = project.file("${project.buildDir}/generated-src/kiara")
    
    @Input
    @Optional
    String platform = 'Java'
    
    @Input
    @Optional
    String srcPath = ''
    
    @Input
    @Optional
    String javaPackage = ''
    
    @Input
    @Optional
    boolean example = false
    
    @Input
    @Optional
    boolean ppDisable = true
    
    @Input
    @Optional
    String ppPath = null
    
    @Input
    @Optional
    File  tempDir = super.getTemporaryDir();
    
    @Input
    Set<File> includeDirs = []
    
    def sourceDir(String sourceDirPath) {
        this.sourceDir = project.file(sourceDirPath).canonicalFile
    }

    def sourceDir(File sourceDir) {
        this.sourceDir = sourceDir
    }

    def outputDir(String outputDirPath) {
        this.outputDir = project.file(outputDirPath).canonicalFile
    }

    def outputDir(File outputDir) {
        this.outputDir = outputDir
    }
    def tempDir(String tempDirPath) {
        this.tempDir = project.file(tempDirPath).canonicalFile
    }

    def tempDir(File tempDir) {
        this.tempDir = tempDir
    }
    def includeDir(File includeDir) {
        includeDirs << includeDir
    }

    def includeDir(String includeDir) {
        includeDirs << project.file(includeDir).canonicalFile
    }
    
    @TaskAction
    def process(IncrementalTaskInputs inputs) {
        // if no incremental inputs, all files are out of date
        if (!inputs.incremental) {
            processAll()
            return
        }
        // if any input file is removed, we have to regenerate all, 
        // because at the moment, we have no possibility, which generated
        // classes are affected. 
        boolean anySourceRemoved = false;
        inputs.removed {
            project.logger.debug("Removed: ${change.file.name}")
            anySourceRemoved = true
        }
        if (anySourceRemoved) {
            processAll()
            return
        }

        List<File> changedFiles = []
        inputs.outOfDate { change ->
            changedFiles.add(change.file)
        }

        if (!outputDir.exists() && !outputDir.mkdirs())
            throw new GradleException("Could not create kiaragen output directory: ${outputDir.absolutePath}")

        changedFiles.each { changedFile ->
            compile(changedFile.absolutePath)
        }
    }

    def processAll() {
        if (!outputDir.deleteDir())
            throw new GradleException("Could not delete kiaragen output directory: ${outputDir.absolutePath}")

        if (!outputDir.mkdirs())
            throw new GradleException("Could not create kiaragen output directory: ${outputDir.absolutePath}")

        project.fileTree(this.sourceDir.canonicalFile) {
            include '**/*.idl'
        }.each { idlFile ->
            process(idlFile)
        }
    }

    def processIDLFile(File idlFile) {
        def kiaragen = new Kiaragen()
        kiaragen.addIdlFile(idlFile)
        kiaragen.outputDir this.outputDir
        kiaragen.srcPath this.srcPath;
        // if no package is set, use relative path of idl file to source root
        // as package path
        if (javaPackage.isEmpty()) {
            String packagePath = idlFile.canonicalPath - sourceDir.canonicalPath;
            String jPackage = packagePath.replace(File.separator,".")
            kiaragen.javaPackage jPackage;
        }
        kiaragen.platform this.platform
        kiaragen.example this.example
        kiaragen.replace true   // always replace files
        kiaragen.ppDisable this.ppDisable;
        kiaragen.ppPath this.ppPath;
        kiaragen.tempDir this.tempDir;
        kiaragen.includePaths this.includeDirs
        
        boolean success;
        try {
            if (!kiaragen.execute()) {
                throw new GradleException();
            }
        } catch (all) {
            throw new GradleException("Failed to process IDL file ${idlFile}", all);
        }
    }
}
