package org.onehippo.build;

import javax.jcr.RepositoryException;
import java.io.IOException;

public interface ScaffoldBuilder extends Rollback {

    public void build(boolean dryRun) throws Exception;

}
