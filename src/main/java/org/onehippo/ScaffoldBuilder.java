package org.onehippo;

import javax.jcr.RepositoryException;
import java.io.IOException;

public interface ScaffoldBuilder {

    public void build(boolean dryRun) throws Exception;

    public void rollback(boolean dryRun) throws IOException, RepositoryException;

}
