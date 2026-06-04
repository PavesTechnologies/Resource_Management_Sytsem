package com.audit;

public final class AuditConstants {
    
    private AuditConstants() {}
    
    public static final class Modules {
        public static final String CLIENT = "CLIENT";
        public static final String DEMAND = "DEMAND";
        public static final String ALLOCATION = "ALLOCATION";
        public static final String PROJECT = "PROJECT";
        public static final String RESOURCE = "RESOURCE";
        public static final String SKILL = "SKILL";
        public static final String BENCH = "BENCH";
        public static final String LEDGER = "LEDGER";
    }
    
    public static final class Actions {
        public static final String CREATE = "CREATE";
        public static final String UPDATE = "UPDATE";
        public static final String DELETE = "DELETE";
        public static final String STATUS_CHANGE = "STATUS_CHANGE";
        public static final String ROLE_OFF = "ROLE_OFF";
        public static final String BULK_OPERATION = "BULK_OPERATION";
    }
}
