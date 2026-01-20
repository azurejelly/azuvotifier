package com.vexsoftware.votifier.update;

import com.vexsoftware.votifier.update.impl.GitHubUpdateChecker;
import com.vexsoftware.votifier.util.CommonConstants;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class GitHubUpdateCheckerTest {

    @Test
    public void constructor_rejectInvalidRepositories() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> new GitHubUpdateChecker("a"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new GitHubUpdateChecker("a/a/a"));
    }

    @Test
    public void fetchLatest_returnsNonBlankVersion() {
        UpdateChecker instance = Assertions.assertDoesNotThrow(
                () -> new GitHubUpdateChecker(CommonConstants.GITHUB_REPOSITORY),
                "Failed to create a valid GitHub update checker instance"
        );

        String latest = Assertions.assertDoesNotThrow(
                instance::fetchLatest,
                "Fetching latest version should not throw"
        );

        Assertions.assertNotNull(latest, "Latest version should not be null");
        Assertions.assertFalse(latest.isBlank(), "Latest version should not be blank");
    }
}
