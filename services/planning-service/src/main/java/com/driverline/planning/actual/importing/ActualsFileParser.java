package com.driverline.planning.actual.importing;

import java.io.InputStream;
import java.util.List;

/** Reads one file format into raw rows. Add a new format by adding another implementation (strategy pattern). */
public interface ActualsFileParser {

    boolean supports(String fileName);

    /**
     * @throws ImportValidationException if the file as a whole can't be used (unreadable, missing columns, too big)
     */
    List<RawActualRow> parse(InputStream content);
}
