package com.querystringframework.exception;

import com.querystringframework.exception.status.BadRequest;

/**
 *
* @author reinaldo.locatelli
 */
public class QueryException extends RuntimeException{
    
    private BadRequest badRequest;

    public QueryException(String message) {
        this.badRequest = new BadRequest();
        this.badRequest.setMessage(message);        
    }

    public BadRequest getBadRequest() {
        return badRequest;
    }

    
}
