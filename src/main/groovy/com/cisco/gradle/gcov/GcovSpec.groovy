package com.cisco.gradle.gcov

import org.gradle.model.Managed
import org.gradle.model.Unmanaged

@Managed
interface GcovSpec {
    File getWorkingDir()
    void setWorkingDir(File dir)

    String getSourceDir()
    void setSourceDir(String dir)

    boolean getXmlEnabled()
    void setXmlEnabled(boolean enabled)

    boolean getHtmlEnabled()
    void setHtmlEnabled(boolean enabled)

    @Unmanaged
    Closure getBinaryFilter()
    void setBinaryFilter(Closure filter)
}
