package com.gyrovague.dualnback;

import java.text.MessageFormat;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.os.Vibrator;
import android.text.format.Time;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import ec.util.MersenneTwister;
import android.graphics.Color;

public class GameActivity extends Activity {
    private static String TAG = "GameActivity";
    private SoundManager mSoundManager;
    private DrawView mDrawView;
    private Button mButtonAudio;
    private Button mButtonVisual;
    private Button[] mAllButtons;
    private Resources mResources;
    private Activity me = this;
    private Context mContext = this;
    private GameManager mGameManager;
    private MersenneTwister mRNG;
    boolean mIsSoundManagerInitialized;
    boolean mIsGameManagerInitialized;
    ProgressDialog mDialog;
    private Trial mCurrentTrial;
    private Vibrator mVibrator;
    private PowerManager mPowerManager;
    private PowerManager.WakeLock mWakeLock;
    private static long[] VIBRATE_PATTERN = new long[] { 250, 250, };
    public static final String PREFS_NAME = "prefs";

    /** Button feedback types */
    public static final int FEEDBACK_TYPE_NONE = 0;
    public static final int FEEDBACK_TYPE_GOOD = 1;
    public static final int FEEDBACK_TYPE_BAD  = 2;
    public static final int FEEDBACK_TYPE_MISS = 3;
    
    /**
     * Message types handled by and sent to the main UI thread.
     */
    public static final int MSG_TYPE_NEW_TRIAL         = 1;
    public static final int MSG_TYPE_GUESS_AUDIO       = 2;
    public static final int MSG_TYPE_GUESS_VISUAL      = 3;
    public static final int MSG_TYPE_GUESS_VALIDATE    = 4;
    public static final int MSG_TYPE_HALT_VISUAL       = 5;
    public static final int MSG_TYPE_HALT_AUDIO        = 6;
    public static final int MSG_TYPE_DRAWING_DONE      = 7;
    public static final int MSG_TYPE_CREATE            = 8;
    public static final int MSG_TYPE_INITIALIZE        = 9;
    public static final int MSG_TYPE_INITIALIZE_DONE   = 10;
    public static final int MSG_TYPE_NEW_BLOCK         = 11;
    public static final int MSG_TYPE_END_OF_DAY        = 12;
    public static final int MSG_TYPE_CLEAR_FEEDBACK    = 13;
    public static final int[] ALL_MESSAGE_TYPES = new int[] {MSG_TYPE_NEW_TRIAL, MSG_TYPE_GUESS_AUDIO, MSG_TYPE_GUESS_VISUAL, MSG_TYPE_GUESS_VALIDATE,
            MSG_TYPE_HALT_VISUAL, MSG_TYPE_HALT_AUDIO, MSG_TYPE_DRAWING_DONE, MSG_TYPE_CREATE,
            MSG_TYPE_INITIALIZE, MSG_TYPE_INITIALIZE_DONE, MSG_TYPE_NEW_BLOCK, MSG_TYPE_END_OF_DAY, MSG_TYPE_CLEAR_FEEDBACK
                                                            };

    /**
     * States that the activity can be in.
     */
    private static final int ACT_STATE_INITIALIZING         = 1;
    private static final int ACT_STATE_LOADING              = 2;
    private static final int ACT_STATE_LOADED               = 3;
    private static final int ACT_STATE_WAITING_FOR_GUESS    = 4;
    private static final int ACT_STATE_RECEIVED_GUESS       = 5;
    private static final int ACT_STATE_REDRAWING            = 6;
    private static final int ACT_STATE_REDRAWN              = 7;
    private static final int ACT_STATE_LOST_FOCUS           = 8;
    private static final int ACT_STATE_CREATING             = 9;
    private static final int ACT_STATE_WAIT_FOR_ALERT_INTERVAL         = 10;
    private static final int ACT_STATE_CREATED              = 11;
    private static final int ACT_STATE_STOP                 = 12;
    private int mActivityState;

    private static final int DIALOG_ALERT_INTERVAL          = 1;
    private AlertDialog mAlertDialog;

    Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            String SUB_TAG = "Handler::handleMessage()";
            boolean result;

