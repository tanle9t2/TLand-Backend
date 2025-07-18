package com.tanle.tland.upload_service.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;

public class Helper {
    public static double getVideoDuration(Path videoPath) throws IOException {
        ProcessBuilder builder = new ProcessBuilder(
                "ffprobe",
                "-v", "error",
                "-show_entries", "format=duration",
                "-of", "default=noprint_wrappers=1:nokey=1",
                videoPath.toString()
        );

        builder.redirectErrorStream(true);
        Process process = builder.start();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line = reader.readLine();
            if (line != null) {
                return Double.parseDouble(line);
            }
        } catch (NumberFormatException e) {
            throw new IOException("Failed to parse duration", e);
        }

        throw new IOException("Could not read video duration");
    }

}
