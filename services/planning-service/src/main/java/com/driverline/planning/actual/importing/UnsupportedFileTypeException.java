package com.driverline.planning.actual.importing;

public class UnsupportedFileTypeException extends RuntimeException {

    public UnsupportedFileTypeException(String fileName) {
        super("'%s' is not a supported file type. Upload a .csv or .xlsx file.".formatted(fileName));
    }
}
