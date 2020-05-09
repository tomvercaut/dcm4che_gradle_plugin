package org.tomvercaut.plugin;

import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.SystemUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.maven.shared.invoker.*;
import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.TaskAction;
import org.gradle.internal.impldep.org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Log4j2
public class Dcm4cheProcessing extends DefaultTask {

    private static final String execGit = (SystemUtils.IS_OS_WINDOWS) ? "git.exe" : "git";
    private static final String execMvn = (SystemUtils.IS_OS_WINDOWS) ? "mvn" : "mvn";

    @TaskAction
    public void process() {
        System.out.println("Dcm4cheProcessing.process");
        System.out.println("dcm4che version: " + getProject().property("version"));
        System.out.println("build dir: " + this.getProject().getBuildDir().toString());

        try {
            int rv;
            var propertyVersion = getProject().property("version");
            if (propertyVersion == null) {
                log.error("Version of the required dcm4che project is not set.");
                System.exit(1);
            }
            var version = propertyVersion.toString();
            if (version == null) {
                log.error("Version of the required dcm4che project is not set.");
                System.exit(1);
            }
            boolean isInstalled = isDcm4cheInstalled(version);
            if (isInstalled) {
                log.info(String.format("dcm4che %s is already installed.", version));
                return;
            }
            validateMinimumRequirements();
            Pair<Integer, Path> resultClone = git_clone();
            if (resultClone.getKey()!=0) {
                log.error("Cloning the dcm4che git repository failed.");
                System.exit(1);
            }
            Path srcDcm4che = resultClone.getValue();
            rv = git_checkout(srcDcm4che, version);
            if (rv != 0) {
                log.error(String.format("Checking out version %s of the git repository failed.", version));
                System.exit(1);
            }
            rv =  mvn_install(srcDcm4che);
            if (rv != 0) {
                log.error("Installation of the dcm4che package into the local maven repository failed.");
                System.exit(1);
            }
            rv =  mvn_clean(srcDcm4che);
            if (rv != 0) {
                log.error("Cleaning up the build of the dcm4che package failed.");
                System.exit(1);
            }
        } catch (IOException | InterruptedException | MavenInvocationException | RuntimeException e) {
            e.printStackTrace();
        }
    }

    /**
     * Clone the dcm4che git repository to a local temporary directory.
     * @return Path where the project resides.
     * @throws IOException if an I/O error occurs
     * @throws InterruptedException if the current thread is
     *         {@linkplain Thread#interrupt() interrupted} by another
     *         thread while it is waiting, then the wait is ended and
     *         an {@link InterruptedException} is thrown.
     */
    private Pair<Integer,Path> git_clone() throws IOException, InterruptedException {
        System.out.println("git clone dcm4che");
        var buildDir = this.getProject().getBuildDir();
        if (!Files.exists(buildDir.toPath())) {
            Files.createDirectories(buildDir.toPath());
        }
        var dcmSrcDir = Files.createDirectories(Paths.get(String.valueOf(buildDir.toPath()), "dcm4che"));
        ProcessBuilder cloneBuilder = new ProcessBuilder(execGit, "clone", "https://github.com/dcm4che/dcm4che.git");
        cloneBuilder.directory(buildDir);
        var cloneProcess = cloneBuilder.start();
        int rv = cloneProcess.waitFor();
        return Pair.of(rv, dcmSrcDir);
    }

    /**
     * Checkout the requested version of the dcm4che git repository.
     * @param p path to the source directory
     * @param version version of the project that needs to checked out
     * @return Exit code of git checkout command
     * @throws IOException if an I/O error occurs
     * @throws InterruptedException if the current thread is
     *         {@linkplain Thread#interrupt() interrupted} by another
     *         thread while it is waiting, then the wait is ended and
     *         an {@link InterruptedException} is thrown.
     */
    private int git_checkout(Path p, String version) throws IOException, InterruptedException {
        System.out.println(String.format("git checkout dcm4che %s", version));
        ProcessBuilder checkoutBuilder = new ProcessBuilder(execGit, "checkout", version);
        checkoutBuilder.directory(p.toFile());
        var checkoutProcess = checkoutBuilder.start();
        return checkoutProcess.waitFor();
    }

    private File getMavenExecutable() {
        var items = Stream.of(System.getenv("PATH").split(Pattern.quote(File.pathSeparator))).map(Paths::get).collect(Collectors.toList());
        for (Path path : items) {
            if (SystemUtils.IS_OS_WINDOWS) {
                Path pcmd1 = path.resolve("mvn.cmd");
                if (isExecutable(pcmd1)) return pcmd1.toFile();
                Path pcmd2 = path.resolve("mvn.bat");
                if (isExecutable(pcmd2)) return pcmd2.toFile();
            } else {
                Path pcmd = path.resolve(execMvn);
                if (isExecutable(pcmd)) return pcmd.toFile();
            }
        }
        return null;
    }

    /**
     * Check if a path is an executable
     * @param p path
     * @return True if the path is an executable, false otherwise.
     */
    private static boolean isExecutable(Path p) {
        return Files.exists(p) && !Files.isDirectory(p) && Files.isExecutable(p);
    }

