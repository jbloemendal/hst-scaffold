package org.onehippo.fold;


import org.onehippo.Route;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import java.util.List;
import java.util.Map;

public interface Folder {

    Route.Component fold(Node componentNode) throws RepositoryException;

    Map<String, Route> fold() throws RepositoryException;

}
