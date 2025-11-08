package nbu.cscb869.services.services.utility.unittests;

import com.cloudinary.Cloudinary;
import com.cloudinary.Uploader;
import nbu.cscb869.common.exceptions.ImageProcessingException;
import nbu.cscb869.common.validation.ErrorMessages;
import nbu.cscb869.services.services.utility.CloudinaryService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CloudinaryServiceUnitTests {

    @Mock
    private Cloudinary cloudinary;

    @InjectMocks
    private CloudinaryService cloudinaryService;

    @Test
    void UploadImage_ValidFile_ReturnsSecureUrl_HappyPath() throws Exception {
        MockMultipartFile file = new MockMultipartFile("image", "test.jpg", "image/jpeg", "content".getBytes());
        Map<String, String> uploadResult = Map.of("secure_url", "https://test.cloud.com/image.jpg");
        Uploader mockUploader = mock(Uploader.class);
        when(cloudinary.uploader()).thenReturn(mockUploader);
        when(mockUploader.upload(any(byte[].class), anyMap())).thenReturn(uploadResult);

        CompletableFuture<String> resultFuture = cloudinaryService.uploadImage(file);
        String result = resultFuture.get();

        assertEquals("https://test.cloud.com/image.jpg", result);
        verify(mockUploader).upload(any(byte[].class), anyMap());
    }

    @Test
    void UploadImage_NullFile_ThrowsImageProcessingException_ErrorCase() {
        assertThrows(ImageProcessingException.class, () -> cloudinaryService.uploadImage(null),
                ErrorMessages.IMAGE_FILE_NULL_OR_EMPTY);
        verifyNoInteractions(cloudinary);
    }

    @Test
    void UploadImage_EmptyFile_ThrowsImageProcessingException_ErrorCase() {
        MockMultipartFile file = new MockMultipartFile("image", "", "image/jpeg", new byte[0]);
        assertThrows(ImageProcessingException.class, () -> cloudinaryService.uploadImage(file),
                ErrorMessages.IMAGE_FILE_NULL_OR_EMPTY);
        verifyNoInteractions(cloudinary);
    }

    @Test
    void UploadImage_OversizedFile_ThrowsImageProcessingException_ErrorCase() {
        MockMultipartFile file = new MockMultipartFile("image", "test.jpg", "image/jpeg",
                new byte[10_485_761]); // Slightly over 10MB
        assertThrows(ImageProcessingException.class, () -> cloudinaryService.uploadImage(file),
                ErrorMessages.format(ErrorMessages.IMAGE_FILE_SIZE_EXCEEDED, 10));
        verifyNoInteractions(cloudinary);
    }

    @Test
    void UploadImage_InvalidFileType_ThrowsImageProcessingException_ErrorCase() {
        MockMultipartFile file = new MockMultipartFile("file", "test.txt", "text/plain", "content".getBytes());
        assertThrows(ImageProcessingException.class, () -> cloudinaryService.uploadImage(file),
                ErrorMessages.IMAGE_FILE_INVALID_TYPE);
        verifyNoInteractions(cloudinary);
    }

    @Test
    void UploadImage_IoException_ThrowsImageProcessingException_ErrorCase() throws IOException {
        MockMultipartFile file = new MockMultipartFile("image", "test.jpg", "image/jpeg", "content".getBytes());
        Uploader mockUploader = mock(Uploader.class);
        when(cloudinary.uploader()).thenReturn(mockUploader);
        when(mockUploader.upload(any(byte[].class), anyMap())).thenThrow(new IOException("Network error"));

        assertThrows(ImageProcessingException.class, () -> cloudinaryService.uploadImage(file),
                ErrorMessages.format(ErrorMessages.IMAGE_UPLOAD_FAILED, "Network error"));
        verify(mockUploader).upload(any(byte[].class), anyMap());
    }

    @Test
    void DeleteImage_ValidPublicId_Succeeds_HappyPath() throws IOException {
        String publicId = "test-image";
        Uploader mockUploader = mock(Uploader.class);
        when(cloudinary.uploader()).thenReturn(mockUploader);
        when(mockUploader.destroy(eq(publicId), anyMap())).thenReturn(null);

        cloudinaryService.deleteImage(publicId);

        verify(mockUploader).destroy(eq(publicId), anyMap());
    }

    @Test
    void DeleteImage_NullPublicId_NoAction_ErrorCase() {
        cloudinaryService.deleteImage(null);

        verifyNoInteractions(cloudinary);
    }

    @Test
    void DeleteImage_EmptyPublicId_NoAction_ErrorCase() {
        cloudinaryService.deleteImage("");

        verifyNoInteractions(cloudinary);
    }

    @Test
    void DeleteImage_IoException_ThrowsImageProcessingException_ErrorCase() throws IOException {
        String publicId = "test-image";
        Uploader mockUploader = mock(Uploader.class);
        when(cloudinary.uploader()).thenReturn(mockUploader);
        doThrow(new IOException("Network error")).when(mockUploader).destroy(eq(publicId), anyMap());

        assertThrows(ImageProcessingException.class, () -> cloudinaryService.deleteImage(publicId),
                ErrorMessages.format(ErrorMessages.IMAGE_DELETE_FAILED, publicId));
        verify(mockUploader).destroy(eq(publicId), anyMap());
    }

    @Test
    void GetPublicIdFromUrl_ValidUrl_ReturnsPublicId_HappyPath() {
        String url = "https://res.cloudinary.com/test/image.jpg";
        String expectedPublicId = "medical_record/doctors/image";

        String result = cloudinaryService.getPublicIdFromUrl(url);

        assertEquals(expectedPublicId, result);
    }

    @Test
    void GetPublicIdFromUrl_NullUrl_ReturnsNull_EdgeCase() {
        String result = cloudinaryService.getPublicIdFromUrl(null);

        assertNull(result);
    }

    @Test
    void GetPublicIdFromUrl_EmptyUrl_ReturnsNull_EdgeCase() {
        String result = cloudinaryService.getPublicIdFromUrl("");

        assertNull(result);
    }

    @Test
    void GetPublicIdFromUrl_InvalidUrlFormat_ReturnsNull_EdgeCase() {
        String result = cloudinaryService.getPublicIdFromUrl("invalid-url");

        assertNull(result);
    }
}
