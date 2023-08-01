package com.ths.simpleapplication.error;

public class BookNotFoundException extends RuntimeException{
    public BookNotFoundException(){
        super();
    }
    public BookNotFoundException(String message, Throwable cause){
        super(message, cause);
    }

    public BookNotFoundException(String message){
        super(message);
    }
    public BookNotFoundException(Throwable cause){
        super(cause);
    }
}
