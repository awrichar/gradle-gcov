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
    private static final String COVERAGE_FOLDER = 'coverage'

    private static final String TASK_MAIN = 'gcov'
    private static final String TASK_CAPTURE = 'gcovCapture'
    private static final String TASK_HTML = 'gcovHtml'
    private static final String TASK_XML = 'gcovXml'

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
    void createGcovTasks(ModelMap<Task> tasks, GcovSpec gcov, @Path('buildDir') File buildDir) {
        tasks.create(TASK_MAIN, Task) { Task task ->
            task.dependsOn TASK_HTML, TASK_XML
            task.group 'Code coverage'
            task.description 'Runs gcov code coverage on available unit tests.'
        }

        tasks.create(TASK_HTML, GcovTask) { GcovTask task ->
            task.enabled = gcov.htmlEnabled
            if (gcov.workingDir) {
                task.workingDir = gcov.workingDir
            }
            task.sourceDir = gcov.sourceDir
            task.resultsDir = "${buildDir}/${COVERAGE_FOLDER}/html"
            task.format = GcovTask.OutputFormat.HTML
            task.dependsOn TASK_CAPTURE
        }

        tasks.create(TASK_XML, GcovTask) { GcovTask task ->
            task.enabled = gcov.xmlEnabled
            if (gcov.workingDir) {
                task.workingDir = gcov.workingDir
            }
            task.sourceDir = gcov.sourceDir
            task.resultsDir = "${buildDir}/${COVERAGE_FOLDER}/xml"
            task.format = GcovTask.OutputFormat.XML
            task.dependsOn TASK_CAPTURE
        }
    }

    @Mutate
    void addBinariesToGcov(ModelMap<Task> tasks, GcovSpec gcov, @Path('binaries') ModelMap<NativeBinarySpec> binaries) {
        tasks.create(TASK_CAPTURE, Task) { Task containerTask ->
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
