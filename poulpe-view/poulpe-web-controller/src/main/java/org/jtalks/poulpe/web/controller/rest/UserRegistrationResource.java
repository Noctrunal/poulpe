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
package org.jtalks.poulpe.web.controller.rest;

import org.apache.http.HttpStatus;
import org.jtalks.poulpe.service.UserService;
import org.jtalks.poulpe.service.exceptions.ValidationException;
import org.jtalks.poulpe.web.controller.rest.pojo.*;
import org.jtalks.poulpe.web.controller.rest.pojo.Error;
import org.restlet.data.Status;
import org.restlet.ext.jaxb.JaxbRepresentation;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.ServerResource;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


/**
 * Implementation registration resource for users
 *
 * @author Mikhail Zaitsev
 */
public class UserRegistrationResource extends ServerResource implements RegistrationResource {

    private UserService userService;

    public UserRegistrationResource(UserService userService) {
        this.userService = userService;
    }

    /**
     * Registers user if the representation of the request is correct.
     * Sets statuses of response as is following:
     * 400 - if is some validation errors or if impossible to unmarshal request
     * 500 - if is some errors when is handling request
     * 200 - if user success registered
     *
     * @param represent request representation {@code User}
     * @return response representation {@code Error}
     */
    @Override
    public Representation register(Representation represent) {
        Error error = null;
        try {
            JaxbRepresentation<User> userRep = new JaxbRepresentation<User>(represent, User.class);
            User user = userRep.getObject();
            userService.registration(user.getUsername(), user.getPasswordHash(), user.getFirstName(), user.getLastName(), user.getEmail());
        } catch (ValidationException e) {
            error = ifValidationException(e);
            getResponse().setStatus(new Status(HttpStatus.SC_BAD_REQUEST));
        } catch (IOException e) {
            error = ifIOException();
            getResponse().setStatus(new Status(HttpStatus.SC_BAD_REQUEST));
        } catch (Exception e) {
            error = ifOtherException(e);
            getResponse().setStatus(new Status(HttpStatus.SC_INTERNAL_SERVER_ERROR));
        }
        Representation resultRep = null;
        if (error == null) {
            resultRep = new StringRepresentation(" ");  //because must be an empty response
        } else {
            resultRep = new JaxbRepresentation<Error>(error);
            ((JaxbRepresentation) resultRep).setFormattedOutput(true);
        }
        return resultRep;
    }

    /**
     * Creates {@code Error} object if thrown the {@code ValidationException}
     *
     * @param ex the {@code ValidationException}
     * @return the object {@code Error}
     */
    private Error ifValidationException(ValidationException ex) {
        org.jtalks.poulpe.web.controller.rest.pojo.Error result = new Error();
        result.setErrorMessage("There are validation errors");
        CodeErrorMessages templateErrorMessages = new CodeErrorMessages();
        templateErrorMessages.setCodeErrorMessage(formatsCodeErrors(ex.getTemplateMessages()));
        result.setCodeErrorMessages(templateErrorMessages);
        return result;
    }

    /**
     * Creates {@code Error} object if thrown the {@code IOException}
     *
     * @return the object {@code Error}
     */
    private Error ifIOException() {
        Error result = new Error();
        result.setErrorMessage(" Impossible to unmarshal request");
        return result;
    }

    /**
     * Creates {@code Error} object if thrown the {@code Exception}
     *
     * @param ex the {@code Exception}
     * @return the object {@code Error}
     */
    private Error ifOtherException(Exception ex) {
        Error result = new Error();
        result.setErrorMessage(ex.getMessage());
        return result;
    }

    /**
     * Removes '{' and '}' from code messages
     *
     * @param strings code messages
     * @return code messages without '{' and '}'
     */
    private List<String> formatsCodeErrors(List<String> strings) {
        List<String> result = new ArrayList<String>();
        for (String s : strings) {
            result.add(s.replaceAll("[{}]", ""));
        }
        return result;
    }
}