import a.b.X;
import a.b.En;

import java.util.Date;
import java.lang.annotation.Documented;

/**
 * @author Kohsuke Kawaguchi
 */
public class TestCode {

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