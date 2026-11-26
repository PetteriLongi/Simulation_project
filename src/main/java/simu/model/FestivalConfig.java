package simu.model;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

// Reads settings from config/festival.properties and checks their values.
public final class FestivalConfig {
    // Stores all settings read from the file.
    private final Properties properties;

    private FestivalConfig(Properties properties) {
        this.properties = properties;
    }

    /** Loads the settings file from the project's config folder.
     * @throws IOException if the file cannot be read
     */
    public static FestivalConfig load() throws IOException {
        Path path = Path.of("config", "festival.properties");
        Properties properties = new Properties();

        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            properties.load(reader);
        }

        return new FestivalConfig(properties);
    }

    /**
     * Reads a decimal setting, such as a service-time value, and requires it to be greater than zero.
     * @param key the setting's name in the file
     * @return the setting as a decimal number
     */
    public double getPositiveDouble(String key) {
        String text = requiredValue(key);
        final double value;

        try {
            value = Double.parseDouble(text);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid number for " + key + ": " + text, e);
        }

        if (!Double.isFinite(value) || value <= 0) {
            throw new IllegalArgumentException(key + " must be a positive number");
        }

        return value;
    }

    /**
     * Reads a whole-number setting and requires it to be greater than zero.
     * @param key the setting's name in the file
     * @return the setting as a whole number
     */
    public int getPositiveInt(String key) {
        int value = parseInt(key);

        if (value <= 0) {
            throw new IllegalArgumentException(key + " must be greater than zero");
        }

        return value;
    }

    /**
     * Reads a whole-number setting that can be zero, but not negative.
     * @param key the setting's name in the file
     * @return the setting as a whole number
     */
    public int getNonNegativeInt(String key) {
        int value = parseInt(key);

        if (value < 0) {
            throw new IllegalArgumentException(key + " cannot be negative");
        }

        return value;
    }

    /**
     * Reads a whole-number setting that may be larger than an int, such as a random seed.
     * @param key the setting's name in the file
     * @return the setting as a long integer
     */
    public long getLong(String key) {
        String text = requiredValue(key);

        try {
            return Long.parseLong(text);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid integer for " + key + ": " + text, e);
        }
    }

    // Reads a setting as a whole number.
    private int parseInt(String key) {
        String text = requiredValue(key);

        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid integer for " + key + ": " + text, e);
        }
    }

    // Finds a setting and reports an error if it is missing or blank.
    private String requiredValue(String key) {
        String value = properties.getProperty(key);

        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Missing required configuration property: " + key);
        }

        return value.trim();
    }
}