package com.cisco.gradle.gcov.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.SkipWhenEmpty
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecSpec

class GcovRunTask extends DefaultTask {
    enum OutputFormat {
        HTML, XML
    }

    Object gcovr = 'gcovr'
    Object workingDir
    String sourceDir
    Object resultsDir
    OutputFormat format

    @InputFiles
    @SkipWhenEmpty
    FileCollection getGcovFiles() {
        return project.fileTree(project.buildDir) {
            it.include '**/*.gcda'
        }
    }

    @OutputDirectory
    File getResultsDir() {
        return project.file(resultsDir)
    }

    @TaskAction
    protected void exec() {
        File resultsFile

        if (format == OutputFormat.XML) {
            resultsFile = new File(getResultsDir(), "${project.name}.xml")
        } else {
            resultsFile = new File(getResultsDir(), 'index.html')
        }

        resultsFile.parentFile.mkdirs()

        project.exec { ExecSpec spec ->
            spec.executable gcovr
            spec.args '-r', sourceDir, '-o', resultsFile.path

            if (workingDir) {
                spec.workingDir workingDir
            }

            if (format == OutputFormat.XML) {
                spec.args '--xml'
            } else {
                spec.args '--html', '--html-details'
            }
        }

        URI resultsUri = new URI("file", "", resultsFile.toURI().getPath(), null, null)
        logger.lifecycle("Coverage results saved to ${resultsUri}")
    }
}
