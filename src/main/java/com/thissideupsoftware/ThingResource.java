package com.thissideupsoftware;

import javax.validation.Valid;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.UUID;

@Path("things")
public class ThingResource {

    private ThingDbService thingDbService;

    public ThingResource(ThingDbService thingDbService) {
        this.thingDbService = thingDbService;
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response create(@Valid Thing thing) {
        thingDbService.save(thing);
        UUID id = thing.getId();
        URI location = null;
        try {
            location = new URI("things/" + id);
        } catch (URISyntaxException e) {
            // Ignore
        }
        return Response.created(location).entity(thing).build();
    }

    @GET
    @Path("{uuid}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response get(@PathParam("uuid") UUID uuid) {
        Thing thing = thingDbService.get(uuid);
        if (thing == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        } else {
            return Response.ok(thing).build();
        }
    }
}
