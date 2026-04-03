package com.google.gson;

/**
 * Utility class for converting Java values to non-null {@link JsonElement} instances.
 *
 * <p>{@code null} values are converted to {@link JsonNull#INSTANCE}. Primitive-compatible values
 * are wrapped in {@link JsonPrimitive}.
 */
public final class JsonElementConversion {

    private JsonElementConversion() {}

    /**
     * Converts the given string value to a {@link JsonElement}.
     *
     * @param value the value to convert
     * @return {@link JsonNull#INSTANCE} if the value is null, otherwise a {@link JsonPrimitive}
     */
    public static JsonElement toJsonElement(String value) {
        return value == null ? JsonNull.INSTANCE : new JsonPrimitive(value);
    }

    /**
     * Converts the given number value to a {@link JsonElement}.
     *
     * @param value the value to convert
     * @return {@link JsonNull#INSTANCE} if the value is null, otherwise a {@link JsonPrimitive}
     */
    public static JsonElement toJsonElement(Number value) {
        return value == null ? JsonNull.INSTANCE : new JsonPrimitive(value);
    }

    /**
     * Converts the given boolean value to a {@link JsonElement}.
     *
     * @param value the value to convert
     * @return {@link JsonNull#INSTANCE} if the value is null, otherwise a {@link JsonPrimitive}
     */
    public static JsonElement toJsonElement(Boolean value) {
        return value == null ? JsonNull.INSTANCE : new JsonPrimitive(value);
    }

    /**
     * Converts the given character value to a {@link JsonElement}.
     *
     * @param value the value to convert
     * @return {@link JsonNull#INSTANCE} if the value is null, otherwise a {@link JsonPrimitive}
     */
    public static JsonElement toJsonElement(Character value) {
        return value == null ? JsonNull.INSTANCE : new JsonPrimitive(value);
    }

    /**
     * Returns a non-null {@link JsonElement}.
     *
     * @param element the element to normalize
     * @return {@link JsonNull#INSTANCE} if the element is null, otherwise the element itself
     */
    public static JsonElement nonNull(JsonElement element) {
        return element == null ? JsonNull.INSTANCE : element;
    }
}