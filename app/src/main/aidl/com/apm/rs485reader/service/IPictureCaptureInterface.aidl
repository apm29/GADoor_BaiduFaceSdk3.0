// IPictureCaptureInterface.aidl
package com.apm.rs485reader.service;

// Declare any non-default types here with import statements

interface IPictureCaptureInterface {
    /**
     * Demonstrates some basic types that you can use as parameters
     * and return values in AIDL.
     */
    void setPicture(in Bitmap data);
}
