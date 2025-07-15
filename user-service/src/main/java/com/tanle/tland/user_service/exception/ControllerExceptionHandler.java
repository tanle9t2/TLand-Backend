package com.tanle.tland.user_service.exception;


import org.apache.coyote.BadRequestException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ControllerExceptionHandler {

    @ExceptionHandler
    public ResponseEntity<ExceptionResponse> handleResourceNotFound(ResourceNotFoundExeption exception) {
        ExceptionResponse response = ExceptionResponse.builder()
                .type("/exception/" + exception.getClass().getSimpleName())
                .title("Resource not found")
                .detail(exception.getMessage())
                .timeStamp(System.currentTimeMillis())
                .status(HttpStatus.NOT_FOUND.value())
                .build();

        return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler
    public ResponseEntity<ExceptionResponse> handleResourceExisted(ResourceExistedException exception) {
        ExceptionResponse response = ExceptionResponse.builder()
                .type("/exception/" + exception.getClass().getSimpleName())
                .title("Resource exist")
                .detail(exception.getMessage())
                .timeStamp(System.currentTimeMillis())
                .status(HttpStatus.CONFLICT.value())
                .build();

        return new ResponseEntity<>(response, HttpStatus.CONFLICT);
    }

    @ExceptionHandler
    public ResponseEntity<ExceptionResponse> handleBadRequestException(BadRequestException exception) {
        ExceptionResponse response = ExceptionResponse.builder()
                .type("/exception/" + exception.getClass().getSimpleName())
                .title("Bad request")
                .detail(exception.getMessage())
                .timeStamp(System.currentTimeMillis())
                .status(HttpStatus.BAD_REQUEST.value())
                .build();

        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler
    public ResponseEntity<ExceptionResponse> handleUnauthorized(UnauthorizedException exception) {
        ExceptionResponse response = ExceptionResponse.builder()
                .type("/exception/" + exception.getClass().getSimpleName())
                .title("Bad request")
                .detail(exception.getMessage())
                .timeStamp(System.currentTimeMillis())
                .status(HttpStatus.UNAUTHORIZED.value())
                .build();

        return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
    }


    @ExceptionHandler
    public ResponseEntity<ExceptionResponse> handleDeleteException(ResourceDeleteException exception) {
        ExceptionResponse response = ExceptionResponse.builder()
                .type("/exception/" + exception.getClass().getSimpleName())
                .title("Delete resource fail")
                .detail(exception.getMessage())
                .timeStamp(System.currentTimeMillis())
                .status(HttpStatus.BAD_REQUEST.value())
                .build();

        return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler
    public ResponseEntity<ExceptionResponse> globalExceptionHandler(Exception ex) {
        ExceptionResponse message = ExceptionResponse.builder()
                .type("Excetion")
                .title("Internal server error")
                .detail(ex.getMessage())
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .timeStamp(System.currentTimeMillis())
                .build();

        return new ResponseEntity<>(message, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
