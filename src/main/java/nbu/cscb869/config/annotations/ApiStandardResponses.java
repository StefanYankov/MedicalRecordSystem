package nbu.cscb869.config.annotations;

import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import nbu.cscb869.config.OpenApiConstants;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A meta-annotation that bundles standard API error responses for reuse across controllers.
 * This ensures consistency in API documentation for common error scenarios.
 */
@Target({ ElementType.METHOD, ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@ApiResponses(value = {
        @ApiResponse(responseCode = "400", description = OpenApiConstants.BAD_REQUEST),
        @ApiResponse(responseCode = "401", description = OpenApiConstants.UNAUTHORIZED),
        @ApiResponse(responseCode = "403", description = OpenApiConstants.FORBIDDEN),
        @ApiResponse(responseCode = "404", description = OpenApiConstants.NOT_FOUND),
        @ApiResponse(responseCode = "500", description = OpenApiConstants.SERVER_ERROR)
})
public @interface ApiStandardResponses {
}
