/*
 * If not stated otherwise in this file or this component's LICENSE file the
 * following copyright and licenses apply:
 *
 * Copyright 2023 Liberty Global Technology Services BV
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.lgi.appstorebundle.error.handler;

import com.lgi.appstorebundle.error.exception.ApplicationNotFoundException;
import com.lgi.appstorebundle.exception.RabbitMQException;
import com.lgi.appstorebundle.model.ErrorResponse;
import com.lgi.appstorebundle.model.ErrorResponseError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import static com.lgi.appstorebundle.filters.CorrelationIdFilter.X_REQUEST_ID_HEADER_NAME;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger LOG = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private static final String APP_NOT_FOUND_MESSAGE = "Application not found!";

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleAll(Exception ex, WebRequest request) {
        return handleGenericResponse(ex, HttpStatus.INTERNAL_SERVER_ERROR, request, ex.getMessage());
    }

    @ExceptionHandler(ApplicationNotFoundException.class)
    public ResponseEntity<Object> handleApplicationNotFound(Exception ex, WebRequest request) {
        return handleGenericResponse(ex, HttpStatus.NOT_FOUND, request, APP_NOT_FOUND_MESSAGE);
    }

    @ExceptionHandler(RabbitMQException.class)
    public ResponseEntity<Object> handleRabbitMQException(Exception ex, WebRequest request) {
        LOG.error("RabbitMQException message: {}", ex.getMessage());
        return handleGenericResponse(ex, HttpStatus.INTERNAL_SERVER_ERROR, request, ex.getMessage());
    }

    private ResponseEntity<Object> handleGenericResponse(Exception ex,
                                                         HttpStatus httpStatus,
                                                         WebRequest webRequest,
                                                         String message) {
        return handleGenericResponse(ex, (HttpHeaders) null, httpStatus, webRequest, message);
    }

    private ResponseEntity<Object> handleGenericResponse(Exception ex,
                                                         @Nullable HttpHeaders httpHeaders,
                                                         HttpStatus httpStatus,
                                                         WebRequest webRequest,
                                                         String message) {
        return handleGenericResponse(ex, createResponse(ex, httpStatus, webRequest, message), httpHeaders, httpStatus, webRequest);
    }
    private ResponseEntity<Object> handleGenericResponse(Exception ex, ErrorResponse errorResponse, @Nullable HttpHeaders httpHeaders, HttpStatus httpStatus,
                                                         WebRequest webRequest) {
        LOG.warn("Exception: ", ex);
        HttpHeaders headers = httpHeaders != null ? httpHeaders : new HttpHeaders();
        return handleExceptionInternal(ex, errorResponse, headers, httpStatus, webRequest);
    }

    private ErrorResponse createResponse(Exception ex, HttpStatus httpStatus, WebRequest webRequest, String message) {
        String correlationId = webRequest.getHeader(X_REQUEST_ID_HEADER_NAME);
        ErrorResponseError error = new ErrorResponseError();
        error.message(message)
                .details(ex.getMessage())
                .httpStatusCode(httpStatus.value())
                .correlationId(correlationId);

        ErrorResponse errorResponse = new ErrorResponse();
        errorResponse.setError(error);

        return errorResponse;
    }
}
