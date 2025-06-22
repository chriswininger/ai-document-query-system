package com.wininger.spring_ai_demo.config.jooq;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import org.jooq.JSON;

import java.io.IOException;

// https://stackoverflow.com/questions/71570788/no-serializer-found-for-class-org-jooq-json-and-no-properties-discovered-to-crea
// retrieve json columns
public class JooqJsonSerializer extends StdSerializer<JSON> {
  public JooqJsonSerializer() {
    super(JSON.class);
  }

  @Override
  public void serialize(JSON value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
    gen.writeRawValue(value.data());
  }
}
