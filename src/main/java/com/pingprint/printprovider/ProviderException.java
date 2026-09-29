package com.pingprint.printprovider;

public class ProviderException extends RuntimeException {
    private final boolean ambiguous;
    public ProviderException(String message, boolean ambiguous, Throwable cause) { super(message, cause); this.ambiguous = ambiguous; }
    public ProviderException(String message, boolean ambiguous) { super(message); this.ambiguous = ambiguous; }
    public boolean isAmbiguous() { return ambiguous; }
}
