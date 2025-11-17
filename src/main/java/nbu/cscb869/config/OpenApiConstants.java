package nbu.cscb869.config;

/**
 * A utility class to hold constant string values for OpenAPI documentation.
 * This centralizes messages for consistency and easy updates.
 */
public final class OpenApiConstants {

    // --- General Responses ---
    public static final String SUCCESS_OK = "Request was successful.";
    public static final String SUCCESS_CREATED = "Resource was successfully created.";
    public static final String SUCCESS_NO_CONTENT = "Resource was successfully deleted or updated with no content to return.";

    public static final String BAD_REQUEST = "Bad Request - The request was malformed or contained invalid data.";
    public static final String UNAUTHORIZED = "Unauthorized - You must be authenticated to access this resource.";
    public static final String FORBIDDEN = "Forbidden - You do not have the necessary permissions to access this resource.";
    public static final String NOT_FOUND = "Not Found - The requested resource could not be found.";
    public static final String CONFLICT = "Conflict - The request could not be completed due to a conflict with the current state of the resource.";
    public static final String SERVER_ERROR = "Internal Server Error - An unexpected error occurred on the server.";

    private OpenApiConstants() {
        // Private constructor to prevent instantiation
    }
}
