package org.onehippo.examination;


import java.util.Map;

public interface Examination {

    Map<String, Object> diagnoseComponents();

    Map<String, Object> diagnoseSitemaps();

    Map<String, Object> diagnoseMenus();

    Map<String, Object> diagnoseTemplates();

}
