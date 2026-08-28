package estoque.exceptions;

public class InternalErrorException extends ApiException {
    public InternalErrorException(String message) {
        super(message, 500);
    }
}
