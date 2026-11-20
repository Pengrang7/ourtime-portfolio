package com.placeboard.common;

import com.placeboard.place.UnsupportedMapUrlException;
import com.placeboard.place.PlaceUnavailableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getDefaultMessage())
                .orElse("요청값을 확인해 주세요.");
        return ResponseEntity.badRequest().body(new ApiError("INVALID_REQUEST", message));
    }

    @ExceptionHandler({
            MethodArgumentTypeMismatchException.class,
            MissingServletRequestParameterException.class,
            HttpMessageNotReadableException.class
    })
    public ResponseEntity<ApiError> handleInvalidRequest(Exception exception) {
        log.debug("잘못된 요청값", exception);
        return ResponseEntity.badRequest()
                .body(new ApiError("INVALID_REQUEST", "요청값을 확인해 주세요."));
    }

    @ExceptionHandler(UnsupportedMapUrlException.class)
    public ResponseEntity<ApiError> handleUnsupportedMapUrl(UnsupportedMapUrlException exception) {
        return ResponseEntity.badRequest().body(new ApiError("UNSUPPORTED_MAP_URL", exception.getMessage()));
    }

    @ExceptionHandler(PlaceUnavailableException.class)
    public ResponseEntity<ApiError> handlePlaceUnavailable(PlaceUnavailableException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ApiError("PLACE_UNAVAILABLE", exception.getMessage()));
    }

    @ExceptionHandler(MapProviderConnectionException.class)
    public ResponseEntity<ApiError> handleProviderConnection(MapProviderConnectionException exception) {
        log.warn("지도 URL 확인 실패", exception);
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(new ApiError("MAP_PROVIDER_CONNECTION_FAILED", exception.getMessage()));
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiError> handleBusiness(BusinessException exception) {
        return ResponseEntity.status(exception.status())
                .body(new ApiError(exception.code(), exception.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpected(Exception exception) {
        log.error("처리되지 않은 서버 오류", exception);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiError("INTERNAL_SERVER_ERROR", "서버 처리 중 오류가 발생했습니다."));
    }
}
