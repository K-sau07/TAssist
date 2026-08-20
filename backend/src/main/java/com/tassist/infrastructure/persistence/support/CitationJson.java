package com.tassist.infrastructure.persistence.support;

/** Persisted shape of one citation inside message.citations JSONB. Nullable snippet. */
public record CitationJson(String fileId, String chunkId, String displayLabel, String snippet) {}
