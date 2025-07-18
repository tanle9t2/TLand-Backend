package com.tanle.tland.asset_service.exception;

import com.tanle.tland.asset_service.response.MessageResponse;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import org.apache.coyote.BadRequestException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.nio.file.AccessDeniedException;

@RestControllerAdvice
public class ControllerExceptionHandler {
    @ExceptionHandler(StatusRuntimeException.class)
    public ResponseEntity<ExceptionResponse> handleGrpcException(StatusRuntimeException ex) {
        Status.Code code = ex.getStatus().getCode();
        String message = ex.getStatus().getDescription();
        HttpStatus httpStatus;

        switch (code) {
            case NOT_FOUND:
                httpStatus = HttpStatus.NOT_FOUND;
                break;
            case UNAVAILABLE:
                httpStatus = HttpStatus.SERVICE_UNAVAILABLE;
                break;
            case DEADLINE_EXCEEDED:
                httpStatus = HttpStatus.GATEWAY_TIMEOUT;
                break;
            case INVALID_ARGUMENT:
                httpStatus = HttpStatus.BAD_REQUEST;
                break;
            default:
                httpStatus = HttpStatus.INTERNAL_SERVER_ERROR;
        }

        ExceptionResponse response = ExceptionResponse.builder()
//                .type("/exception/" + exception.getClass().getSimpleName())
                .title("Resource not found")
                .detail(message)
                .timeStamp(System.currentTimeMillis())
                .status(httpStatus.value())
                .build();
        return new ResponseEntity<>(response, httpStatus);
    }

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


    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ExceptionResponse> handleUnauthorization(AccessDeniedException exception) {
        ExceptionResponse response = ExceptionResponse.builder()
                .type("/exception/" + exception.getClass().getSimpleName())
                .title("You do not have permission to access this resource")
                .detail(exception.getMessage())
                .timeStamp(System.currentTimeMillis())
                .status(HttpStatus.UNAUTHORIZED.value())
                .build();

        return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
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
