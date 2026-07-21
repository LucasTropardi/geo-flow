package br.com.lucastropardi.geoflow.processor.exception;

public class ProcessorException extends RuntimeException {

    public ProcessorException(String message) {
        super(message);
    }

    public ProcessorException(String message, Throwable cause) {
        super(message, cause);
    }
}