            switch (msg.what) {
            case MSG_TYPE_CREATE:
                Log.d(TAG+SUB_TAG, "MSG_TYPE_CREATE");
                Time time = new Time();
                time.setToNow();
                long millis = time.toMillis(false);
                mRNG = new MersenneTwister(new int[] {(int) (millis & 0xFFFFFFFF), (int) (millis >> 32)});
                mWakeLock.acquire();

                initializeLayout();

                // --------------------------------------------------------------------------------------
                //  Create SoundManager and GameManager instances in background threads.
                // --------------------------------------------------------------------------------------
                Thread createSoundManager = new Thread(new Runnable() {
                    public void run() {
                        mSoundManager = new SoundManager(mDrawView.getContext());
                        if (mGameManager != null) {
                            mActivityState = ACT_STATE_CREATED;
                            mHandler.sendEmptyMessage(MSG_TYPE_INITIALIZE);
                        }
                    } // public void run()
                }); // Thread initSoundManager = new Thread(new Runnable()

                Thread createGameManager = new Thread(new Runnable() {
                    public void run() {
                        mGameManager = new GameManager(mRNG);
                        mGameManager.setHandlerUI(mHandler);
                        
                        // restore previous level
                        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
                        int nInterval = settings.getInt("nInterval", GameManager.NINTERVAL_MIN);
                        mGameManager.setnInterval(nInterval);
                        
                        if (mSoundManager != null) {
                            mActivityState = ACT_STATE_CREATED;
                            mHandler.sendEmptyMessage(MSG_TYPE_INITIALIZE);
                        }
                    } // public void run()
                }); // Thread initSoundManager = new Thread(new Runnable()

                createSoundManager.run();
                createGameManager.run();
                // --------------------------------------------------------------------------------------

                break;

            case MSG_TYPE_INITIALIZE:
                Log.d(TAG+SUB_TAG, "MSG_TYPE_INITIALIZE");
                mActivityState = ACT_STATE_INITIALIZING;
                mIsSoundManagerInitialized = false;
                mIsGameManagerInitialized = false;
                //setAllButtonsEnabledState(false);
                Thread initSoundManager = new Thread(new Runnable() {
                    public void run() {
                        boolean result = mSoundManager.initialize();
                        if (!result) {
                            try {
                                Thread.sleep(500);
                            } catch (InterruptedException e) {
                                // TODO Auto-generated catch block
                                e.printStackTrace();
                            }
                            mSoundManager.initialize();
                        }
                        mIsSoundManagerInitialized = true;
                        if (mIsGameManagerInitialized) {
                            mHandler.sendEmptyMessage(MSG_TYPE_INITIALIZE_DONE);
                        } // if (isGameManagerInitialized)
                    } // public void run()
                }); // Thread initSoundManager = new Thread(new Runnable()

                Thread initGameManager = new Thread(new Runnable() {
                    public void run() {
                        mGameManager.prepareCurrentBlock();
                        mIsGameManagerInitialized = true;
                        if (mIsSoundManagerInitialized) {
                            mHandler.sendEmptyMessage(MSG_TYPE_INITIALIZE_DONE);
                        } // if (mIsSoundManagerInitialized)
                    } // public void run()
                }); // Thread initGameManager = new Thread(new Runnable()

                initSoundManager.run();
                initGameManager.run();
                break;

            case MSG_TYPE_INITIALIZE_DONE:
                Log.d(TAG+SUB_TAG, "MSG_TYPE_INITIALIZE_DONE");
                mActivityState = ACT_STATE_WAIT_FOR_ALERT_INTERVAL;
                String message = "";
                if (mGameManager.getCurrentBlock() > 0) {
                    message += MessageFormat.format(mResources.getString(R.string.alert_rate_message), mGameManager.getRate());
                    message += " ";
                }
                message += MessageFormat.format(mResources.getString(R.string.alert_interval_back_message), mGameManager.getnInterval());
                AlertDialog alert = Alerts.showAlert(mResources.getString(R.string.alert_new_session),
                                                     message,
                                                     me,
                                                     mContext,
                                                     mHandler,
                                                     MSG_TYPE_NEW_TRIAL);
                setmAlertDialog(alert);
                mAlertDialog.show();
                getmAlertDialog();
                break;

            case MSG_TYPE_NEW_TRIAL:
                Log.d(TAG+SUB_TAG, "MSG_TYPE_NEW_TRIAL. mActivityState: " + mActivityState);
                mAlertDialog = null;
                mHandler.removeMessages(MSG_TYPE_GUESS_VALIDATE);
                //setAllButtonsEnabledState(true);

                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    Log.e("Handler::handleMessage()", "MSG_TYPE_NEW_TRIAL: Sleep interrupted", e);
                }

