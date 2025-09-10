package com.clanboards.exceptions;

/** Thrown when a clan's war log is private (HTTP 403 on war endpoints). */
public class PrivateWarLogException extends RuntimeException {
    public PrivateWarLogException(String message) { super(message); }
}

