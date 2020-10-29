package de.unibi.agbi.biodwh2.core.exceptions;

public class ExporterFormatException extends ExporterException {
    private static final long serialVersionUID = -3234100921721868307L;

    public ExporterFormatException() {
        super();
    }

    public ExporterFormatException(String message) {
        super(message);
    }

    public ExporterFormatException(String message, Throwable cause) {
        super(message, cause);
    }

    public ExporterFormatException(Throwable cause) {
        super(cause);
    }

    public ExporterFormatException(String message, Throwable cause, boolean enableSuppression,
                                   boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
