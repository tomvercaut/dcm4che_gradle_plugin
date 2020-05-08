package org.tomvercaut.plugin;

import org.gradle.api.Action;
import org.gradle.api.Plugin;
import org.gradle.api.Project;

public class Dcm4chePlugin implements Plugin<Project> {
    @Override
    public void apply(Project project) {
        Dcm4chePluginExtension extension = project.getExtensions().create("dcm4che_options", Dcm4chePluginExtension.class);

//        project.getTasks().withType(Dcm4cheProcessing.class).configureEach(new Action<Dcm4cheProcessing>() {
//            @Override
//            public void execute(Dcm4cheProcessing dcm4cheProcessing) {
//
//            }
//        });
//        project.defaultTasks("Dcm4cheProcessing");
        project.getTasks().create("dcm4che", Dcm4cheProcessing.class);
    }
}
