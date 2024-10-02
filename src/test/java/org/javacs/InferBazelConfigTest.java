package org.javacs;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import java.nio.file.Paths;
import org.junit.Test;

public class InferBazelConfigTest {
    @Test
    public void bazelClassPath() {
        var bazel = new JarLocator(Paths.get("src/test/examples/bazel-project"));
        assertThat(bazel.classPath(), contains(hasToString(endsWith("guava-18.0.jar"))));
    }

    @Test
    public void bazelClassPathInSubdir() {
        var bazel = new JarLocator(Paths.get("src/test/examples/bazel-project/hello"));
        assertThat(bazel.classPath(), contains(hasToString(endsWith("guava-18.0.jar"))));
    }

    @Test
    public void bazelClassPathWithProtos() {
        var bazel = new JarLocator(Paths.get("src/test/examples/bazel-protos-project"));
        assertThat(bazel.classPath(), hasItem(hasToString(endsWith("libperson_proto-speed.jar"))));
    }

    @Test
    public void bazelDocPath() {
        var bazel = new JarLocator(Paths.get("src/test/examples/bazel-project"));
        var docPath = bazel.buildSourcePath();
        assertThat(docPath, contains(hasToString(endsWith("guava-18.0-sources.jar"))));
    }

    @Test
    public void bazelDocPathInSubdir() {
        var bazel = new JarLocator(Paths.get("src/test/examples/bazel-project/hello"));
        assertThat(bazel.buildSourcePath(), contains(hasToString(endsWith("guava-18.0-sources.jar"))));
    }

    @Test
    public void bazelDocPathWithProtos() {
        var bazel = new JarLocator(Paths.get("src/test/examples/bazel-protos-project"));
        assertThat(bazel.buildSourcePath(), hasItem(hasToString(endsWith("person_proto-speed-src.jar"))));
    }
}