                if (mGameManager.isCurrentBlockFinished()) {
                    Log.d(TAG+SUB_TAG, "current block finished");
                    if (mGameManager.isCurrentDayFinished()) {
                        Log.d(TAG+SUB_TAG, "current day finished");
                        mHandler.sendEmptyMessage(MSG_TYPE_END_OF_DAY);
                    } else {
                        Log.d(TAG+SUB_TAG, "current day not finished");
                        mGameManager.advanceBlock();
                        mHandler.sendEmptyMessage(MSG_TYPE_INITIALIZE);
                    } // if (mGameManager.isCurrentDayFinished())
                } else {
                    Log.d(TAG+SUB_TAG, "current block not finished");
                    mCurrentTrial = mGameManager.getCurrentTrial();
                    mDrawView.setmSquaresDrawnFromIndex(mCurrentTrial.visual);
                    mActivityState = ACT_STATE_REDRAWING;
                    mDrawView.setmHandlerUI(mHandler);
                    mDrawView.invalidate();
                } // if (mGameManager.isCurrentBlockFinished())

                break;

            case MSG_TYPE_DRAWING_DONE:
                Log.d("Handler::handleMessage()", "MSG_TYPE_DRAWING_DONE. mActivityState: " + mActivityState);
                if (mActivityState == ACT_STATE_REDRAWING) {
                    mActivityState = ACT_STATE_REDRAWN;
                    mSoundManager.playSound(mCurrentTrial.audio);
                    mHandler.sendEmptyMessageDelayed(MSG_TYPE_HALT_VISUAL, 500);
                    mHandler.sendEmptyMessageDelayed(MSG_TYPE_GUESS_VALIDATE, 3000);

                    if (mCurrentTrial.guessable) {
                        //setAllButtonsEnabledState(true);
                        mActivityState = ACT_STATE_WAITING_FOR_GUESS;
                    }
                } else if (mActivityState == ACT_STATE_WAIT_FOR_ALERT_INTERVAL) {
                    // unexpected.  user clicked back on the alert dialog.  count this
                    // as the user wanting to bail out.
                    // TODO interesting.  this causes the unit tests to fail, as this
                    // always gets triggered...I need a better way of detecting "back".
                    //mHandler.sendEmptyMessage(MSG_TYPE_END_OF_DAY);
                }
                break;

            case MSG_TYPE_GUESS_AUDIO:
            case MSG_TYPE_GUESS_VISUAL:
                if (mActivityState == ACT_STATE_WAITING_FOR_GUESS) {
                    Log.d("Handler::handleMessage()", "Accept guess.");
                    mHandler.removeMessages(MSG_TYPE_CLEAR_FEEDBACK);
                    processMessageGuess(msg.what);
                }
                break;
            case MSG_TYPE_GUESS_VALIDATE:
                Log.d("Handler::handleMessage()", "MSG_TYPE_GUESS_ " + msg.what + ", mActivityState: " + mActivityState);
                if ((mActivityState == ACT_STATE_WAITING_FOR_GUESS) ||
                        (mActivityState == ACT_STATE_REDRAWN)) {
                    Log.d("Handler::handleMessage()", "Accept guess.");
                    validateGuess();
                }
                break;
            case MSG_TYPE_HALT_VISUAL:
                if ((mActivityState == ACT_STATE_WAITING_FOR_GUESS) || (mActivityState == ACT_STATE_REDRAWN)) {
                    mDrawView.disableAllSquares();
                    mDrawView.invalidate();
                }
                break;

