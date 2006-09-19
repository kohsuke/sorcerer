/*
 * Copyright 2001-2006 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jvnet.sorcerer.maven_plugin;

import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.doxia.siterenderer.Renderer;
import org.apache.maven.model.ReportPlugin;
import org.apache.maven.project.MavenProject;
import org.apache.maven.reporting.AbstractMavenReport;
import org.apache.maven.reporting.MavenReportException;
import org.jvnet.sorcerer.Analyzer;
import org.jvnet.sorcerer.InternalLinkResolverFactory;
import org.jvnet.sorcerer.JavadocLinkResolverFactory;
import org.jvnet.sorcerer.LinkResolverFacade;
import org.jvnet.sorcerer.LinkResolverFactory;
import org.jvnet.sorcerer.ParsedSourceSet;
import org.jvnet.sorcerer.frame.FrameSetGenerator;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

/**
 * Base class for the Sorcerer reports.
 */
public abstract class AbstractSorcererReport
    extends AbstractMavenReport {
    /**
     * @parameter expression="${project}"
     * @required
     * @readonly
     */
    protected MavenProject project;

    /**
     * @component
     */
    private Renderer siteRenderer;

    /**
     * Output folder where the main page of the report will be generated.
     *
     * @parameter expression="${project.reporting.outputDirectory}"
     * @required
     */
    private String outputDirectory;

    /**
     * File input encoding.
     *
     * @parameter default-value="ISO-8859-1"
     */
    private String encoding;

    /**
     * Title of window of the Xref HTML files.
     *
     * @parameter expression="${project.name} ${project.version} Reference"
     */
    private String windowTitle;

    /**
     * Style sheet used for the Xref HTML files.
     * Should not be used. If used, should be an absolute path, like "${basedir}/myStyles.css".
     *
     * @parameter default-value="stylesheet.css"
     */
    private String stylesheet;

    /**
     * Tab width (how many spaces is one tab worth?)
     *
     * @parameter default-value="8"
     */
    private int tabWidth;

    /**
     * The projects in the reactor for aggregation report.
     *
     * @parameter expression="${reactorProjects}"
     * @readonly
     */
    protected List<MavenProject> reactorProjects;

    /**
     * Links to external javadocs.
     *
     * @parameter expression="${javadocs}"
     */
    private Javadoc[] javadocs;

    /**
     * Whether to build an aggregated report at the root, or build individual reports.
     *
     * @parameter expression="${aggregate}" default-value="false"
     */
    protected boolean aggregate;


    /**
     * Compiles the list of directories which contain source files that will be included in the JXR report generation.
     *
     * @param sourceDirs the List of the source directories
     * @return a List of the directories that will be included in the JXR report generation
     */
    protected List<String> pruneSourceDirs(List<String> sourceDirs) {
        List<String> pruned = new ArrayList<String>(sourceDirs.size());
        for (String dir : sourceDirs) {
            if (!pruned.contains(dir) && hasSources(new File(dir))) {
                pruned.add(dir);
            }
        }
        return pruned;
    }

    /**
     * Initialize some attributes required during the report generation
     */
    protected void init() {
        // wanna know if Javadoc is being generated
        // TODO: what if it is not part of the site though, and just on the command line?
        Collection plugin = project.getReportPlugins();
        if (plugin != null) {
            for (Object aPlugin : plugin) {
                ReportPlugin reportPlugin = (ReportPlugin) aPlugin;
                if ("maven-javadoc-plugin".equals(reportPlugin.getArtifactId())) {
                    break;
                }
            }
        }
    }

    /**
     * Checks whether the given directory contains Java files.
     *
     * @param dir the source directory
     * @return true if the folder or one of its subfolders coantins at least 1 Java file
     */
    private boolean hasSources(File dir) {
        boolean found = false;
        if (dir.exists() && dir.isDirectory()) {
            File[] files = dir.listFiles();
            for (int i = 0; i < files.length && !found; i++) {
                File currentFile = files[i];
                if (currentFile.isFile() && currentFile.getName().endsWith(".java")) {
                    found = true;
                } else if (currentFile.isDirectory()) {
                    boolean hasSources = hasSources(currentFile);
                    if (hasSources) {
                        found = true;
                    }
                }
            }
        }
        return found;
    }

    protected Renderer getSiteRenderer() {
        return siteRenderer;
    }

    protected String getOutputDirectory() {
        return outputDirectory;
    }

    public MavenProject getProject() {
        return project;
    }

    protected ResourceBundle getBundle(Locale locale) {
        return ResourceBundle.getBundle(AbstractSorcererReport.class.getName(), locale);
    }

    protected boolean canGenerateReport(List sourceDirs) {
        boolean canGenerate = !sourceDirs.isEmpty();

        if (aggregate && !project.isExecutionRoot()) {
            canGenerate = false;
        }
        return canGenerate;
    }

    private LinkResolverFactory createLinkResolverFactory() throws IOException {
        List<LinkResolverFactory> list = new ArrayList<LinkResolverFactory>();
        for (Javadoc j : javadocs) {
            list.add(new JavadocLinkResolverFactory(j.href,j.packageList));
        }
        list.add(new InternalLinkResolverFactory());

        return new LinkResolverFacade(list.toArray(new LinkResolverFactory[list.size()]));
    }

    protected void executeReport(Locale locale) throws MavenReportException {
        List<String> sourceDirs = constructSourceDirs();
        if (canGenerateReport(sourceDirs)) {
            // init some attributes -- TODO (javadoc)
            init();

            try {
                Analyzer a = new Analyzer();
                for (String dir : sourceDirs) {
                    a.addSourceFolder(new File(dir));
                }

                for( String path : getClasspathElements() ) {
                    a.addClasspath(new File(path));
                }


                a.setSourceEncoding(encoding);
                a.setLocale(locale);
                ParsedSourceSet pss = a.analyze(new Listener(getLog()));
                pss.setTabWidth(tabWidth);
                pss.setLinkResolverFactory(createLinkResolverFactory());

                // TODO: support i18n and use locale for HTML generation
                FrameSetGenerator fsg = new FrameSetGenerator(pss);
                fsg.setTitle(windowTitle);
                fsg.generateAll(new File(getDestinationDirectory()));
            } catch (IOException e) {
                throw new MavenReportException("Error while generating the HTML source code of the projet.", e);
            } catch (DependencyResolutionRequiredException e) {
                throw new MavenReportException("Failed to resolve dependencies",e);
            }
        }
    }

    /**
     * Gets the list of the source directories to be included in the JXR report generation
     *
     * @return a List of the source directories whose contents will be included in the JXR report generation
     */
    protected List<String> constructSourceDirs() {
        List<String> sourceDirs = new ArrayList<String>(getSourceRoots());
        if (aggregate) {
            for (MavenProject project : reactorProjects) {
                if ("java".equals(project.getArtifact().getArtifactHandler().getLanguage())) {
                    sourceDirs.addAll(getSourceRoots(project));
                }
            }
        }

        sourceDirs = pruneSourceDirs(sourceDirs);
        return sourceDirs;
    }

    public boolean canGenerateReport() {
        return canGenerateReport(constructSourceDirs());
    }

    public boolean isExternalReport() {
        return true;
    }

    /**
     * Abstract method that returns the target directory where the generated JXR reports will be put.
     *
     * @return a String that contains the target directory name
     */
    protected abstract String getDestinationDirectory();

    /**
     * Abstract method that returns the specified source directories that will be included in the JXR report generation.
     *
     * @return a List of the source directories
     */
    protected abstract List<String> getSourceRoots();

    /**
     * Abstract method that returns the compile source directories of the specified project that will be included in the
     * JXR report generation
     *
     * @param project the MavenProject where the JXR report plugin will be executed
     * @return a List of the source directories
     */
    protected abstract List<String> getSourceRoots(MavenProject project);

    /**
     * Abstract method that returns the location of the javadoc files.
     *
     * @return a String that contains the loaction of the javadocs
     */
    protected abstract String getJavadocLocation();

    /**
     * Gets the classpath to be used for analyzing source files.
     */
    protected abstract List<String> getClasspathElements() throws DependencyResolutionRequiredException;
}
