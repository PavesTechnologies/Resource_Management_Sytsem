package com.cdc.service;

// Superseded by CdcConnectionManager (generic, unified).
// Kept as a thin extension so any future EOS-specific logic has a home.
public class EosConnectionManager extends CdcConnectionManager {

    public EosConnectionManager(String jdbcUrl,
                                 String username,
                                 String password) {
        super(jdbcUrl, username, password, "eos-resync-pool", true);
    }
}
