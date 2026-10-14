package com.example.ArtConnect.exception;

import org.apache.catalina.connector.ClientAbortException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.async.AsyncRequestNotUsableException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import com.example.ArtConnect.dto.ApiMessage;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiMessage> handleApi(ApiException e) {
        return respond(e.getStatus(), e.getMessage());
    }

    /** Covers @Valid failures on both @RequestBody (MethodArgumentNotValidException) and form binding. */
    @ExceptionHandler(BindException.class)
    public ResponseEntity<ApiMessage> handleValidation(BindException e) {
        FieldError fieldError = e.getBindingResult().getFieldError();
        String message = fieldError == null
                ? "Some of the submitted values are invalid"
                : humanize(fieldError.getField()) + " " + fieldError.getDefaultMessage();
        return respond(HttpStatus.BAD_REQUEST, message);
    }

    @ExceptionHandler({ MissingServletRequestParameterException.class, MissingServletRequestPartException.class,
            MethodArgumentTypeMismatchException.class, HttpMessageNotReadableException.class })
    public ResponseEntity<ApiMessage> handleBadRequest(Exception e) {
        return respond(HttpStatus.BAD_REQUEST, "The request is missing a value or has a value in the wrong format");
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiMessage> handleTooLarge(MaxUploadSizeExceededException e) {
        return respond(HttpStatus.PAYLOAD_TOO_LARGE, "Images must be 10 MB or smaller");
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiMessage> handleConflict(DataIntegrityViolationException e) {
        log.info("Data integrity violation: {}", e.getMostSpecificCause().getMessage());
        return respond(HttpStatus.CONFLICT, "This conflicts with existing data. Please refresh and try again.");
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiMessage> handleNotFound(NoResourceFoundException e) {
        return respond(HttpStatus.NOT_FOUND, "Not found");
    }

    /** The browser went away mid-response (e.g. seeking in the background video); nothing to send. */
    @ExceptionHandler({ ClientAbortException.class, AsyncRequestNotUsableException.class })
    public void handleClientAbort() {
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiMessage> handleUnexpected(Exception e) {
        log.error("Unhandled error", e);
        return respond(HttpStatus.INTERNAL_SERVER_ERROR, "Something went wrong. Please try again.");
    }

    private static ResponseEntity<ApiMessage> respond(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(new ApiMessage(message));
    }

    /** "auctionDurationHours" -> "Auction duration hours" */
    private static String humanize(String field) {
        String spaced = field.replaceAll("([a-z])([A-Z])", "$1 $2").toLowerCase();
        return Character.toUpperCase(spaced.charAt(0)) + spaced.substring(1);
    }
}
