package nbu.cscb869.services.services.utility.integrationtests;

import nbu.cscb869.config.CloudinaryConfig;
import nbu.cscb869.services.services.utility.CloudinaryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(
        classes = {
                CloudinaryService.class,
                CloudinaryConfig.class
        },
        webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
@Testcontainers
class CloudinaryServiceIntegrationTests {

    @Autowired
    private CloudinaryService cloudinaryService;

    @Test
    void UploadImage_ValidFile_ReturnsSecureUrl_HappyPath() throws IOException, ExecutionException, InterruptedException {
        ClassPathResource resource = new ClassPathResource("test-image.jpg");
        MockMultipartFile file = new MockMultipartFile(
                "image", "test-image.jpg", "image/jpeg", resource.getInputStream());
        String url = cloudinaryService.uploadImage(file).get();

        assertNotNull(url);
        assertTrue(url.contains("duntro9y8"));
        String publicId = cloudinaryService.getPublicIdFromUrl(url);
        cloudinaryService.deleteImage(publicId); // Cleanup
    }

    @Test
    void DeleteImage_ValidPublicId_Succeeds_HappyPath() throws IOException, ExecutionException, InterruptedException {
        ClassPathResource resource = new ClassPathResource("test-image.jpg");
        MockMultipartFile file = new MockMultipartFile(
                "image", "test-image.jpg", "image/jpeg", resource.getInputStream());
        String url = cloudinaryService.uploadImage(file).get();
        String publicId = cloudinaryService.getPublicIdFromUrl(url);

        cloudinaryService.deleteImage(publicId);
        assertDoesNotThrow(() -> cloudinaryService.deleteImage(publicId)); // Idempotent check
    }
}