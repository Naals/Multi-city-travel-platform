package com.project.flightservice.exception;

public class NoRouteFoundException extends RuntimeException{
    public NoRouteFoundException(String msg){
        super(msg);
    }
}
