package org.fiware.kiara.gradle


import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.tasks.*;
import org.gradle.api.tasks.incremental.IncrementalTaskInputs;
import org.fiware.kiara.generator.Kiaragen;

class KiaragenTask extends DefaultTask {

    public static final String KAIRA_IDL_EXTENSION = "idl";
    
    @InputDirectory
    @Optional
    File sourceDir = project.file('src/main/idl')
    
    @OutputDirectory
    @Optional
    File outputDir = project.file("${project.buildDir}/generated-src/kiara")
    
    @Input
    @Optional
    String platform = 'java'
    
    @Input
    @Optional
    String srcPath = ''
    
    @Input
    @Optional
    String javaPackage = null
    
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
    @Optional
    List<String> includePaths = []
    
    @Input
    @Optional
    String idlExtension = KAIRA_IDL_EXTENSION
    
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
        includePaths << includeDir.canonicalPath
    }

    def includeDir(String includePath) {
        includePaths << project.file(includePath).canonicalPath
    }
    
    @TaskAction
    def process(IncrementalTaskInputs inputs) {
        // if no incremental inputs, all files are out of date
        if (!inputs.incremental) {
            logger.info("Recreate all generated files")
            processAll()
            return
        }

        // process all changed files (gradle expects to handle this before the removed)
        List<File> changedFiles = []
        inputs.outOfDate { change ->
            changedFiles.add(change.file)
        }

        // if any input file is removed, we have to regenerate all output files, 
        // because at the moment we have no possibility to detect which generated
        // classes are affected. 
        boolean anySourceRemoved = false;
        inputs.removed { change ->
            logger.debug("Removed: ${change.file.name}")
            anySourceRemoved = true
        }
        if (anySourceRemoved) {
            logger.info("Any source removed. Recreate all generated files")
            processAll()
            return
        }

        if (!outputDir.exists() && !outputDir.mkdirs())
            throw new GradleException("Could not create kiaragen output directory: ${outputDir.absolutePath}")

        changedFiles.each { changedFile ->
            logger.info("File changed: ${changedFile.canonicalPath}")
            processIDLFile(changedFile)
        }
    }

    def processAll() {
        if (!outputDir.deleteDir())
            throw new GradleException("Could not delete kiaragen output directory: ${outputDir.absolutePath}")
        if (!outputDir.mkdirs())
            throw new GradleException("Could not create kiaragen output directory: ${outputDir.absolutePath}")

        project.fileTree(this.sourceDir.canonicalFile) {
            include "**/*.${idlExtension}"
        }.each { idlFile ->
            logger.info("Process file: ${idlFile}")
            processIDLFile(idlFile)
        }
    }

    def processIDLFile(File idlFile) {
        def kiaragen = new Kiaragen()
        kiaragen.addIdlFile(idlFile)
        logger.info("idlFile = ${idlFile}")
        kiaragen.outputDir = this.outputDir
        logger.info("outputDir = ${this.outputDir}")
        kiaragen.srcPath = this.srcPath
        logger.info("srcPath = ${this.srcPath}")
        // if no package is set, use relative path of idl file to source root
        // as package path
        String jPackage = javaPackage
        if (jPackage == null) {
            String pckPath = (idlFile.parentFile.canonicalPath - sourceDir.canonicalPath) - File.separator
            if (!pckPath.empty) {
                jPackage = pckPath.trim().replace(File.separator,".")
            }
        }
        kiaragen.javaPackage = jPackage
        logger.info("javaPackage = ${jPackage}")
        kiaragen.platform = this.platform
        logger.info("platform = ${this.platform}")
        kiaragen.example = this.example
        logger.info("example = ${this.example}")
        kiaragen.replace = true   // always replace files
        kiaragen.ppDisable = this.ppDisable
        logger.info("ppDisable = ${this.ppDisable}")
        kiaragen.ppPath = this.ppPath
        logger.info("ppPath = ${this.ppPath}")
        kiaragen.tempDir = this.tempDir
        logger.info("tempDir = ${this.tempDir}")
        kiaragen.includePaths = this.includePaths
        logger.info("includePaths = ${this.includePaths}")
        
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
