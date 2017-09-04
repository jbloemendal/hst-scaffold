package org.onehippo.examination;


import java.util.Map;

public interface Examination {

    Map<String, Diagnose> diagnoseComponents();

    Map<String, Diagnose> diagnoseSitemaps();

    Map<String, Diagnose> diagnoseMenus();

    Map<String, Diagnose> diagnoseTemplates();

}
