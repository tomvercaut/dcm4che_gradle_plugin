package org.tomvercaut.plugin;

import org.gradle.api.Project;
import org.gradle.api.provider.Property;

public class Dcm4chePluginExtension {
    private String version;


    public Dcm4chePluginExtension() {
        version = "";
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }
}
