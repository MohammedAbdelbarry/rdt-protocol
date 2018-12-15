package edu.csed.networks.rdt.packet.exceptions;

public class PacketCorruptedException extends Exception {
    private static final long serialVersionUID = -6848628003606117886L;

    public PacketCorruptedException() {
        super();
    }

    public PacketCorruptedException(String message) {
        super(message);
    }

    public PacketCorruptedException(String message, Throwable cause) {
        super(message, cause);
    }

    public PacketCorruptedException(Throwable cause) {
        super(cause);
    }

    public PacketCorruptedException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
