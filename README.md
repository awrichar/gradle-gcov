# gradle-gcov

A Gradle plugin for analyzing code coverage using
[gcov](https://gcc.gnu.org/onlinedocs/gcc/Gcov.html) and [gcovr](http://gcovr.com).
Run with `gradle gcov`.

## Example Usage

    apply plugin: 'com.cisco.gcov'

    model {
        gcov {
            xmlEnabled = true
            htmlEnabled = true
            binaryFilter = { buildType.name == 'dev' }
        }
    }

## Configuration

All configuration is done via the `gcov` block in the model space. It has the following properties:

* **workingDir** - the directory in which to invoke `gcovr` (default: `projectDir`).
* **sourceDir** - the relative path to the folder that `gcovr` should search for source files (default: `'.'`).
* **htmlEnabled** - whether to generate a coverage report in HTML format (default: `true`).
* **xmlEnabled** - whether to generate a coverage report in [Cobertura's](http://cobertura.github.io/cobertura)
XML format (default: `false`).
* **binaryFilter** - a closure invoked for each `NativeBinarySpec` in the project, which should return true to indicate
if the binary should be configured for coverage.
