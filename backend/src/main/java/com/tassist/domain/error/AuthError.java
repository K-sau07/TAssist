package com.tassist.domain.error;

/** Authentication/authorization failures (spec §17.3). */
public sealed interface AuthError extends TassistError
        permits Unauthenticated, Forbidden, InvalidCredentials, EmailTaken {}
