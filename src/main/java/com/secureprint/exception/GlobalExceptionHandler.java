package com.secureprint.exception;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.Map;
import org.springframework.http.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<Map<String,Object>> validation(MethodArgumentNotValidException e, HttpServletRequest r) {
        return response(HttpStatus.BAD_REQUEST, ErrorCode.VALIDATION_ERROR, e.getBindingResult().getFieldErrors().stream().findFirst().map(x -> x.getDefaultMessage()).orElse("Invalid request"), r);
    }
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    ResponseEntity<Map<String,Object>> uploadTooLarge(MaxUploadSizeExceededException e, HttpServletRequest r) {
        return response(HttpStatus.BAD_REQUEST, ErrorCode.VALIDATION_ERROR, "Uploaded file exceeds the configured size limit", r);
    }
    @ExceptionHandler(ConflictException.class)
    ResponseEntity<Map<String,Object>> conflict(ConflictException e,HttpServletRequest r){ return response(HttpStatus.CONFLICT,ErrorCode.INVALID_STATE,e.getMessage(),r); }
    @ExceptionHandler(PolicyViolationException.class)
    ResponseEntity<Map<String,Object>> policy(PolicyViolationException e,HttpServletRequest r){
        HttpStatus status=e.getCode()==ErrorCode.VALIDATION_ERROR?HttpStatus.BAD_REQUEST:
                e.getCode()==ErrorCode.NOT_FOUND?HttpStatus.NOT_FOUND:
                (e.getCode()==ErrorCode.INVALID_STATE||e.getCode()==ErrorCode.PRINT_LIMIT_REACHED||e.getCode()==ErrorCode.EDIT_LIMIT_REACHED?HttpStatus.CONFLICT:HttpStatus.FORBIDDEN);
        return response(status,e.getCode(),e.getMessage(),r);
    }
    @ExceptionHandler({BadCredentialsException.class, UnauthorizedException.class})
    ResponseEntity<Map<String,Object>> unauthorized(Exception e,HttpServletRequest r){ return response(HttpStatus.UNAUTHORIZED,ErrorCode.UNAUTHENTICATED,"Authentication failed",r); }
    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<Map<String,Object>> denied(HttpServletRequest r){ return response(HttpStatus.FORBIDDEN,ErrorCode.FORBIDDEN,"Access denied",r); }
    @ExceptionHandler(Exception.class)
    ResponseEntity<Map<String,Object>> generic(Exception e,HttpServletRequest r){
        log.error("Unhandled request failure: {}", e.getClass().getName(), e);
        return response(HttpStatus.INTERNAL_SERVER_ERROR,ErrorCode.INVALID_STATE,"Request failed",r);
    }
    static ResponseEntity<Map<String,Object>> response(HttpStatus s,ErrorCode c,String m,HttpServletRequest r) {
        return ResponseEntity.status(s).body(Map.of("timestamp", Instant.now(),"status",s.value(),"code",c.name(),"message",m,"path",r.getRequestURI()));
    }
}
