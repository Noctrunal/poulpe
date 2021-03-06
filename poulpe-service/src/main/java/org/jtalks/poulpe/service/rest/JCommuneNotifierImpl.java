/**
 * Copyright (C) 2011  JTalks.org Team
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */
package org.jtalks.poulpe.service.rest;

import org.apache.commons.lang3.StringUtils;
import org.jtalks.poulpe.model.dao.UserDao;
import org.jtalks.poulpe.model.entity.PoulpeBranch;
import org.jtalks.poulpe.model.entity.PoulpeSection;
import org.jtalks.poulpe.service.JCommuneNotifier;
import org.jtalks.poulpe.service.exceptions.JcommuneRespondedWithErrorException;
import org.jtalks.poulpe.service.exceptions.JcommuneUrlNotConfiguredException;
import org.jtalks.poulpe.service.exceptions.NoConnectionToJcommuneException;
import org.restlet.Context;
import org.restlet.data.Method;
import org.restlet.representation.EmptyRepresentation;
import org.restlet.resource.ClientResource;
import org.restlet.resource.ResourceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.annotations.VisibleForTesting;

/**
 * Notifier to notify JCommune component about elements deleting. It is useful to help forum keep such information, as
 * user's messages count, up to date.
 * @author Evgeny Kapinos
 */
public class JCommuneNotifierImpl implements JCommuneNotifier {
    /**
     * A link which means 'delete the whole component' which will cause all the topics from all the branches to be
     * removed by JCommune.
     */
    private static final String WHOLEFORUM_URL_PART = "/component";
    /** A URL to trigger re-indexing of forum search engine. */
    private static final String REINDEX_URL_PART = "/search/index/rebuild";
    /** URL to ask JCommune to remove content of the specific section. */
    private static final String SECTIONS_URL_PART = "/sections/";
    /** URL to ask JCommune to remove content of the specific branch. */
    private static final String BRANCH_URL_PART = "/branches/";
    
    /**
     * If either connection can't be established (e.g. firewall drops it) or connection was established, but response
     * is not coming back during this timeout, then the connection will be dropped. Note, that these are milliseconds.
     */
    private static final int CONNECTION_TIMEOUT = 5000;   
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final UserDao userDao;

    /**
     * {@inheritDoc}
     */
    public JCommuneNotifierImpl(UserDao userDao) {
        this.userDao = userDao;
    }
    /**
     * {@inheritDoc}
     */
    @Override
    public void notifyAboutSectionDelete(String jCommuneUrl, PoulpeSection section) 
            throws NoConnectionToJcommuneException, JcommuneRespondedWithErrorException,
                   JcommuneUrlNotConfiguredException {      
        checkUrlIsConfigured(jCommuneUrl);
        notifyJCommune(jCommuneUrl + SECTIONS_URL_PART + section.getId(), Method.DELETE);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void notifyAboutBranchDelete(String jCommuneUrl, PoulpeBranch branch)
            throws NoConnectionToJcommuneException, JcommuneRespondedWithErrorException, 
                   JcommuneUrlNotConfiguredException {        
        checkUrlIsConfigured(jCommuneUrl);
        notifyJCommune(jCommuneUrl + BRANCH_URL_PART + branch.getId(), Method.DELETE);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void notifyAboutComponentDelete(String jCommuneUrl) 
            throws NoConnectionToJcommuneException, JcommuneRespondedWithErrorException, 
                   JcommuneUrlNotConfiguredException {       
        checkUrlIsConfigured(jCommuneUrl);
        notifyJCommune(jCommuneUrl + WHOLEFORUM_URL_PART, Method.DELETE);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void notifyAboutReindexComponent(String jCommuneUrl) 
            throws NoConnectionToJcommuneException, JcommuneRespondedWithErrorException, 
                   JcommuneUrlNotConfiguredException {      
        checkUrlIsConfigured(jCommuneUrl);
        notifyJCommune(jCommuneUrl + REINDEX_URL_PART, Method.POST);
    }

    /**
     * Notifies JCommune that an element is about to be deleted (for instance Component, Branch, Section).
     *
     * @param url full URL for REST request  
     * @param method delete or post method, see {@link Method}
     * @throws NoConnectionToJcommuneException some connection problems happened, while trying to notify JCommune
     * @throws JcommuneRespondedWithErrorException occurs when the response status is not {@code OK 200}
     */
    @VisibleForTesting
    protected void notifyJCommune(String url, Method method) 
            throws NoConnectionToJcommuneException, JcommuneRespondedWithErrorException {               
        logSendRequest(url, method);
        
        String adminPassword = userDao.getByUsername("admin").getPassword();
        
        ClientResource clientResource = new ClientResource(new Context(), url + "?password=" + adminPassword);
        /*
         * How to set parameters described here:
         * http://wiki.restlet.org/docs_2.1/13-restlet/27-restlet/325-restlet/37-restlet.html
         * Which parameters described here:
         * org.restlet.engine.connector.ClientConnectionHelper, string "socket.setSoTimeout(getMaxIoIdleTimeMs());"
         */
        clientResource.getContext().getParameters().add("socketConnectTimeoutMs", String.valueOf(CONNECTION_TIMEOUT));
        clientResource.getContext().getParameters().add("maxIoIdleTimeMs", String.valueOf(CONNECTION_TIMEOUT));
        try{
            if (method.equals(Method.DELETE)){
                clientResource.delete();
            } else {
                clientResource.post(new EmptyRepresentation());      
            }
        } catch (ResourceException e){
            processResourceException(e);
        }              
    }
    
    /**     
     * Checks URL
     *
     * @param jCommuneUrl JCommune URL
     * @throws JcommuneUrlNotConfiguredException occurs when the {@code jCommuneUrl} is incorrect
     */
    protected void checkUrlIsConfigured(String jCommuneUrl) throws JcommuneUrlNotConfiguredException {
        if (StringUtils.isBlank(jCommuneUrl)) {
            throw new JcommuneUrlNotConfiguredException();
        }
    }

    /**
     * Process an error that was receive from JCommune in the REST response. 
     *
     * @param e exception from REST framework
     * @throws JcommuneRespondedWithErrorException
     *          if REST request reached JCommune, but JCommune responded with error code, such situation may happen for
     *          instance when we're deleting some branch, but it was already deleted, or JCommune has troubles removing
     *          that branch (database connection lost). Note that if we reach some other site and it responds with 404
     *          for example, this will be still this error.
     * @throws NoConnectionToJcommuneException
     *          if nothing was found at the specified URL, note that if URL was set incorrectly to point to another
     *          site, this can't be figured out by us, we just operate with HTTP codes, which means that either the
     *          request will be fine or {@link JcommuneRespondedWithErrorException} might be thrown in case if some
     *          other site was specified and it returned 404
     */
    private void processResourceException(ResourceException e)
            throws NoConnectionToJcommuneException, JcommuneRespondedWithErrorException {        
        logResponseError(e);        
        if (e.getStatus().isConnectorError()){
            throw new NoConnectionToJcommuneException(e);
        } else{
            throw new JcommuneRespondedWithErrorException(e);
        }
    }
    
    /**
     * Logs all JCommune in the requests. 
     * @param url full URL for REST request  
     * @param method delete or post method, see {@link Method}
     */
    private void logSendRequest(String url, Method method){
        logger.info("Sending {} request to JCommune: [{}]", method, url);         
    }
    
    /**
     * Logs an error that was receive from JCommune in the response. 
     * @param e exception from REST     
     */
    private void logResponseError(ResourceException e){
        logger.error("JCommune error: {}", e.getStatus());
    }
}
