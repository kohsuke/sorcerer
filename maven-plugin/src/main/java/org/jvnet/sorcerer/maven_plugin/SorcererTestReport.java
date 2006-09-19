package org.jvnet.sorcerer.maven_plugin;

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

import org.apache.maven.project.MavenProject;
import org.apache.maven.artifact.DependencyResolutionRequiredException;

import java.io.File;
import java.util.List;
import java.util.Locale;

/**
 * Creates an html-based, cross referenced version of Java source code
 * for a project's test sources.
 *
 * @author <a href="mailto:bellingard.NO-SPAM@gmail.com">Fabrice Bellingard</a>
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 * @author Kohsuke Kawaguchi
 *
 * @goal test-sorcerer
 * @role org.apache.maven.reporting.MavenReport
 * @role-hint test-sorcerer
 * @instantiation-strategy per-lookup
 */
public class SorcererTestReport
    extends AbstractSorcererReport
{
    /**
     * Test directories of the project.
     *
     * @parameter expression="${project.testCompileSourceRoots}"
     * @required
     * @readonly
     */
    private List<String> sourceDirs;

    /**
     * Folder where the Xref files will be copied to.
     *
     * @parameter expression="${project.reporting.outputDirectory}/sorcerer-test"
     */
    private String destDir;

    protected List<String> getSourceRoots() {
        return this.sourceDirs;
    }

    protected List<String> getSourceRoots(MavenProject project) {
        return project.getTestCompileSourceRoots();
    }

    protected String getDestinationDirectory() {
        return destDir;
    }

    public String getDescription(Locale locale) {
        return getBundle(locale).getString("report.sorcerer.test.description");
    }

    public String getName(Locale locale) {
        return getBundle(locale).getString("report.sorcerer.test.name");
    }

    public String getOutputName() {
        return "sorcerer-test/index";
    }

    protected String getJavadocLocation() {
        // Don't link Javadoc
        return null;
    }

    protected List<String> getClasspathElements() throws DependencyResolutionRequiredException {
        return project.getTestCompileSourceRoots();
    }
}
