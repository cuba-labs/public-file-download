package com.company.demo.web.publicfile;

import com.company.demo.entity.PublicFile;
import com.haulmont.cuba.core.global.GlobalConfig;
import com.haulmont.cuba.gui.components.AbstractLookup;
import com.haulmont.cuba.gui.components.Component;
import com.haulmont.cuba.gui.components.Link;
import com.haulmont.cuba.gui.xml.layout.ComponentsFactory;

import javax.inject.Inject;

public class PublicFileBrowse extends AbstractLookup {

    @Inject
    private GlobalConfig globalConfig;

    @Inject
    private ComponentsFactory componentsFactory;

    public Component generateLink(PublicFile publicFile) {
        Link link = componentsFactory.createComponent(Link.class);
        link.setCaption("Download");
        link.setUrl(globalConfig.getWebAppUrl() + "/dispatch/published/" + publicFile.getLinkId());
        return link;
    }
}