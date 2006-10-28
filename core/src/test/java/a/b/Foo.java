package a.b;

/**
 * @author Kohsuke Kawaguchi
 */
public class Foo {
    Foo(int x, int y) {}
    Foo(Foo z) {}

    Foo test(Foo f) { return f; }
}
