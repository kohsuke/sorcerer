package org.jvnet.sorcerer;

import org.jvnet.sorcerer.util.JsonWriter;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Collections;

/**
 * Represents an external project that this project depends on.
 *
 * @author Kohsuke Kawaguchi
 */
public abstract class Dependency implements JsonWriter.Writable {
    /**
     * List of packages that this dependency provides.
     */
    public final PackageSet packageNames = new PackageSet();

    public final String name;

    /**
     * The URL of the javadoc or sorcerer output of this external project.
     */
    public final URL baseUrl;

    private Dependency(String name, URL baseUrl) {
        this.name = name;
        this.baseUrl = baseUrl;
    }

    public void write(JsonWriter w) {
        w.startObject();
        w.property("name",name);
        w.property("baseURL",baseUrl);
        w.propertyUnquoted("linker",getLinker());
        w.property("packages",packageNames);
        w.endObject();
    }

    abstract String getLinker();


    /**
     * Dependency to an external javadoc site.
     */
    public static final class Javadoc extends Dependency {
        public Javadoc(String name, URL baseUrl) throws IOException {
            this(name,baseUrl,null);
        }

        public Javadoc(String name, URL baseUrl, File localPackageInfo) throws IOException {
            super(name,baseUrl);

            URL pkgList;
            if(localPackageInfo==null)   pkgList = new URL(baseUrl,"package-list");
            else
                pkgList = localPackageInfo.toURI().toURL();

            parsePackageInfo(pkgList);
        }

        String getLinker() {
            return "linker.javadoc";
        }

        /**
         * Parses a "package-info" file into a set of package names.
         */
        private void parsePackageInfo(URL packageInfo) throws IOException {
            BufferedReader in = new BufferedReader(new InputStreamReader(packageInfo.openStream(),"UTF-8"));

            String line;
            while((line=in.readLine())!=null) {
                packageNames.add(line);
            }
            Collections.sort(packageNames);
        }
    }

    /**
     * Dependency to an external sorcerer site.
     */
    public static final class Sorcerer extends Dependency {
        public Sorcerer(String name, URL baseUrl) {
            super(name,baseUrl);
        }

        String getLinker() {
            return "linker.sorcerer";
        }
    }
}
