package com.global_exception_handler;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
public class ClientException extends RuntimeException{
    private String ExceptionMessage;
}
