package nbu.cscb869.config;

import com.cloudinary.Cloudinary;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.annotation.EnableRetry;

/**
 * Configuration class for setting up Cloudinary bean by parsing the CLOUDINARY_URL environment variable.
 */
@Configuration
@EnableRetry
public class CloudinaryConfig {

    /**
     * Creates a Cloudinary bean from the CLOUDINARY_URL environment variable.
     * This is the standard and recommended way to configure the Cloudinary SDK.
     * @return a configured Cloudinary instance
     */
    @Bean
    public Cloudinary cloudinary() {
        // The Cloudinary SDK will automatically find and parse the CLOUDINARY_URL environment variable.
        return new Cloudinary();
    }
}
