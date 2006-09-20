import a.b.En;
import a.b.X;

import java.lang.annotation.Documented;
import java.util.Date;

/**
 * @author Kohsuke Kawaguchi
 */
public class TestCode {

    PrivateClass p = new PrivateClass();
    AnotherLocal al = new AnotherLocal();

    TestCode tc = null; // self reference

    public class Base {
        public void run() {}
    }

    public class Inner extends Base {
        public void run() {}
    }

    public class Derived extends Inner implements Runnable {
        public void run() {
            super.run();
        }
    }

    public class Derived2 extends Inner implements Runnable {
        public void run() {
            super.run();
        }
    }

    public static void main(String[] args) {
        X x = new X();
        x.foo(3);
        x.foo(5L);
        x.l.add(x.l);
        System.out.println(new Date().toGMTString());
        System.out.println(x.y);
        L:
        switch(En.ABC) {
        case ABC:
        case DEF:
            break L;
        }
        Comparable<String> c = new Comparable<String>() {
            public int compareTo(String o) {
                class Foo {}
                Foo f = new Foo();
                // TODO
                throw new UnsupportedOperationException(o);
            }

            Comparable<String> c = new Comparable<String>() {
                public int compareTo(String o) {
                    class Foo {}
                    TestCode tc = new TestCode();
                    Foo f = new Foo();
                    // TODO
                    throw new UnsupportedOperationException(o);
                }
            };
        };
    }

    static {
        ; // initializer block
    }

    private int fieldDecl = 3;
    private final String anotherField = "555";

    @Documented
    @interface Foo {}
}

class PrivateClass {}

/*
long tail
long tail
long tail
long tail
long tail
long tail
long tail
long tail
long tail
long tail
long tail
long tail
long tail
long tail
long tail
long tail
long tail
long tail
long tail
long tail
long tail
long tail
long tail
long tail
long tail
long tail
long tail
long tail
long tail
long tail
long tail
long tail
long tail
long tail
long tail
long tail
long tail
long tail
long tail
long tail
long tail
long tail
long tail
long tail
long tail
long tail
long tail
long tail
long tail
long tail
v*/