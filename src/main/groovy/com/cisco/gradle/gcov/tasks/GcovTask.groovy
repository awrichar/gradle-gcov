package com.cisco.gradle.gcov.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecSpec

class GcovTask extends DefaultTask {
    enum OutputFormat {
        HTML, XML
    }

    Object gcovr = 'gcovr'
    Object workingDir = project.projectDir
    String sourceDir
    Object resultsDir
    OutputFormat format

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
            spec.workingDir workingDir
            spec.args '-r', sourceDir, '-o', resultsFile.path

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
