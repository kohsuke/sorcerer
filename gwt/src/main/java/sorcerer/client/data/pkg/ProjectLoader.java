package sorcerer.client.data.pkg;

import sorcerer.client.ScriptLoader;
import sorcerer.client.js.JsArray;

/**
 * Loads <tt>package-list.js</tt> as necessary.
 *
 * @author Kohsuke Kawaguchi
 */
public class ProjectLoader {
    private final JsArray<Project> projects = JsArray.create();
    private final JsArray<Listener> listeners = JsArray.create();

    public void load(String baseURL) {
        ScriptLoader.load(baseURL+"/package-list.js");
    }

    public void addListener(Listener l) {
        listeners.push(l);
    }

    /**
     * Loaded JavaScript will invoke this method.
     */
    static void define(Project pkg) {
        INSTANCE._define(pkg);
    }

    private void _define(Project pkg) {
        // TODO: version conflict resolution and duplicate reduction.
        projects.push(pkg);
        for (int i=0; i<listeners.length(); i++)
            listeners.get(i).onChange();
    }

    public static ProjectLoader INSTANCE = new ProjectLoader();

    public native static void export() /*-{
        $wnd.setProject = $entry(@sorcerer.client.data.pkg.ProjectLoader::define(Lsorcerer/client/data/pkg/Project;));
    }-*/;

    static {
        export();
    }

    public interface Listener {
        void onChange();
    }
}
