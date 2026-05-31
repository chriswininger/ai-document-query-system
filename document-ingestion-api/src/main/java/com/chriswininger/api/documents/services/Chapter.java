package com.chriswininger.api.documents.services;

import java.io.Serializable;

public record Chapter(String label, String content) implements Serializable {}
