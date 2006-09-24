package a.b;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/**
 * @author Kohsuke Kawaguchi
 */
@Target({ElementType.PACKAGE, ElementType.TYPE})
@MyWay(X.class)
public @interface MyWay {
    Class value();
}
