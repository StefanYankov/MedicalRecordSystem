package nbu.cscb869.config;

/**
 * A centralized place for holding web-related constant values.
 * This prevents the use of "magic numbers" and strings in controller annotations and business logic.
 */
public final class WebConstants {

    // Private constructor to prevent instantiation
    private WebConstants() {}

    /**
     * Default page number for paginated requests. Corresponds to the first page.
     */
    public static final String DEFAULT_PAGE_NUMBER = "0";

    /**
     * Default page size for paginated requests.
     */
    public static final String DEFAULT_PAGE_SIZE = "10";

    /**
     * Maximum page size allowed for any paginated request to prevent abuse.
     */
    public static final int MAX_PAGE_SIZE = 100;

    /**
     * Default field to sort by for paginated requests.
     */
    public static final String DEFAULT_SORT_BY = "id";

    /**
     * Default sort direction for paginated requests (ascending).
     */
    public static final String DEFAULT_SORT_ASC = "true";

}
