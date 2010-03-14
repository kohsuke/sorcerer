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

    /**
     * Loading package-list.js in this base URL currently.
     */
    private String loading;

    private final JsArray<String> queue = JsArray.create();

    public void load(String baseURL) {
        // load one at a time to retain the base URL information.
        if (loading!=null) {
            queue.push(baseURL);
        } else {
            loading = baseURL;
            ScriptLoader.load(baseURL+"/package-list.js");
        }
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
        INSTANCE._define(prj);
    }

    private void _define(JsArray<Project> prj) {
        for (Project p : prj.iterable()) {
            p.init(loading);

            // TODO: version conflict resolution and duplicate reduction.
            projects.put(p.id(),p);
            for (int i=0; i<listeners.length(); i++)
                listeners.get(i).onChange(p);
        }

        loading = null;
        if (queue.length()>0)
            load(queue.pop());
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
