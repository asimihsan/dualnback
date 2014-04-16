/**
 *
 */
package com.gyrovague.dualnback.tests;

import android.app.AlertDialog;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.test.ActivityInstrumentationTestCase2;
import android.test.TouchUtils;
import android.test.suitebuilder.annotation.MediumTest;
import android.util.Log;
import android.widget.Button;

import com.gyrovague.dualnback.DrawView;
import com.gyrovague.dualnback.GameActivity;
import com.gyrovague.dualnback.R;

/**
 * @author asimihsan
 *
 */
@MediumTest
public class GameActivityTests extends ActivityInstrumentationTestCase2<GameActivity> {
  private DrawView mDrawView;
  private Context mContext;
  private Button mBtnAudioOnly;
  private AlertDialog mAlertDialog;

  public GameActivityTests() {
    super("com.gyrovague.dualnback", GameActivity.class);
    Log.d("test", "constructor");
  } // public GameActivityTests()

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    Log.d("test", "setUp() entry");
    mContext = getInstrumentation().getTargetContext();
    GameActivity a = getActivity();
    mDrawView = (DrawView)a.findViewById(R.id.draw_view);
    mBtnAudioOnly = (Button)a.findViewById(R.id.btn_audio_only);
    mAlertDialog = (AlertDialog)a.getmAlertDialog();
  } // protected void setUp() throws Exception

  public void testPreconditions() {
    Log.d("test", "testPreconditions() entry");
    assertNotNull(mDrawView);
    assertNotNull(mAlertDialog);
  }

  public void testBasicStart() throws InterruptedException {
    Log.d("test", "testBasicStart() entry");
    GameActivity a = getActivity();

    // --------------------------------------------------------------------------------------------------
    //  Alert dialog notifying the user about the interval pops up.  Check it's there.  Click OK.
    //  Check it goes away.
    // --------------------------------------------------------------------------------------------------
    handleAlertDialogOk(false);
    // --------------------------------------------------------------------------------------------------

    // --------------------------------------------------------------------------------------------------
    //  Rotate the screen five times.  This'll trigger onCreate() five times and hopefully force the
    //  "Exceed maximum number of OpenCore instances" error, cause the SoundManager to be unable
    //  to get MediaPlayer instances, and hit a null exception.
    // --------------------------------------------------------------------------------------------------
    for(int i = 0; i < 5; i++) {
      switch(a.getRequestedOrientation()) {
      case ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE:
        rotateScreen(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        break;
      case ActivityInfo.SCREEN_ORIENTATION_PORTRAIT:
        rotateScreen(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        break;
      default:
        fail();
      }
      Thread.sleep(1000);
    }
    // --------------------------------------------------------------------------------------------------

    handleAlertDialogOk(true);
  }

  public void test3() {
    Log.d("test", "testPreconditions() entry");
    assertNotNull(mDrawView);
    assertNotNull(mAlertDialog);
  }

  private void handleAlertDialogOk(boolean click) throws InterruptedException {
    GameActivity a;
    AlertDialog alert_dialog = null;
    for(int i = 0; (i < 10) && (alert_dialog == null); i++, Thread.sleep(1000)) {
      Log.d("test", "waiting for alert dialog to appear...");
      a = getActivity();
      alert_dialog = a.getmAlertDialog();
    }
    a = getActivity();
    assertNotNull(alert_dialog);
    if (click) {
      Button alert_ok = alert_dialog.getButton(AlertDialog.BUTTON_POSITIVE);
      assertNotNull(alert_ok);
      TouchUtils.clickView(this, alert_ok);
      alert_dialog = (AlertDialog)a.getmAlertDialog();
      assertNull(alert_dialog);
    }
  }

  private void rotateScreen(int orientation) throws InterruptedException {
    GameActivity a = getActivity();
    Log.d("test", "rotating landscape.");
    a.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
    Log.d("test", "sleeping.");
    Thread.sleep(3000);
    Log.d("test", "waking.");
  }
}
