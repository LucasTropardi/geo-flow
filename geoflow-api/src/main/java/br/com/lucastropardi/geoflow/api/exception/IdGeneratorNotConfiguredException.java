package br.com.lucastropardi.geoflow.api.exception;

public class IdGeneratorNotConfiguredException extends RuntimeException {

    public IdGeneratorNotConfiguredException(String entityName) {
        super("No id_generator row configured for entity: " + entityName);
    }
}
