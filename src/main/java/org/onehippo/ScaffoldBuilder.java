package org.onehippo;

public interface ScaffoldBuilder {

    public void dryRun() throws Exception;

    public void build() throws Exception;

    public void rollback();

}
