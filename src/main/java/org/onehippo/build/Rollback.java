package org.onehippo.build;


import javax.jcr.RepositoryException;
import java.io.IOException;

public interface Rollback {

    public void backup(boolean dryRun) throws IOException, RepositoryException;

    public void rollback(boolean dryRun) throws IOException, RepositoryException;

}
