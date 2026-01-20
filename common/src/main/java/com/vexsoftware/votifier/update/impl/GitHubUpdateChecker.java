package com.vexsoftware.votifier.update.impl;

import com.google.gson.*;
import com.vexsoftware.votifier.update.UpdateChecker;
import com.vexsoftware.votifier.update.exception.UpdateFetchException;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.*;

public class GitHubUpdateChecker implements UpdateChecker {

    private final String repository;

    /**
     * Constructs a new {@link GitHubUpdateChecker} instance.
     *
     * @param repository The repository of the project, formatted as {@code username/repository}.
     */
    public GitHubUpdateChecker(String repository) {
        if (!repository.contains("/")) {
            throw new IllegalArgumentException("repository must contain one / character");
        }

        if (repository.split("/").length != 2) {
            throw new IllegalArgumentException("Repository must be formatted as username/repository");
        }

        this.repository = repository;
    }

    @Override
    public String fetchLatest() throws UpdateFetchException {
        try {
            JsonArray array = fetchReleaseArray();

            for (JsonElement element : array) {
                JsonObject release = element.getAsJsonObject();

                if (release.get("prerelease").getAsBoolean()) {
                    continue;
                }

                return release.get("tag_name").getAsString();
            }

            throw new UpdateFetchException("GitHub returned no valid releases");
        } catch (IOException ex) {
            throw new UpdateFetchException("Failed to connect to GitHub", ex);
        } catch (JsonParseException | IllegalStateException ex) {
            throw new UpdateFetchException("Failed to read GitHub response", ex);
        }
    }

    private JsonArray fetchReleaseArray() throws IOException, JsonParseException {
        URL url = new URL("https://api.github.com/repos/" + repository + "/releases");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        connection.setRequestMethod("GET");
        connection.setRequestProperty("Accept", "application/vnd.github+json");
        connection.setRequestProperty("User-Agent", "azuvotifier"); // FIXME: should probably include the project version too
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(5000);

        int status = connection.getResponseCode();
        if (status != HttpURLConnection.HTTP_OK) {
            throw new IOException("Request failed with status code " + status);
        }

        try (InputStreamReader reader = new InputStreamReader(connection.getInputStream())) {
            return JsonParser.parseReader(reader).getAsJsonArray();
        }
    }
}
