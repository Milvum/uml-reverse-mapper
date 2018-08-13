package com.milvum.urm;

import com.milvum.urm.domain.DomainClass;
import com.milvum.urm.presenters.Presenter;
import com.milvum.urm.presenters.Representation;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.*;
import org.apache.maven.project.MavenProject;
import org.reflections.Reflections;

@Mojo(name = "map", defaultPhase = LifecyclePhase.PROCESS_CLASSES, requiresDependencyResolution = ResolutionScope.COMPILE)
public class DomainMapperMojo extends AbstractMojo {

    @Parameter(defaultValue = "${project.basedir}", property = "outputDir", required = true)
    private File outputDirectory;
    @Component
    private MavenProject project;

    @Parameter(property = "map.packages", required = true)
    List<String> packages;

    @Parameter(property = "map.ignores")
    List<String> ignores;
    @Parameter(property = "map.fieldIgnores")
    private List<String> fieldIgnores;
    @Parameter(property = "map.methodIgnores")
    private List<String> methodIgnores;

    @Parameter(property = "presenter")
    private String presenterString;

    @Parameter(property = "map.skipForProjects")
    private List<String> skipForProjects;

    @Parameter(defaultValue = "true", property = "overwrite")
    private boolean overwrite;

    public void execute() throws MojoExecutionException, MojoFailureException {
        if (skipForProjects != null && !skipForProjects.isEmpty()) {
            String projectName = project.getName();
            if (skipForProjects.contains(projectName)) {
                getLog().info("Skip configured (in pom.xml) for current project \"" + projectName + "\". "
                    + "Plugin will not be executed!");
                return;
            }
        }

        if (packages == null || packages.isEmpty()) {
            throw new MojoFailureException("No packages defined for scanning.");
        }
        try {
            Presenter presenter = Presenter.parse(presenterString);

            String fileName = project.getName() + ".urm." + presenter.getFileEnding();
            Path path = Paths.get(outputDirectory.getPath(), fileName);

            if (!getLog().isDebugEnabled()) {
                // nullify the Reflections logger to prevent it from spamming
                // the console if we aren't in debug mode
                Reflections.log = null;
            }

            if (overwrite || !Files.exists(path)) {
                List<URL> projectClasspathList = getClasspathUrls();

                if (fieldIgnores != null && !fieldIgnores.isEmpty()) {
                    DomainClass.IGNORED_FIELDS = fieldIgnores;
                }
                if (methodIgnores != null && !methodIgnores.isEmpty()) {
                    DomainClass.IGNORED_METHODS = methodIgnores;
                }

                DomainMapper mapper = DomainMapper.create(presenter, packages, ignores,
                    new URLClassLoader(projectClasspathList.toArray(new URL[projectClasspathList.size()])));

                Representation representation = mapper.describeDomain();
                Files.write(path, representation.getContent().getBytes());
                getLog().info(fileName + " successfully written to: \"" + path + "\"!");
            } else {
                getLog().info(fileName + " already exists, file was not overwritten!");
            }
        } catch (ClassNotFoundException | DependencyResolutionRequiredException | IOException e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }
    }

    @SuppressWarnings("unchecked")
    private List<URL> getClasspathUrls() throws DependencyResolutionRequiredException, MojoExecutionException {
        List<URL> projectClasspathList = new ArrayList<>();
        for (String element : (List<String>) project.getCompileClasspathElements()) {
            try {
                projectClasspathList.add(new File(element).toURI().toURL());
            } catch (MalformedURLException e) {
                throw new MojoExecutionException(element + " is an invalid classpath element", e);
            }
        }
        return projectClasspathList;
    }
}
