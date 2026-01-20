package com.vexsoftware.votifier.update;

import com.vexsoftware.votifier.update.exception.UpdateFetchException;

/**
 * Helps with checking whether the plugin is up to date or not.
 *
 * @author azurejelly
 */
public interface UpdateChecker {

    /**
     * Fetches the latest version of the project from an external source, such as
     * GitHub.
     *
     * <p>Implementations may perform network operations, so this method should be
     * called asynchronously.
     *
     * @return the latest available version string of the project.
     * @throws UpdateFetchException if something goes wrong while fetching the latest version.
     */
    String fetchLatest() throws UpdateFetchException;
}
