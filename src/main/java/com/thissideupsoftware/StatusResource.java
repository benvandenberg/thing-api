package com.thissideupsoftware;

import javax.ws.rs.HEAD;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

/**
 * Returns the status of the application.
 */
@Path("/status")
public class StatusResource {

    /**
     * Return the status of the application.
     *
     * @return The status of the application.
     */
    @HEAD
    public Response status() {
        return Response.ok().build();
    }
}
