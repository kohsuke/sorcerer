package dummy;

/**
 * @author Kohsuke Kawaguchi
 */
class AnonymousType {
    void foo() {
        new Runnable() {
            public void run() {}
        };
    }
}
