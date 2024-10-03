package org.javacs;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.logging.Logger;
import javax.tools.*;

public class Docs {

    /** File manager with source-path + platform sources, which we will use to look up individual source files */
    final SourceFileManager fileManager = new SourceFileManager();

    Docs(Set<Path> sourcePaths, Set<Path> sourceJarPaths) {
        try {
            fileManager.setLocationFromPaths(StandardLocation.SOURCE_PATH, sourcePaths);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        try {
            Set<Path> unzippedSrcJarPaths = new HashSet<>();
            for (Path sourceJarPath : sourceJarPaths) {
                FileSystem sourceJarFS = FileSystems.newFileSystem(sourceJarPath, Docs.class.getClassLoader());
                unzippedSrcJarPaths.add(sourceJarFS.getPath("/"));
            }
            fileManager.setLocationFromPaths(StandardLocation.MODULE_SOURCE_PATH, unzippedSrcJarPaths);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
