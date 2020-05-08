package org.tomvercaut.plugin;

import org.apache.commons.io.FileUtils;
import org.gradle.api.Project;
import org.gradle.internal.impldep.org.junit.Assert;
import org.gradle.testfixtures.ProjectBuilder;
import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.gradle.testkit.runner.TaskOutcome;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class Dcm4chePluginTest {

    @BeforeEach
    void setUp() throws IOException {
        testProjectDir = Files.createTempDirectory("dcm4cheplugin");
        // Prepare build.gradle
        build_gradle = Files.createFile(Paths.get(testProjectDir.toAbsolutePath().toString(), "build.gradle"));
        try(Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(build_gradle.toFile())))) {
            writer.write(String.format("plugins { id \"%s\" }\n", id));
        }
        System.out.println("Dcm4chePluginTest.setUp: " + build_gradle.toAbsolutePath().toString());
    }

    /**
     * Helper method that runs a Gradle task in the testProjectDir
     *
     * @param arguments         the task arguments to execute
     * @param isSuccessExpected boolean representing whether or not the build is supposed to fail
     * @return the task's BuildResult
     */
    private BuildResult gradle(boolean isSuccessExpected, List<String> arguments) {
        List<String> largs = new ArrayList<>();
        largs.add("--stacktrace");
        if (arguments != null && !arguments.isEmpty())
            largs.addAll(arguments);
        GradleRunner runner = GradleRunner.create()
                .withArguments(largs)
                .withProjectDir(testProjectDir.toFile())
                .withPluginClasspath()
                .withDebug(true);
        return isSuccessExpected ? runner.build() : runner.buildAndFail();
    }

    @AfterEach
    void tearDown() throws IOException {
        if (testProjectDir != null)
            FileUtils.deleteDirectory(testProjectDir.toFile());
    }

    @Test
    public void testDcm4che() throws IOException {
        Project project = ProjectBuilder.builder().build();
        project.getExtensions().add("version", "5.22.2");
        project.getPluginManager().apply(id);
        assertTrue(project.getPluginManager().hasPlugin(id));

        BufferedWriter writer = new BufferedWriter(new FileWriter(build_gradle.toFile(), true));
        writer.newLine();
        writer.write("dcm4che {");
        writer.newLine();
        writer.write("version = \"5.20.0\"");
        writer.newLine();
        writer.write("}");
        writer.newLine();
        writer.close();
        BuildResult result = gradle(true, Collections.singletonList("dcm4che"));
        var outcome = Objects.requireNonNull(result.task(":dcm4che")).getOutcome();
        Assert.assertNotNull(outcome);
        Assert.assertEquals(TaskOutcome.SUCCESS, outcome);
        System.out.println("Task outcome: ");
        System.out.println(result.getOutput());
    }

    private final String id = "org.tomvercaut.dcm4che-plugin";
    private Path testProjectDir;
    private Path build_gradle;
}