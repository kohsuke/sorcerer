package sorcerer.client.data.pkg;

import sorcerer.client.ScriptLoader;
import sorcerer.client.js.JsArray;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Loads <tt>package-list.js</tt> as necessary.
 *
 * @author Kohsuke Kawaguchi
 */
public class ProjectLoader implements Iterable<Project> {
    private final Map<String,Project> projects = new HashMap<String, Project>();
    private final JsArray<Listener> listeners = JsArray.create();

    public void load(String baseURL) {
        ScriptLoader.load(baseURL+"/package-list.js");
    }

    public void addListener(Listener l) {
        listeners.push(l);
    }

    public Iterator<Project> iterator() {
        return projects.values().iterator();
    }

    public Project get(String id) {
        return projects.get(id);
    }

    /**
     * Loaded JavaScript will invoke this method.
     */
    static void define(JsArray<Project> prj) {
        for (Project p : prj.iterable()) {
            INSTANCE._define(p);
        }
    }

    private void _define(Project prj) {
        prj.init();

        // TODO: version conflict resolution and duplicate reduction.
        projects.put(prj.id(),prj);
        for (int i=0; i<listeners.length(); i++)
            listeners.get(i).onChange(prj);
    }

    public static ProjectLoader INSTANCE = new ProjectLoader();

    public native static void export() /*-{
        $wnd.setProject = $entry(@sorcerer.client.data.pkg.ProjectLoader::define(Lsorcerer/client/js/JsArray;));

        // TODO: fix the linker support
        $wnd.linker = {};
    }-*/;

    static {
        export();
    }

    public interface Listener {
        void onChange(Project pkg);
    }
}
