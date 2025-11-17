package nbu.cscb869.services.services.utility.integrationtests;

import com.cloudinary.Cloudinary;
import com.cloudinary.Uploader;
import nbu.cscb869.services.services.utility.CloudinaryService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CloudinaryServiceIntegrationTests {

    @Mock
    private Cloudinary cloudinary;

    @Mock
    private Uploader uploader;

    @InjectMocks
    private CloudinaryService cloudinaryService;

    @Test
    void uploadImage_ValidFile_ReturnsSecureUrl_HappyPath() throws IOException, ExecutionException, InterruptedException {
        // ARRANGE
        when(cloudinary.uploader()).thenReturn(uploader); // Mock setup is now local to this test
        MockMultipartFile file = new MockMultipartFile("image", "test.jpg", "image/jpeg", "test data".getBytes());
        String expectedUrl = "http://res.cloudinary.com/test-cloud/image/upload/v12345/folder/public_id.jpg";
        Map<String, Object> mockResponse = Map.of("secure_url", expectedUrl);

        when(uploader.upload(any(byte[].class), anyMap())).thenReturn(mockResponse);

        // ACT
        String actualUrl = cloudinaryService.uploadImage(file).get();

        // ASSERT
        assertNotNull(actualUrl);
        assertEquals(expectedUrl, actualUrl);
        verify(uploader).upload(any(byte[].class), anyMap());
    }

    @Test
    void deleteImage_ValidPublicId_Succeeds_HappyPath() throws IOException {
        // ARRANGE
        when(cloudinary.uploader()).thenReturn(uploader); // Mock setup is now local to this test
        String publicId = "test-public-id";
        when(uploader.destroy(anyString(), anyMap())).thenReturn(Map.of("result", "ok"));

        // ACT & ASSERT
        assertDoesNotThrow(() -> cloudinaryService.deleteImage(publicId));
        verify(uploader).destroy(eq(publicId), anyMap());
    }

    @Test
    void getPublicIdFromUrl_ValidUrl_ReturnsCorrectId() {
        // ARRANGE
        String url = "http://res.cloudinary.com/duntro9y8/image/upload/v1699628746/folder/public_id.jpg";
        String expectedPublicId = "folder/public_id";

        // ACT
        String actualPublicId = cloudinaryService.getPublicIdFromUrl(url);

        // ASSERT
        assertEquals(expectedPublicId, actualPublicId);
    }

    @Test
    void getPublicIdFromUrl_UrlWithoutVersion_ReturnsCorrectId() {
        // ARRANGE
        String url = "http://res.cloudinary.com/duntro9y8/image/upload/folder/public_id.jpg";
        String expectedPublicId = "folder/public_id";

        // ACT
        String actualPublicId = cloudinaryService.getPublicIdFromUrl(url);

        // ASSERT
        assertEquals(expectedPublicId, actualPublicId);
    }
}
