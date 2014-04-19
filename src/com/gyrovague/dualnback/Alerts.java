/**
 * Wrapper around AlertDialog.
 */
package com.gyrovague.dualnback;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Handler;

/**
 * @author asimihsan
 *
 */
public class Alerts {

    /**
     * Show a simple AlertDialog that, on click of the OK button, send an empty message back to a handler.
     * @param title Title of the alert dialog.
     * @param message Message of the alert dialog.
     * @param context Context within which the dialog belongs.
     * @param handler Handler to send an empty message back to when the OK button is clicked.
     * @param message_type Message type to send back to the handler when the OK button is clicked.
     */
    public static AlertDialog showAlert(String title, String message, Activity activity, Context context, Handler handler, int message_type) {
        // Create a builder
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(title);
        builder.setMessage(message);

        // Add buttons and listener
        PromptListener e1 = new PromptListener(handler, message_type);
        builder.setPositiveButton("OK", e1);

        // Create the dialog
        AlertDialog ad = builder.create();
        ad.setOwnerActivity(activity);

        // Show
        //ad.show();

        return ad;
    } // public static void showAlert(String title, String message, Context context)
} // public class Alerts

final class PromptListener
    implements android.content.DialogInterface.OnClickListener {
    private final Handler mHandler;
    private final int mMessageType;

    PromptListener(Handler handler, int message_type) {
        this.mHandler = handler;
        this.mMessageType = message_type;
    } // PromptListener(Handler handler, int message_type)

    public void onClick(DialogInterface v, int buttonID) {
        mHandler.sendEmptyMessage(mMessageType);
    } // public void onClick(DialogInterface v, int buttonID)

} // public class EmptyListener
