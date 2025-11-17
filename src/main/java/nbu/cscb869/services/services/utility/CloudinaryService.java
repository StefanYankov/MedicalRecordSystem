package nbu.cscb869.services.services.utility;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import nbu.cscb869.common.exceptions.ImageProcessingException;
import nbu.cscb869.common.validation.ErrorMessages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service for handling image uploads and deletions using Cloudinary.
 */
@Service
public class CloudinaryService {

    private static final Logger log = LoggerFactory.getLogger(CloudinaryService.class);
    private static final Pattern CLOUDINARY_URL_PATTERN = Pattern.compile(".*/upload/(?:v\\d+/)?(.*?)(\\.\\w+)$");


    private final Cloudinary cloudinary;

    public CloudinaryService(Cloudinary cloudinary) {
        this.cloudinary = cloudinary;
    }

    /**
     * Asynchronously uploads an image to Cloudinary and returns the secure URL.
     * @param file the image file to upload
     * @return a CompletableFuture containing the secure URL of the uploaded image
     * @throws ImageProcessingException if the file is null, empty, exceeds 10MB, or is not an image type
     */
    @Async
    @Retryable(value = IOException.class, maxAttempts = 3, backoff = @Backoff(delay = 1000))
    public CompletableFuture<String> uploadImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ImageProcessingException(ErrorMessages.IMAGE_FILE_NULL_OR_EMPTY);
        }
        if (file.getSize() > 10_485_760) { // 10MB limit
            throw new ImageProcessingException(ErrorMessages.IMAGE_FILE_SIZE_EXCEEDED, 10);
        }
        if (!file.getContentType().startsWith("image/")) {
            throw new ImageProcessingException(ErrorMessages.IMAGE_FILE_INVALID_TYPE);
        }
        try {
            log.info("Starting image upload for file: {}", file.getOriginalFilename());
            Map uploadResult = cloudinary.uploader().upload(file.getBytes(), ObjectUtils.asMap(
                    "resource_type", "image",
                    "folder", "medical_record/doctors"
            ));
            String secureUrl = (String) uploadResult.get("secure_url");
            log.info("Image uploaded successfully: {}", secureUrl);
            return CompletableFuture.completedFuture(secureUrl);
        } catch (IOException e) {
            log.error("Failed to upload image: {}", e.getMessage(), e);
            throw new ImageProcessingException(ErrorMessages.IMAGE_UPLOAD_FAILED, e.getMessage());
        }
    }

    /**
     * Deletes an image from Cloudinary using its public ID.
     * @param publicId the public ID of the image to delete
     * @throws ImageProcessingException if deletion fails
     */
    @Retryable(value = IOException.class, maxAttempts = 3, backoff = @Backoff(delay = 1000))
    public void deleteImage(String publicId) {
        if (publicId == null || publicId.isEmpty()) {
            return;
        }
        try {
            log.info("Deleting image with public ID: {}", publicId);
            cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
            log.info("Image deleted successfully");
        } catch (IOException e) {
            log.error("Failed to delete image: {}", e.getMessage(), e);
            throw new ImageProcessingException(ErrorMessages.IMAGE_DELETE_FAILED, publicId);
        }
    }

    /**
     * Extracts the public ID from a Cloudinary URL.
     * @param url the Cloudinary URL of the image
     * @return the public ID, or null if the URL is invalid
     */
    public String getPublicIdFromUrl(String url) {
        if (url == null || url.isEmpty()) {
            return null;
        }
        Matcher matcher = CLOUDINARY_URL_PATTERN.matcher(url);
        if (matcher.matches()) {
            return matcher.group(1);
        }
        return null;
    }
}
