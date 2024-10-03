package org.javacs;

import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;
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
        return fileManager.getJavaFileForInput(StandardLocation.CLASS_PATH, className, JavaFileObject.Kind.SOURCE);
    }

}
