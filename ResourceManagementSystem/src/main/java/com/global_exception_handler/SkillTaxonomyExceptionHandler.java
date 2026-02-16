package com.global_exception_handler;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
public class SkillTaxonomyExceptionHandler extends RuntimeException {
    private String ExceptionMessage;
}
