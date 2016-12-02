package com.cisco.gradle.gcov.tasks

import org.gradle.api.tasks.Exec

class GcovTask extends Exec {
    enum OutputFormat {
        HTML, XML
    }

    File resultsDir = project.file("${project.buildDir}/coverage")
    String sourceDir

    private File resultsFile

    GcovTask() {
        executable = 'gcovr'
    }

    @Override
    protected void exec() {
        if (resultsFile) {
            resultsFile.parentFile.mkdirs()
        }

        args '-r', sourceDir

        super.exec()

        if (resultsFile) {
            URI resultsUri = new URI("file", "", resultsFile.toURI().getPath(), null, null)
            logger.lifecycle("Coverage results saved to ${resultsUri}")
        }
    }

    void setFormat(OutputFormat format) {
        switch (format) {
            case OutputFormat.HTML:
                resultsFile = project.file("${resultsDir}/index.html")
                args '--html', '--html-details', '-o', resultsFile.path
                break

            case OutputFormat.XML:
                resultsFile = project.file("${resultsDir}/${project.name}.xml")
                args '--xml', '-o', resultsFile.path
                break
        }
    }
}
