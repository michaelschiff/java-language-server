package org.javacs;

import com.google.devtools.build.lib.analysis.AnalysisProtosV2;
import com.google.devtools.build.lib.analysis.AnalysisProtosV2.PathFragment;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

class JarLocator
{
    private static final Logger LOG = Logger.getLogger("main");

    /** Root of the workspace that is currently open in VSCode */
    private final Path workspaceRoot;
    /** External dependencies specified manually by the user */
    private final Collection<String> externalDependencies;

    public JarLocator(Path workspaceRoot, Collection<String> externalDependencies) {
        this.workspaceRoot = workspaceRoot;
        this.externalDependencies = externalDependencies;
    }

    public JarLocator(Path workspaceRoot) {
        this.workspaceRoot = workspaceRoot;
        this.externalDependencies = Set.of();
    }

    public Set<Path> classPath() {
        var bazelWorkspaceRoot = bazelWorkspaceRoot();
        if (Files.exists(bazelWorkspaceRoot.resolve("MODULE.bazel"))) {
            var absolute = new HashSet<Path>();
            for (var relative : bazelAQuery(bazelWorkspaceRoot, "Javac", "--classpath", "java_library", "java_test", "java_binary")) {
                absolute.add(bazelWorkspaceRoot.resolve(relative));
            }
            return absolute;
        }

        return Collections.emptySet();
    }

    public Set<Path> bazelSourcepath()
    {
        var bazelWorkspaceRoot = bazelWorkspaceRoot();
        if (Files.exists(bazelWorkspaceRoot.resolve("MODULE.bazel"))) {
            var absolute = new HashSet<Path>();
            var outputBase = bazelOutputBase(bazelWorkspaceRoot);
            for (var relative : bazelAQuery(
                bazelWorkspaceRoot,
                "Javac",
                "--sources",
                "java_library",
                "java_test",
                "java_binary"
            )) {
                absolute.add(outputBase.resolve(relative));
            }
        }
        return Collections.emptySet();
    }

    public Set<Path> bazelSourceJarPath() {
        var bazelWorkspaceRoot = bazelWorkspaceRoot();
        if (Files.exists(bazelWorkspaceRoot.resolve("MODULE.bazel"))) {
            Set<Path> res = new HashSet<>();
            for (String relative : bazelAQuery(
                bazelWorkspaceRoot,
                "Javac",
                "--classpath",
                "java_library",
                "java_test",
                "java_binary"
            )) {
                Path srcJar = bazelWorkspaceRoot.resolve(relative.strip().replace("header_", "").replace(".jar", "-sources.jar"));
                if (Files.exists(srcJar)) {
                    res.add(srcJar);
                }
            }
            return res;
        } else {
            return Collections.emptySet();
        }
    }

    private Path bazelWorkspaceRoot() {
        for (var current = workspaceRoot; current != null; current = current.getParent()) {
            if (Files.exists(current.resolve("MODULE.bazel"))) {
                return current;
            }
        }
        return workspaceRoot;
    }
    private Path bazelOutputBase(Path bazelWorkspaceRoot) {
        // Run bazel as a subprocess
        String[] command = {
            "bazel", "info", "output_base",
        };
        var output = fork(bazelWorkspaceRoot, command);
        if (output == NOT_FOUND) {
            return NOT_FOUND;
        }
        // Read output
        try {
            var out = Files.readString(output).trim();
            return Paths.get(out);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private Set<String> bazelAQuery(
            Path bazelWorkspaceRoot, String filterMnemonic, String filterArgument, String... kinds) {
        String kindUnion = "";
        for (var kind : kinds) {
            if (!kindUnion.isEmpty()) {
                kindUnion += " union ";
            }
            kindUnion += "kind(" + kind + ", //...)";
        }
        String[] command = {
            "bazel",
            "aquery",
            "--output=proto",
            "--include_aspects", // required for java_proto_library, see
            // https://stackoverflow.com/questions/63430530/bazel-aquery-returns-no-action-information-for-java-proto-library
            "--allow_analysis_failures",
            "mnemonic(" + filterMnemonic + ", " + kindUnion + ")"
        };
        var output = fork(bazelWorkspaceRoot, command);
        if (output == NOT_FOUND) {
            return Set.of();
        }
        return readActionGraph(output, filterArgument);
    }

    private Set<String> readActionGraph(Path output, String filterArgument) {
        try {
            var containerV2 = AnalysisProtosV2.ActionGraphContainer.parseFrom(Files.newInputStream(output));
            return readActionGraphFromV2(containerV2, filterArgument);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private Set<String> readActionGraphFromV2(AnalysisProtosV2.ActionGraphContainer container, String filterArgument) {
        var argumentPaths = new HashSet<String>();
        var outputIds = new HashSet<Integer>();
        for (var action : container.getActionsList()) {
            var isFilterArgument = false;
            for (var argument : action.getArgumentsList()) {
                if (isFilterArgument && argument.startsWith("-")) {
                    isFilterArgument = false;
                    continue;
                }
                if (!isFilterArgument) {
                    isFilterArgument = argument.equals(filterArgument);
                    continue;
                }
                argumentPaths.add(argument.strip());
            }
            outputIds.addAll(action.getOutputIdsList());
        }
        var artifactPaths = new HashSet<String>();
        for (var artifact : container.getArtifactsList()) {
	          if (outputIds.contains(artifact.getId()) && !filterArgument.equals("--output")) {
                // artifact is the output of another java action
                continue;
            }
            var relative = buildPath(container.getPathFragmentsList(), artifact.getPathFragmentId());
            if (!argumentPaths.contains(relative)) {
                // artifact was not specified by --filterArgument
                continue;
            }
            LOG.fine("...found bazel dependency " + relative);
            artifactPaths.add(relative);
        }
        return artifactPaths;
    }

    //TODO: make this not recursive (if its even actually necessary)
    private static String buildPath(List<PathFragment> fragments, int id) {
        for (PathFragment fragment : fragments) {
            if (fragment.getId() == id) {
                if (fragment.getParentId() != 0) {
                    return buildPath(fragments, fragment.getParentId()) + "/" + fragment.getLabel();
                }
                return fragment.getLabel();
            }
        }
        throw new RuntimeException();
    }

    private static Path fork(Path workspaceRoot, String[] command) {
        try {
            LOG.fine("Running " + String.join(" ", command) + " ...");
            var output = Files.createTempFile("java-language-server-bazel-output", ".proto");
            var process =
                    new ProcessBuilder()
                            .command(command)
                            .directory(workspaceRoot.toFile())
                            .redirectError(ProcessBuilder.Redirect.INHERIT)
                            .redirectOutput(output.toFile())
                            .start();
            // Wait for process to exit
            var result = process.waitFor();
            if (result != 0) {
                LOG.severe("`" + String.join(" ", command) + "` returned " + result);
                return NOT_FOUND;
            }
            return output;
        } catch (InterruptedException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static final Path NOT_FOUND = Paths.get("");
}
