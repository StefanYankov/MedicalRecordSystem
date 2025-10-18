package nbu.cscb869.common.exceptions;

import nbu.cscb869.common.validation.ErrorMessages;

/**
 * Custom exception for handling image processing errors (e.g., upload, deletion failures).
 */
public class ImageProcessingException extends RuntimeException {

    /**
     * Constructs a new ImageProcessingException with a formatted error message.
     * @param messageKey the key from ErrorMessages to format
     * @param args arguments for message formatting
     */
    public ImageProcessingException(String messageKey, Object... args) {
        super(ErrorMessages.format(messageKey, args));
    }

    /**
     * Constructs a new ImageProcessingException with a cause and formatted error message.
     * @param messageKey the key from ErrorMessages to format
     * @param cause the underlying cause of the exception
     * @param args arguments for message formatting
     */
    public ImageProcessingException(String messageKey, Throwable cause, Object... args) {
        super(ErrorMessages.format(messageKey, args), cause);
    }
}