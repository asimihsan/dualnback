/**
 *
 */
package com.gyrovague.dualnback.exception;

import android.util.Log;

/**
 * @author asimihsan
 *
 */
public class InconsistentDataException extends Exception {
    public InconsistentDataException(String tag, String msg) {
        super(msg);
        Log.e(tag, msg);
    } // public InconsistentDataException(String msg)

    public InconsistentDataException(String tag, String msg, Throwable t) {
        super(msg, t);
        Log.e(tag, msg, t);
    } // public InconsistentDataException(String msg, Throwable t)

} // public class InconsistentDataException extends Exception
