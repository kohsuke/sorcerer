package org.jvnet.sorcerer.ant;

import java.io.File;

/**
 * Nested &lt;javadoc> element inside {@link SorcererTask}.
 * @author Kohsuke Kawaguchi
 */
public class Javadoc {
    /**
     * URL to remote javadoc site.
     */
    private String href;

    private File packageList;

    public String getHref() {
        return href;
    }

    public void setHref(String href) {
        this.href = href;
    }

    public File getPackageList() {
        return packageList;
    }

    public void setPackageList(File packageList) {
        this.packageList = packageList;
    }
}
