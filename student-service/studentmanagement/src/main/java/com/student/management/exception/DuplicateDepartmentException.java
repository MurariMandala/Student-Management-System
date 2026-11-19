package com.student.management.exception;

public class DuplicateDepartmentException extends RuntimeException{

    public DuplicateDepartmentException(String department){
        super("Department: "+department+" is already exist");
    }
}
