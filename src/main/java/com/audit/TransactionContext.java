package com.audit;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TransactionContext {
    
    private static final ThreadLocal<String> transactionId = new ThreadLocal<>();
    private static final ThreadLocal<String> requestId = new ThreadLocal<>();
    private static final ThreadLocal<String> source = new ThreadLocal<>();
    
    public static void setTransactionId(String id) {
        transactionId.set(id);
    }
    
    public static String getTransactionId() {
        return transactionId.get();
    }
    
    public static void setRequestId(String id) {
        requestId.set(id);
    }
    
    public static String getRequestId() {
        return requestId.get();
    }
    
    public static void setSource(String src) {
        source.set(src);
    }
    
    public static String getSource() {
        return source.get();
    }
    
    public static void clear() {
        transactionId.remove();
        requestId.remove();
        source.remove();
    }
    
    public static void generateAndSetTransactionId() {
        setTransactionId(java.util.UUID.randomUUID().toString());
    }
    
    public static void generateAndSetRequestId() {
        setRequestId(java.util.UUID.randomUUID().toString());
    }
    
    public static String getOrCreateTransactionId() {
        String existingId = getTransactionId();
        if (existingId == null) {
            generateAndSetTransactionId();
            existingId = getTransactionId();
            log.debug("Generated new transaction ID: {}", existingId);
        }
        return existingId;
    }
    
    public static String getOrCreateRequestId() {
        String existingId = getRequestId();
        if (existingId == null) {
            generateAndSetRequestId();
            existingId = getRequestId();
        }
        return existingId;
    }
    
    public static void clearIfPresent() {
        if (getTransactionId() != null || getRequestId() != null || getSource() != null) {
            clear();
        }
    }
    
    public static boolean hasTransactionId() {
        return getTransactionId() != null;
    }
}
