package org.tomvercaut.plugin;

import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.TaskAction;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class Dcm4cheProcessing extends DefaultTask {

    @TaskAction
    public void process() {
        System.out.println("Dcm4cheProcessing.process");
        System.out.println("dcm4che version: " + getProject().property("version"));
        System.out.println("build dir: " + this.getProject().getBuildDir().toString() );

        try {
            git_clone();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void git_clone() throws IOException, InterruptedException {
        System.out.println("Cloning dcm4che project");
        var version = getProject().property("version");
        var buildDir = this.getProject().getBuildDir();
        if (!Files.exists(buildDir.toPath())) {
            Files.createDirectories(buildDir.toPath());
        }
        var dcmSrcDir = Files.createDirectories(Paths.get(String.valueOf(buildDir.toPath()), "dcm4che"));
        ProcessBuilder cloneBuilder = new ProcessBuilder("git", "clone", "https://github.com/dcm4che/dcm4che.git");
        cloneBuilder.directory(buildDir);
        var cloneProcess = cloneBuilder.start();
        int rv = cloneProcess.waitFor();
        if (rv != 0) {
            throw new RuntimeException("git clone exit code: " +rv);
        }
        assert version != null;
        ProcessBuilder checkoutBuilder = new ProcessBuilder("git", "checkout", version.toString());
        checkoutBuilder.directory(dcmSrcDir.toFile());
        var checkoutProcess = checkoutBuilder.start();
        rv = checkoutProcess.waitFor();
        if (rv != 0) {
            throw new RuntimeException("git checkout exit code: " +rv);
        }
    }
}
