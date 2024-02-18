package com.inkneko.heimusic.util.music.model;

import lombok.Data;

import java.util.List;

@Data
public class ProbeResult {
    List<Stream> streams;
    Format format;
}