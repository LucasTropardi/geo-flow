package br.com.lucastropardi.geoflow.api.exception;

public class JobNotFoundException extends RuntimeException {

    public JobNotFoundException(Long jobId) {
        super("Processing job not found: " + jobId);
    }
}
