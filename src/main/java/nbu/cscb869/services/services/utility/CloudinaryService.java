package nbu.cscb869.services.services.utility;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import nbu.cscb869.common.exceptions.InvalidDoctorException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

/**
 * Service for handling image uploads and deletions using Cloudinary.
 */
@Service
public class CloudinaryService {

    private final Cloudinary cloudinary;

    /**
     * Constructs a new CloudinaryService with the specified Cloudinary configuration.
     *
     * @param cloudName the Cloudinary cloud name from application properties
     * @param apiKey    the Cloudinary API key from application properties
     * @param apiSecret the Cloudinary API secret from application properties
     */
    public CloudinaryService(
            @Value("${cloudinary.cloud-name}") String cloudName,
            @Value("${cloudinary.api-key}") String apiKey,
            @Value("${cloudinary.api-secret}") String apiSecret) {
        this.cloudinary = new Cloudinary(ObjectUtils.asMap(
                "cloud_name", cloudName,
                "api_key", apiKey,
                "api_secret", apiSecret
        ));
    }

    /**
     * Uploads an image to Cloudinary and returns the secure URL.
     *
     * @param file the image file to upload
     * @return the secure URL of the uploaded image
     * @throws InvalidDoctorException if the file is null, empty, or upload fails
     */
    public String uploadImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new InvalidDoctorException("Image file must not be null or empty");
        }
        try {
            Map<String, Object> uploadResult = cloudinary.uploader().upload(file.getBytes(), ObjectUtils.asMap(
                    "resource_type", "image",
                    "folder", "medical_record/doctors"
            ));
            return (String) uploadResult.get("secure_url");
        } catch (IOException e) {
            throw new InvalidDoctorException("Failed to upload image: " + e.getMessage());
        }
    }

    /**
     * Deletes an image from Cloudinary using its public ID.
     *
     * @param publicId the public ID of the image to delete
     * @throws InvalidDoctorException if deletion fails
     */
    public void deleteImage(String publicId) {
        if (publicId == null || publicId.isEmpty()) {
            return;
        }
        try {
            cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
        } catch (IOException e) {
            throw new InvalidDoctorException("Failed to delete image: " + e.getMessage());
        }
    }

    /**
     * Extracts the public ID from a Cloudinary URL.
     *
     * @param url the Cloudinary URL of the image
     * @return the public ID, or null if the URL is invalid
     */
    public String getPublicIdFromUrl(String url) {
        if (url == null || url.isEmpty()) {
            return null;
        }
        String[] parts = url.split("/");
        String fileName = parts[parts.length - 1];
        return "medical_record/doctors/" + fileName.substring(0, fileName.lastIndexOf("."));
    }
}