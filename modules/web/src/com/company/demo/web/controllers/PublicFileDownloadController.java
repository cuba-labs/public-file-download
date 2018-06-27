/*
 * TODO Copyright
 */

package com.company.demo.web.controllers;

import com.company.demo.entity.PublicFile;
import com.google.common.base.Strings;
import com.haulmont.cuba.client.ClientConfig;
import com.haulmont.cuba.core.app.DataService;
import com.haulmont.cuba.core.entity.FileDescriptor;
import com.haulmont.cuba.core.global.Configuration;
import com.haulmont.cuba.core.global.FileTypesHelper;
import com.haulmont.cuba.core.global.GlobalConfig;
import com.haulmont.cuba.core.global.LoadContext;
import com.haulmont.cuba.core.sys.AppContext;
import com.haulmont.cuba.core.sys.SecurityContext;
import com.haulmont.cuba.core.sys.remoting.ClusterInvocationSupport;
import com.haulmont.cuba.security.app.LoginService;
import com.haulmont.cuba.security.global.UserSession;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.BasicHttpClientConnectionManager;
import org.slf4j.Logger;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

import javax.annotation.Resource;
import javax.inject.Inject;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.UUID;

@Controller("demo_PublicFileDownloadController")
public class PublicFileDownloadController {
    @Inject
    private Logger log;

    @Inject
    private DataService dataService;

    @Inject
    private LoginService loginService;

    @Inject
    private Configuration configuration;

    @Resource(name = ClusterInvocationSupport.NAME)
    private ClusterInvocationSupport clusterInvocationSupport;

    private String fileDownloadContext;

    @Inject
    public void setConfiguration(Configuration configuration) {
        this.fileDownloadContext = configuration.getConfig(ClientConfig.class).getFileDownloadContext();
    }

    @RequestMapping(value = "/published/{linkId}", method = RequestMethod.GET)
    public ModelAndView download(HttpServletRequest request, HttpServletResponse response,
                                 @PathVariable("linkId") String linkId) throws IOException {

        GlobalConfig config = configuration.getConfig(GlobalConfig.class);
        UUID anonymousSessionId = config.getAnonymousSessionId();
        UserSession anonymousSession = loginService.getSession(anonymousSessionId);
        if (anonymousSession == null) {
            log.error("Unable to obtain anonymous session");
            error(response);
            return null;
        }

        AppContext.setSecurityContext(new SecurityContext(anonymousSession));
        try {
            LoadContext<PublicFile> lc = new LoadContext<>(PublicFile.class)
                    .setView("publicFile-view")
                    .setQuery(
                            new LoadContext.Query("select f from demo$PublicFile f where f.linkId = :linkId")
                                    .setParameter("linkId", linkId)
                    );
            PublicFile publicFile = dataService.load(lc);
            if (publicFile == null) {
                log.warn("Unable to find file with id {}", linkId);
                error(response);
                return null;
            }

            FileDescriptor fd = publicFile.getFile();

            String fileName;
            try {
                fileName = URLEncoder.encode(fd.getName(), StandardCharsets.UTF_8.name());
            } catch (UnsupportedEncodingException e) {
                log.error(e.toString());
                error(response);
                return null;
            }

            response.setHeader("Cache-Control", "no-cache");
            response.setHeader("Pragma", "no-cache");
            response.setDateHeader("Expires", 0);
            response.setHeader("Content-Type", getContentType(fd));
            response.setHeader("Pragma", "no-cache");

            boolean attach = Boolean.valueOf(request.getParameter("a"));
            response.setHeader("Content-Disposition", (attach ? "attachment" : "inline")
                    + "; filename=" + fileName);

            writeResponse(response, anonymousSession, fd);
        } finally {
            AppContext.setSecurityContext(null);
        }
        return null;
    }

    private void writeResponse(HttpServletResponse response, UserSession userSession, FileDescriptor fd)
            throws IOException {
        InputStream is = null;
        ServletOutputStream os = response.getOutputStream();
        try {
            for (Iterator<String> iterator = clusterInvocationSupport.getUrlList().iterator(); iterator.hasNext(); ) {
                String url = iterator.next() + fileDownloadContext +
                        "?s=" + userSession.getId() +
                        "&f=" + fd.getId().toString();

                HttpClientConnectionManager connectionManager = new BasicHttpClientConnectionManager();
                HttpClient httpClient = HttpClientBuilder.create()
                        .setConnectionManager(connectionManager)
                        .build();

                HttpGet httpGet = new HttpGet(url);

                try {
                    HttpResponse httpResponse = httpClient.execute(httpGet);
                    int httpStatus = httpResponse.getStatusLine().getStatusCode();
                    if (httpStatus == HttpStatus.SC_OK) {
                        HttpEntity httpEntity = httpResponse.getEntity();
                        if (httpEntity != null) {
                            is = httpEntity.getContent();
                            IOUtils.copy(is, os);
                            os.flush();
                            break;
                        } else {
                            log.debug("Unable to download file from " + url + "\nHttpEntity is null");
                            if (iterator.hasNext())
                                log.debug("Trying next URL");
                            else
                                error(response);
                        }
                    } else {
                        log.debug("Unable to download file from " + url + "\n" + httpResponse.getStatusLine());
                        if (iterator.hasNext()) {
                            log.debug("Trying next URL");
                        } else {
                            error(response);
                        }
                    }
                } catch (IOException ex) {
                    log.debug("Unable to download file from " + url + "\n" + ex);
                    if (iterator.hasNext()) {
                        log.debug("Trying next URL");
                    } else {
                        error(response);
                    }
                } finally {
                    IOUtils.closeQuietly(is);

                    connectionManager.shutdown();
                }
            }
        } finally {
            IOUtils.closeQuietly(os);
        }
    }

    private String getContentType(FileDescriptor fd) {
        if (Strings.isNullOrEmpty(fd.getExtension())) {
            return FileTypesHelper.DEFAULT_MIME_TYPE;
        }

        return FileTypesHelper.getMIMEType("." + fd.getExtension().toLowerCase());
    }

    private void error(HttpServletResponse response) throws IOException {
        if (!response.isCommitted()) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }
}