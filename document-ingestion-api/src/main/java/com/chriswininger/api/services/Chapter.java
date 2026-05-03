package com.chriswininger.api.services;

import java.io.Serializable;

public record Chapter(String label, String content) implements Serializable {}
