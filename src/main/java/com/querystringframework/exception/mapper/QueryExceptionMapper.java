package com.querystringframework.exception.mapper;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import com.querystringframework.exception.QueryException;

/**
 *
* @author reinaldo.locatelli
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
