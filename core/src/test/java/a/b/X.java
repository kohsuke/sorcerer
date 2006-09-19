package a.b;

import java.util.List;
import java.util.ArrayList;

/**
 * @author Kohsuke Kawaguchi
 */
public class X {
    public List l = new ArrayList();
    En z;
    // overloading test
    public void foo(int x) {}
    public void foo(long x) {
        this.f = (int) x;
    }

    private int f;

    public static int y=5;
}
