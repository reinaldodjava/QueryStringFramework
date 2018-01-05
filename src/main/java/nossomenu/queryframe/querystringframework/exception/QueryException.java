/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package nossomenu.queryframe.querystringframework.exception;

import nossomenu.queryframe.querystringframework.exception.status.BadRequest;

/**
 *
 * @author reina
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
