package com.google.gson;

import com.google.gson.internal.Streams;
import com.google.gson.internal.bind.JsonTreeReader;
import com.google.gson.internal.bind.JsonTreeWriter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.Writer;

/**
 * Utility class providing convenience conversions for {@link TypeAdapter}.
 *
 * <p>This class centralizes conversions between Java values and their JSON representations
 * using different input/output supports such as {@link Writer}, {@link Reader}, {@link String}
 * and {@link JsonElement}.
 */
final class TypeAdapterSupport {

    private TypeAdapterSupport() {}

    /**
     * Converts the given value to a JSON document and writes it to the specified writer.
     *
     * @param adapter the type adapter used for conversion
     * @param out the destination writer
     * @param value the Java value to serialize
     * @param <T> the adapted type
     * @throws IOException if an I/O error occurs
     */
    static <T> void toJson(TypeAdapter<T> adapter, Writer out, T value) throws IOException {
        JsonWriter writer = new JsonWriter(out);
        adapter.write(writer, value);
    }

    /**
     * Converts the given value to its JSON string representation.
     *
     * @param adapter the type adapter used for conversion
     * @param value the Java value to serialize
     * @param <T> the adapted type
     * @return the JSON string
     */
    static <T> String toJson(TypeAdapter<T> adapter, T value) {
        StringBuilder stringBuilder = new StringBuilder();
        try {
            toJson(adapter, Streams.writerForAppendable(stringBuilder), value);
        } catch (IOException e) {
            throw new JsonIOException(e);
        }
        return stringBuilder.toString();
    }

    /**
     * Converts the given value to a JSON tree.
     *
     * @param adapter the type adapter used for conversion
     * @param value the Java value to serialize
     * @param <T> the adapted type
     * @return the JSON tree representation
     */
    static <T> JsonElement toJsonTree(TypeAdapter<T> adapter, T value) {
        try {
            JsonTreeWriter jsonWriter = new JsonTreeWriter();
            adapter.write(jsonWriter, value);
            return jsonWriter.get();
        } catch (IOException e) {
            throw new JsonIOException(e);
        }
    }

    /**
     * Reads a JSON document from the specified reader and converts it to a Java value.
     *
     * @param adapter the type adapter used for conversion
     * @param in the source reader
     * @param <T> the adapted type
     * @return the converted Java value
     * @throws IOException if an I/O error occurs
     */
    static <T> T fromJson(TypeAdapter<T> adapter, Reader in) throws IOException {
        JsonReader reader = new JsonReader(in);
        return adapter.read(reader);
    }

    /**
     * Reads a JSON document from the specified string and converts it to a Java value.
     *
     * @param adapter the type adapter used for conversion
     * @param json the source JSON string
     * @param <T> the adapted type
     * @return the converted Java value
     * @throws IOException if an I/O error occurs
     */
    static <T> T fromJson(TypeAdapter<T> adapter, String json) throws IOException {
        return fromJson(adapter, new StringReader(json));
    }

    /**
     * Converts the specified JSON tree to a Java value.
     *
     * @param adapter the type adapter used for conversion
     * @param jsonTree the source JSON tree
     * @param <T> the adapted type
     * @return the converted Java value
     */
    static <T> T fromJsonTree(TypeAdapter<T> adapter, JsonElement jsonTree) {
        try {
            JsonReader jsonReader = new JsonTreeReader(jsonTree);
            return adapter.read(jsonReader);
        } catch (IOException e) {
            throw new JsonIOException(e);
        }
    }
}