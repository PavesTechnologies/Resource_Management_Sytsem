package com.global_exception_handler;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
public class ClientExceptionHandler extends RuntimeException{
    private String ExceptionMessage;
}
