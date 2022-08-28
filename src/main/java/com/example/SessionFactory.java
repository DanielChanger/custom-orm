package com.example;

import lombok.RequiredArgsConstructor;

import javax.sql.DataSource;

@RequiredArgsConstructor
public class SessionFactory {
    private final DataSource dataSource;

    public Session createSession() {
        return new Session(dataSource);
    }
}
