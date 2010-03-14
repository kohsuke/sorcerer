package sorcerer.client;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.ui.RootPanel;
import com.smartgwt.client.types.Overflow;
import com.smartgwt.client.types.VisibilityMode;
import com.smartgwt.client.util.Page;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.layout.HLayout;
import com.smartgwt.client.widgets.layout.SectionStack;
import com.smartgwt.client.widgets.layout.SectionStackSection;
import sorcerer.client.LazyDataLoader.Callback;
import sorcerer.client.data.SourceFileLoader;
import sorcerer.client.data.pkg.ClassListLoader;
import sorcerer.client.data.pkg.Klass;
import sorcerer.client.data.pkg.Project;
import sorcerer.client.data.pkg.ProjectLoader;
import sorcerer.client.js.JsArray;
import sorcerer.client.outline.OutlineTreeWidget;
import sorcerer.client.pkg.PackageTreeWidget;
import sorcerer.client.sourceview.SourceViewWidget;

import static java.lang.Math.max;


/**
 * Entry point classes define <code>onModuleLoad()</code>.
 */
public class Application implements EntryPoint {
    private SourceViewWidget mainCanvas;
    private static Application INSTANCE;
    private OutlineTreeWidget outline;

    public void onModuleLoad() {
        INSTANCE = this;
        Page.setAppImgDir("resource-files/");
        History.addValueChangeHandler(new ValueChangeHandler<String>() {
            public void onValueChange(ValueChangeEvent<String> e) {
                jumpTo(e.getValue());
            }
        });

        HLayout h = new HLayout();
        h.setWidth100();
        h.setHeight100();

        h.addMember(createLeft());
        h.addMember(createRightPane());

        RootPanel.get().add(h);

        SourceFileLoader.export();
        ProjectLoader.export();
        ClassListLoader.export();

        ProjectLoader.INSTANCE.load("data");
    }

    private SectionStack createLeft() {
        SectionStackSection pkg = new SectionStackSection("Package");
        pkg.setExpanded(true);
        pkg.setCanCollapse(true);
        pkg.addItem(new PackageTreeWidget());

        SectionStackSection outline = new SectionStackSection("Outline");
        outline.setExpanded(true);
        outline.setCanCollapse(true);
        outline.addItem(this.outline=new OutlineTreeWidget());

        SectionStack left = new SectionStack();
        left.setVisibilityMode(VisibilityMode.MULTIPLE);
        left.setHeight100();
        left.setWidth(200);
        left.addSection(pkg);
        left.addSection(outline);
        left.setShowResizeBar(true);
        return left;
    }

    private SectionStack createRightPane() {
        SectionStackSection pkg = new SectionStackSection("Source code");
        pkg.setExpanded(true);
        pkg.setShowHeader(false);
        pkg.addItem(mainCanvas=new SourceViewWidget());

        SectionStackSection search = new SectionStackSection("Search Result");
        search.setExpanded(false);
        search.setCanCollapse(true);
        search.addItem(new HelpCanvas("help4"));

        SectionStack right = new SectionStack();
        right.setVisibilityMode(VisibilityMode.MULTIPLE);
        right.setHeight100();
        right.setWidth100();
        right.addSection(pkg);
        right.addSection(search);

        return right;
    }

    class HelpCanvas extends Canvas {
        private String contents =  "<b>Severity 1</b> - Critical problem<br>System is unavailable in production or " +
              "is corrupting data, and the error severely impacts the user's operations." +
              "<br><br><b>Severity 2</b> - Major problem<br>An important function of the system " +
              "is not available in production, and the user's operations are restricted." +
              "<br><br><b>Severity 3</b> - Minor problem<br>Inability to use a function of the " +
              "system occurs, but it does not seriously affect the user's operations.";

        public HelpCanvas(String id) {
            setID(id);
            setPadding(10);
            setOverflow(Overflow.AUTO);
            setContents(contents);
        }
    }

    public SourceViewWidget getSourceView() {
        return mainCanvas;
    }

    public OutlineTreeWidget getOutlineView() {
        return outline;
    }

    /**
     * Restores the right state.
     */
    public void jumpTo(String historyId) {
        String pkgName = historyId.substring(0, max(0,historyId.lastIndexOf('.')));
        final String shortName = historyId.substring(historyId.lastIndexOf('.')+1);
        for (Project p : ProjectLoader.INSTANCE) {
            p.getPackage(pkgName).retrieveClassList(new Callback<JsArray<Klass>>() {
                public void call(JsArray<Klass> value) {
                    for (Klass k : value.iterable()) {
                        if (k.shortName().equals(shortName)) {
                            k.show();
                        }
                    }
                }
            });
        }
    }
    
    public static Application get() {
        return INSTANCE;
    }
}
