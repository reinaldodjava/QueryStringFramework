/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package nossomenu.queryframe.querystringframework.exception.mapper;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import nossomenu.queryframe.querystringframework.exception.QueryException;

/**
 *
 * @author reina
 */
@Provider
public class QueryExceptionMapper implements ExceptionMapper<QueryException> {

    @Override
    public Response toResponse(QueryException exception) {
        return Response.status(400)
                .type(MediaType.APPLICATION_JSON)
                .entity(exception.getBadRequest())
                .build();
    }

}
