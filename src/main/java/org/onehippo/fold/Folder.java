package org.onehippo.fold;


import org.onehippo.Route;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import java.util.List;

public interface Folder {

    List<Route> fold() throws RepositoryException;

    Route foldRoute(Node sitemapItem) throws RepositoryException;

}
