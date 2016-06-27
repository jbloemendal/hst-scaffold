package org.onehippo;

public interface ScaffoldBuilder {

    public void dryRun();

    public void build();

    public void rollback();

}
