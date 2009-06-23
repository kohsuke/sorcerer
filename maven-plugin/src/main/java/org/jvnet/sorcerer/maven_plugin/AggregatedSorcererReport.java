package org.jvnet.sorcerer.maven_plugin;

/**
 * @author Kohsuke Kawaguchi
 * @goal aggregate
 * @aggregator
 */
public class AggregatedSorcererReport extends SorcererReport {
    @Override
    protected boolean isAggregator() {
        return true;
    }
}
