package com.ths.simpleapplication.error;

public class BookIdMismatchException extends RuntimeException{
    public BookIdMismatchException(){
        super();
    }

    public BookIdMismatchException(String message, Throwable cause){
        super(message, cause);
    }

    public BookIdMismatchException(String message){
        super(message);
    }

    public BookIdMismatchException(Throwable cause){
        super(cause);
    }
}
