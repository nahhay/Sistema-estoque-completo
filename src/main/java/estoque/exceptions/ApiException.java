package estoque.exceptions;

public abstract class ApiException extends RuntimeException {
    private final int statusCode;

    protected ApiException(String message, int statusCode) {
        super(message);
        this.statusCode = statusCode;
    }

    public int getStatusCode() {
        return statusCode;
    }
}
