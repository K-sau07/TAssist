package com.tassist.domain.vo;

/** Ingestion lifecycle of a file (spec §8 / §11.1). */
public enum FileStatus { UPLOADING, PARSING, EMBEDDING, READY, FAILED }
