package com.chriswininger.api.dto.inferenceresults;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Target({FIELD, TYPE})
@Retention(RUNTIME)
/*
Decorate fields in a record, allowing us to build the format block
for structured inference requests
*/
public @interface InferenceDescription {
    String[] value();
}