    private InvocationResult invokeMaven(InvocationRequest request) throws MavenInvocationException {
        Invoker invoker = new DefaultInvoker();
        invoker.setMavenExecutable(getMavenExecutable());
        invoker.setMavenHome(Paths.get(getUserHomeDir(), ".m2").toFile());
        return invoker.execute(request);
    }

    /**
     * Run the maven install command on a path.
     * @param p path to a maven project
     * @return Exit code of maven install command
     * @throws MavenInvocationException  Signals an error during the construction of the command line used to invoke Maven.
     */
    private int mvn_install(Path p) throws MavenInvocationException {
        InvocationRequest request = new DefaultInvocationRequest();
        request.setPomFile(Paths.get(p.toAbsolutePath().toString(), "pom.xml").toFile());
        request.setGoals(Collections.singletonList("install"));
        return invokeMaven(request).getExitCode();
    }

    /**
     * Run the maven clean command on a path.
     * @param p path to a maven project
     * @return Exit code of the maven clean command
     * @throws MavenInvocationException  Signals an error during the construction of the command line used to invoke Maven.
     */
    private int mvn_clean(Path p) throws MavenInvocationException {
        InvocationRequest request = new DefaultInvocationRequest();
        request.setPomFile(Paths.get(p.toAbsolutePath().toString(), "pom.xml").toFile());
        request.setGoals(Collections.singletonList("clean"));
        return invokeMaven(request).getExitCode();
    }

    /**
     * Checks if git is available on the PATH.
     *
     * @return True if git is present, false otherwise.
     */
    private boolean hasGit() {
        return hasExecutableInPath(execGit);
    }

    /**
     * Checks if maven is available on the PATH.
     *
     * @return True if maven is present, false otherwise.
     */
    private boolean hasMvn() {
        return hasExecutableInPath(execMvn);
    }

    /**
     * Check if all the necessary executables are present on the system to install dcm4che.
     */
    private void validateMinimumRequirements() {
        log.info("Validating minimum requirements to install dcm4che");
        boolean ok = true;
        boolean git = hasGit();
        boolean mvn = hasMvn();
        if (!git) {
            ok = false;
            log.warn("git: not found");
        } else {
            log.info("git: found");
        }
        if (!mvn) {
            ok = false;
            log.warn("mvn: not found");
        } else {
            log.info("mvn: found");
        }
        if (!ok) {
            String nl = System.lineSeparator();
            StringBuilder sb = new StringBuilder();
            sb.append("Please install the necessary requirements before trying to install dcm4che").append(nl);
            sb.append("Information on how to install the requirements:").append(nl);
            if (!git) {
                sb.append("  git: https://git-scm.com");
            }
            if (!mvn) {
                sb.append("  mvn: http://maven.apache.org");
            }
            log.warn(sb.toString());
            System.exit(1);
        }
    }

    /**
     * Check if an executable exists based on the system environment variable PATH.
     *
     * @param exec name of the executable
     * @return True if the executable exists and is executable.
     */
    private boolean hasExecutableInPath(String exec) {
        var items = Stream.of(System.getenv("PATH").split(Pattern.quote(File.pathSeparator))).map(Paths::get).collect(Collectors.toList());
        for (Path path : items) {
            var pcmd = path.resolve(exec);
            if (isExecutable(pcmd)) return true;
        }
        return false;
    }

    /**
     * Get a list of dcm4he modules.
     * @return list of dcm4he modules
     */
    private List<String> getDcm4cheModules() {
        return Arrays.asList(
                "assembly",
                "audit",
                "audit-keycloak",
                "camel",
                "conf",
                "conf-api",
                "conf-api-hl7",
                "conf-json",
                "conf-json-schema",
                "conf-ldap",
                "conf-ldap-audit",
                "conf-ldap-hl7",
                "conf-ldap-imageio",
                "conf-ldap-schema",
                "core",
                "dcmr",
                "deident",
                "dict",
//                "dict-arc",
                "emf",
                "hl7",
                "image",
                "imageio",
                "imageio-opencv",
                "imageio-rle",
                "jboss-modules",
                "js-dict",
                "json",
                "mime",
                "net",
                "net-audit",
                "net-hl7",
                "net-imageio",
                "parent",
                "soundex",
                "test-data",
                "ws-rs",
                "xdsi"
        );
    }

    private String getUserHomeDir() {
        return System.getProperty("user.home");
    }

    private Path getLocalMvnDcm4chePath() throws InvalidPathException {
        String homeDir = getUserHomeDir();
        return Paths.get(homeDir, ".m2", "repository", "org", "dcm4che");
    }

    private boolean isDcm4cheInstalled(String version) {
        try {
            Path basePath = getLocalMvnDcm4chePath();
            String sbase = basePath.toAbsolutePath().toString();
            if (!Files.isDirectory(basePath)) return false;
            final List<String> mods = getDcm4cheModules();
            for (String mod : mods) {
                String modDir = String.format("dcm4che-%s", mod);
                String nameJar = String.format("%s-%s.jar", modDir, version);
                String namePom = String.format("%s-%s.pom", modDir, version);
//                Path pathJar = Paths.get(sbase, modDir, version, nameJar);
                Path pathPom = Paths.get(sbase, modDir, version, namePom);
                if (!Files.isRegularFile(pathPom)) {
                    return false;
                }
            }
        } catch (InvalidPathException e) {
            log.error(e);
            return false;
        }
        return true;
    }
}
