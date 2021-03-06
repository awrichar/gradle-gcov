package com.cisco.gradle.gcov

import com.cisco.gradle.gcov.tasks.GcovResetTask
import com.cisco.gradle.gcov.tasks.GcovReportTask
import org.gradle.api.Task
import org.gradle.api.tasks.TaskContainer
import org.gradle.model.Defaults
import org.gradle.model.Each
import org.gradle.model.Model
import org.gradle.model.ModelMap
import org.gradle.model.Mutate
import org.gradle.model.Path
import org.gradle.model.RuleSource
import org.gradle.nativeplatform.NativeBinarySpec
import org.gradle.nativeplatform.tasks.InstallExecutable
import org.gradle.nativeplatform.test.tasks.RunTestExecutable

class GcovPlugin extends RuleSource {
    private static final String COVERAGE_FOLDER = 'gcov'

    private static final String TASK_MAIN = 'gcov'
    private static final String TASK_RESET = 'gcovReset'
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
    void setGcovTaskDefaults(@Each GcovReportTask task, GcovSpec gcov) {
        task.workingDir = gcov.workingDir
        task.sourceDir = gcov.sourceDir
        task.dependsOn TASK_RESET
    }

    @Mutate
    void createGcovTasks(ModelMap<Task> tasks,
                         GcovSpec gcov,
                         @Path('buildDir') File buildDir,
                         @Path('binaries') ModelMap<NativeBinarySpec> binaries) {

        Collection<NativeBinarySpec> filteredBinaries = filterBinaries(binaries, gcov.binaryFilter)
        Collection<TaskContainer> filteredTasks = filteredBinaries*.tasks
        File coverageFolder = new File(buildDir, COVERAGE_FOLDER)

        filteredBinaries.each { NativeBinarySpec binary ->
            binary.cCompiler.args '-fprofile-arcs', '-ftest-coverage'
            binary.cppCompiler.args '-fprofile-arcs', '-ftest-coverage'
            binary.linker.args '-lgcov'
        }

        tasks.create(TASK_MAIN, Task) { Task task ->
            task.dependsOn TASK_HTML, TASK_XML
            task.group 'Code coverage'
            task.description 'Runs gcov code coverage on available unit tests.'
            task.outputs.dir coverageFolder
        }

        tasks.create(TASK_RESET, GcovResetTask) { GcovResetTask task ->
            task.timestampFile = new File(coverageFolder, 'timestamp')

            filteredTasks*.withType(InstallExecutable) { InstallExecutable installTask ->
                task.dependsOn installTask
                task.inputs.files installTask
            }

            filteredTasks*.withType(RunTestExecutable) { RunTestExecutable runTask ->
                runTask.mustRunAfter task
                runTask.inputs.file task.timestampFile
            }
        }

        tasks.create(TASK_HTML, GcovReportTask) { GcovReportTask task ->
            task.enabled = gcov.htmlEnabled
            task.resultsDir = new File(coverageFolder, 'html')
            task.format = GcovReportTask.OutputFormat.HTML

            filteredTasks*.withType(RunTestExecutable) { RunTestExecutable runTask ->
                task.dependsOn runTask
            }
        }

        tasks.create(TASK_XML, GcovReportTask) { GcovReportTask task ->
            task.enabled = gcov.xmlEnabled
            task.resultsDir = new File(coverageFolder, 'xml')
            task.format = GcovReportTask.OutputFormat.XML

            filteredTasks*.withType(RunTestExecutable) { RunTestExecutable runTask ->
                task.dependsOn runTask
            }
        }
    }

    private static Collection<NativeBinarySpec> filterBinaries(Iterable<NativeBinarySpec> binaries, Closure filter) {
        return binaries.findAll { NativeBinarySpec binary ->
            filter.delegate = binary
            filter.resolveStrategy = Closure.DELEGATE_FIRST
            filter(binary)
        }
    }
}
