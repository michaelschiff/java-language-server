package org.javacs;

import org.junit.Test;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

public class DocsTest
{
  @Test
  public void testJarSrcs() {
    Set<Path> docsPath = new HashSet<>();
    docsPath.add(Path.of("/Users/michaelschiff/go/src/github.com/Arize-ai/arize/bazel-out/darwin_arm64-fastbuild/bin/external/rules_jvm_external~~maven~maven/com/fasterxml/jackson/core/jackson-annotations/2.12.7/jackson-annotations-2.12.7-sources.jar"));
//    Docs docs = new Docs(docsPath);
  }
}
