package br.com.lucastropardi.geoflow.api.exception;

public class JobEventPublishException extends RuntimeException {

    public JobEventPublishException(String message, Throwable cause) {
        super(message, cause);
    }
}
