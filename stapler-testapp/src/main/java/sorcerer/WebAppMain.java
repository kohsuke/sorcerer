package sorcerer;

import org.jvnet.sorcerer.Analyzer;
import org.jvnet.sorcerer.stapler.Sorcerer;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import java.io.File;
import java.io.IOException;

/**
 * @author Kohsuke Kawaguchi
 */
public class WebAppMain implements ServletContextListener {

    public void contextInitialized(ServletContextEvent servletContextEvent) {
        try {
            Analyzer a = new Analyzer();
            a.addSourceFolder(new File("src/main/java"));

            Sorcerer app = new Sorcerer(a.analyze());

            servletContextEvent.getServletContext().setAttribute("app",app);
        } catch (IOException e) {
            throw new Error(e);
        }
    }

    public void contextDestroyed(ServletContextEvent servletContextEvent) {
    }
}