            case MSG_TYPE_HALT_AUDIO:
                break;

            case MSG_TYPE_CLEAR_FEEDBACK:
                setButtonFeedback(mButtonAudio, FEEDBACK_TYPE_NONE);
                setButtonFeedback(mButtonVisual, FEEDBACK_TYPE_NONE);
                break;
                
            case MSG_TYPE_END_OF_DAY:
                Log.d("Main::Handler::handleMessage()::MSG_TYPE_END_OF_DAY", "Day is over.");
                mActivityState = ACT_STATE_STOP;
                for(int msg_type : ALL_MESSAGE_TYPES) {
                    mHandler.removeMessages(msg_type);
                }
                break;
            }
        }

    };

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mResources = mContext.getResources();
        mVibrator = (Vibrator)getSystemService(Context.VIBRATOR_SERVICE);
        mPowerManager = (PowerManager)getSystemService(Context.POWER_SERVICE);
        mWakeLock = mPowerManager.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK, "DualNBack");
        //mDialog = ProgressDialog.show(mContext, "", mResources.getString(R.string.progress_loading));
        mActivityState = ACT_STATE_CREATING;
        restoreMe();
    }

    public void initializeLayout() {
        setContentView(R.layout.main);
        mDrawView = (DrawView)findViewById(R.id.draw_view);
        mDrawView.setmHandlerUI(mHandler);
        mDrawView = (DrawView)findViewById(R.id.draw_view);
        mDrawView.setmHandlerUI(mHandler);
        mButtonAudio = (Button)findViewById(R.id.btn_audio);
        mButtonVisual = (Button)findViewById(R.id.btn_visual);
        mAllButtons = new Button[] {mButtonAudio, mButtonVisual};
        //setAllButtonsEnabledState(false);

        // ------------------------------------------------------------------------------------------------------
        // Set up button click listeners.
        // ------------------------------------------------------------------------------------------------------
        mButtonAudio.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                mHandler.sendEmptyMessage(MSG_TYPE_GUESS_AUDIO);
            }
        });
        mButtonVisual.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                mHandler.sendEmptyMessage(MSG_TYPE_GUESS_VISUAL);
            }
        });
        // ------------------------------------------------------------------------------------------------------
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        initializeLayout();
    }

    @Override
    public Object onRetainNonConfigurationInstance() {
        return null;
    }

    private void restoreMe() {
        String SUB_TAG = "restoreMe()";
        Object previous_state = getLastNonConfigurationInstance();
        Log.d(TAG+SUB_TAG, "previous_state: " + previous_state);
        if (previous_state != null) {
            // do something
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        mActivityState = ACT_STATE_CREATING;
        mHandler.sendEmptyMessage(MSG_TYPE_CREATE);
    }

    /**
     * Flush all state to somewhere safe, free all resources.
     */
    @Override
    public void onPause() {
        super.onPause();
        String SUB_TAG = "onPause()";
        Log.d(TAG+SUB_TAG, "entry");
        disableAll();
        if (mAlertDialog != null) {
            Log.d(TAG+SUB_TAG, "dimissing alert dialog: " + mAlertDialog);
            mAlertDialog.dismiss();
            setmAlertDialog(null);
        }
        if (mWakeLock.isHeld()) {
            mWakeLock.release();
        }
        mHandler.sendMessageAtFrontOfQueue(mHandler.obtainMessage(MSG_TYPE_END_OF_DAY));
    }
    
    @Override
    protected void onStop(){
       super.onStop();

      // preferences
      SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
      SharedPreferences.Editor editor = settings.edit();
      
      // save level
      int nInterval = mGameManager.getnInterval();
      editor.putInt("nInterval", nInterval);
      
      editor.commit();
    }  
    
    
    public void setAllButtonsEnabledState(boolean state) {
        Log.d("Main::setAllButtonsEnabledState()", "entry");
        for (Button button : mAllButtons) {
            button.setEnabled(state);
        } // for (Button button : mAllButtons)
    } // public void setAllButtonsEnabledState(boolean state)

    
    public void setButtonFeedback(Button button, int feedbackType) {
        Log.d("Main::setButtonFeedback()", "entry");
        switch (feedbackType) {
        case FEEDBACK_TYPE_NONE:
            button.setTextColor(Color.BLACK);
            break;
        case FEEDBACK_TYPE_GOOD:
            button.setTextColor(Color.GREEN);
            break;
        case FEEDBACK_TYPE_BAD:
            button.setTextColor(Color.RED);
            break;
        case FEEDBACK_TYPE_MISS:
            button.setTextColor(Color.BLUE);
            break;
        }
    } // public void setAllButtonsEnabledState(boolean state)
    
   public void resetButtonsFeedback() {
        Log.d("Main::resetButtonsFeedback()", "entry");
        for (Button button : mAllButtons) {
            setButtonFeedback(button, FEEDBACK_TYPE_NONE);
        }
    }
    
    private void disableAll() {
        Log.d("Main::disableAll()", "entry");
        if (mSoundManager != null) {
            mSoundManager.stopPlaying();
        }
        if (mDrawView != null) {
            mDrawView.disableAllSquares();
            mDrawView.invalidate();
        }
    } // private void disableAll()

    private void processMessageGuess(int msg_type) {
        final String SUB_TAG = "::processPartialMessageGuess";
        Log.d("Main::processPartialMessageGuess()", "entry");
        boolean is_correct;
        switch (msg_type) {
        case MSG_TYPE_GUESS_AUDIO:
            is_correct = mGameManager.evaluatePartialGuess(Guess.AUDIO);
            setButtonFeedback(mButtonAudio, is_correct ? FEEDBACK_TYPE_GOOD : FEEDBACK_TYPE_BAD);
            break;
        case MSG_TYPE_GUESS_VISUAL:
            is_correct = mGameManager.evaluatePartialGuess(Guess.VISUAL);
            setButtonFeedback(mButtonVisual, is_correct ? FEEDBACK_TYPE_GOOD : FEEDBACK_TYPE_BAD);
            break;
        default:
            // TODO raise exception.
            is_correct = false;
            break;
        } // switch (msg_type)
        Log.d(TAG+SUB_TAG, "is_correct: " + is_correct);
    }

    private void validateGuess() {
        final String SUB_TAG = "::validateGuess";
        Log.d("Main::validateGuess()", "entry");
        
        mActivityState = ACT_STATE_RECEIVED_GUESS;
        disableAll();
        boolean is_correct = mGameManager.evaluateGuess();
        Log.d(TAG+SUB_TAG, "is_correct: " + is_correct);
        
        // mark miss
        resetButtonsFeedback();
        if (mGameManager.getAudioMiss())
            setButtonFeedback(mButtonAudio, FEEDBACK_TYPE_MISS);
        if (mGameManager.getVisualMiss())
            setButtonFeedback(mButtonVisual, FEEDBACK_TYPE_MISS);
        mHandler.sendEmptyMessageDelayed(MSG_TYPE_CLEAR_FEEDBACK, 500);

        mHandler.sendEmptyMessage(MSG_TYPE_NEW_TRIAL);
        
    } // private void validateGuess()
    
    public synchronized AlertDialog getmAlertDialog() {
        String SUB_TAG = "::getmAlertDialog()";
        Log.d(TAG+SUB_TAG, "entry. mAlertDialog: " + mAlertDialog);
        Log.d(TAG+SUB_TAG, "activity: " + me.toString());
        return mAlertDialog;
    }

    public synchronized void setmAlertDialog(AlertDialog alert) {
        String SUB_TAG = "::setmAlertDialog()";
        Log.d(TAG+SUB_TAG, "entry. mAlertDialog: " + mAlertDialog + ", alert: " + alert);
        mAlertDialog = alert;
    }

    public Context getmContext() {
        return mContext;
    }
  
    public MersenneTwister getmRNG() {
        return mRNG;
    }

}
