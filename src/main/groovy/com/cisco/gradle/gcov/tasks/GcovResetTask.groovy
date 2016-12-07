package com.cisco.gradle.gcov.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

class GcovResetTask extends DefaultTask {
    @OutputFile
    File timestampFile

    FileCollection getGcovFiles() {
        return project.fileTree(project.buildDir) {
            it.include '**/*.gcda'
        }
    }

    @TaskAction
    protected void run() {
        project.delete {
            it.delete gcovFiles
        }

        if (inputs.hasInputs) {
            timestampFile.text = new Date().toString()
        }
    }
}
