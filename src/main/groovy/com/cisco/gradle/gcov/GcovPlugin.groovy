package com.cisco.gradle.gcov

import com.cisco.gradle.gcov.tasks.GcovTask
import org.gradle.api.Task
import org.gradle.api.file.FileTree
import org.gradle.language.nativeplatform.tasks.AbstractNativeCompileTask
import org.gradle.model.Defaults
import org.gradle.model.Model
import org.gradle.model.ModelMap
import org.gradle.model.Mutate
import org.gradle.model.Path
import org.gradle.model.RuleSource
import org.gradle.nativeplatform.NativeBinarySpec
import org.gradle.nativeplatform.test.tasks.RunTestExecutable

class GcovPlugin extends RuleSource {
    @Model
    void gcov(GcovSpec spec) {}

    @Defaults
    void setGcovDefaults(GcovSpec gcov) {
        gcov.sourceDir = '.'
        gcov.htmlEnabled = true
        gcov.xmlEnabled = false
        gcov.binaryFilter = { true }
    }

    @Defaults
    void createGcovTasks(ModelMap<Task> tasks, GcovSpec gcov) {
        tasks.create('gcov', Task) { Task task ->
            task.dependsOn 'gcovHtml', 'gcovXml'
            task.group 'Code coverage'
            task.description 'Runs gcov code coverage on available unit tests.'
        }

        tasks.create('gcovHtml', GcovTask) { GcovTask task ->
            task.enabled = gcov.htmlEnabled
            if (gcov.workingDir) {
                task.workingDir = gcov.workingDir
            }
            task.sourceDir = gcov.sourceDir
            task.setFormat GcovTask.OutputFormat.HTML
            task.dependsOn 'gcovCapture'
        }

        tasks.create('gcovXml', GcovTask) { GcovTask task ->
            task.enabled = gcov.xmlEnabled
            if (gcov.workingDir) {
                task.workingDir = gcov.workingDir
            }
            task.sourceDir = gcov.sourceDir
            task.setFormat GcovTask.OutputFormat.XML
            task.dependsOn 'gcovCapture'
        }
    }

    @Mutate
    void addBinariesToGcov(ModelMap<Task> tasks, GcovSpec gcov, @Path('binaries') ModelMap<NativeBinarySpec> binaries) {
        tasks.create('gcovCapture', Task) { Task containerTask ->
            binaries.each { NativeBinarySpec binary ->
                gcov.binaryFilter.delegate = binary
                gcov.binaryFilter.resolveStrategy = Closure.DELEGATE_FIRST
                if (!gcov.binaryFilter(binary)) {
                    return
                }

                binary.cCompiler.args '-fprofile-arcs', '-ftest-coverage'
                binary.cppCompiler.args '-fprofile-arcs', '-ftest-coverage'
                binary.linker.args '-lgcov'

                binary.tasks.withType(RunTestExecutable) { RunTestExecutable runTask ->
                    // ensure all unit tests run before attempting to analyze coverage
                    containerTask.dependsOn runTask

                    // purge all *.gcda artifacts before each test run
                    binary.tasks.withType(AbstractNativeCompileTask) { AbstractNativeCompileTask compileTask ->
                        FileTree runData = project.fileTree(compileTask.objectFileDir) {
                            include '**/*.gcda'
                        }

                        runTask.doFirst {
                            project.delete(runData.files)
                        }
                    }
                }
            }
        }
    }
}
