package org.javacs;

import java.io.IOException;
import java.net.URI;
import java.nio.file.CopyOption;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.FileAttribute;
import java.util.*;
import javax.tools.*;

public class SourceLocator
{

    private final SourceFileManager fileManager = new SourceFileManager();

    public SourceLocator(Set<Path> sourcePaths, Set<Path> sourceJarPaths) throws IOException
    {
            // Any source .java we found
        fileManager.setLocationFromPaths(StandardLocation.SOURCE_PATH, sourcePaths);

        // Any source .jar files we found
        Set<Path> unzippedSrcJarPaths = new HashSet<>();
        for (Path sourceJarPath : sourceJarPaths) {
            FileSystem sourceJarFS = FileSystems.newFileSystem(URI.create("jar:file:" + sourceJarPath.toString()), Map.of());
            unzippedSrcJarPaths.add(sourceJarFS.getPath("/"));
        }
        fileManager.setLocationFromPaths(StandardLocation.CLASS_PATH, unzippedSrcJarPaths);

        // TODO: find Java SDK jars and put them here too
    }

    public JavaFileObject findProjectClass(String className) throws IOException
    {
        return fileManager.getJavaFileForInput(StandardLocation.SOURCE_PATH, className, JavaFileObject.Kind.SOURCE);
    }

    public JavaFileObject findDependencyClass(String className) throws IOException
    {
        JavaFileObject jfo = fileManager.getJavaFileForInput(
            StandardLocation.CLASS_PATH,
            className,
            JavaFileObject.Kind.SOURCE
        );
        if (jfo == null) {
            return null;
        }
        String[] classParts = className.split("\\.");
        String classShortName = classParts[classParts.length - 1];
        String[] packageParts = Arrays.copyOfRange(classParts, 0, classParts.length - 1);
        Path tmppkg = Path.of("/tmp/java-language-server", packageParts);
        Files.createDirectories(tmppkg);
        Path tmpJava = Path.of(tmppkg.toString(), classShortName+".java");
        Files.copy(jfo.openInputStream(), tmpJava, StandardCopyOption.REPLACE_EXISTING);
        return new SourceFileObject(tmpJava);
    }

}
