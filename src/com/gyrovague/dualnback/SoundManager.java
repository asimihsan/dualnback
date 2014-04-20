/**
 * Deal with playing/stopping sounds.
 */
package com.gyrovague.dualnback;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.content.res.Resources;
import android.content.res.Resources.NotFoundException;
import android.media.MediaPlayer;
import ec.util.MersenneTwister;

/**
 * @author asimihsan
 *
 */
public class SoundManager {
    private static final int NUM_CONSONANTS = 8;

    private final Context mContext;
    private final MersenneTwister mRNG;
    private boolean resourcesAllocated = false;
    private boolean isPlaying = false;
    private int currentSound;

    /**
     * All possible consonants, and the eight we're using now.
     */
    private static final int[] mConsonants = new int[] {R.raw.b, R.raw.c, R.raw.d, R.raw.f, R.raw.g, R.raw.h, R.raw.j, R.raw.k, R.raw.l, R.raw.m, R.raw.n, R.raw.p, R.raw.q, R.raw.r, R.raw.s, R.raw.t, R.raw.v, R.raw.w, R.raw.x, R.raw.y, R.raw.z};
    private final int[] mConsonantsInUse = new int[NUM_CONSONANTS];
    private final MediaPlayer[] mConsonantMediaPlayers = new MediaPlayer[NUM_CONSONANTS];

    /**
     * Constructor.  Create all MediaPlayer instances up front.
     * @param context Context within which to play the sounds.
     * @param RNG Mersenne twister RNG.
     */
    public SoundManager(Context context, MersenneTwister RNG) {
        this.mContext = context;
        this.mRNG = RNG;

        // choose eight consonants to play.
        Set<Integer> set = new HashSet<Integer>();
        int marker = 0;
        int current_consonant;
        int num_consonants = mConsonants.length;
        while (marker < NUM_CONSONANTS) {
            current_consonant = mConsonants[mRNG.nextInt(num_consonants)];
            if (set.contains(current_consonant) != true) {
                set.add(current_consonant);
                mConsonantsInUse[marker] = current_consonant;
                marker++;
            } // if (set.contains(current_consonant) != true)
        } // while (marker < NUM_CONSONANTS)
    } // SoundManager(Context context, MersenneTwisterFast RNG)

    /**
     * Allocate all required resources.
     */
    public boolean initialize() {
        Resources resources = mContext.getResources();
        AssetFileDescriptor afd;
        boolean result = true;
        if (!resourcesAllocated) {
            for (int i = NUM_CONSONANTS-1; i >= 0; i--) {

                // ------------------------------------------------------------------------------
                // don't try to create() all eight consonants now. this'll mean we try
                // to prepare() all eight, which in 1.5 will cause us to exhaust
                // MAX_OPENCORE_INSTANCES = 25 if we accidentally attempt to allocate
                // three SoundManagers (happened in an older version), or even worse
                // in 1.6+ cause us to fail the prepare() call after four or five
                // consonants.
                //
                // bottom line is that we're expected to have a minimal number of
                // MediaPlayer instances prepared at any given time.
                // ------------------------------------------------------------------------------
                mConsonantMediaPlayers[i] = new MediaPlayer();
                mConsonantMediaPlayers[i].reset();
                try {
                    afd = resources.openRawResourceFd(mConsonantsInUse[i]);
                    mConsonantMediaPlayers[i].setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
                    afd.close();
                } catch (IllegalArgumentException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } catch (IllegalStateException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } catch (NotFoundException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                //mConsonantMediaPlayers[i] = MediaPlayer.create(mContext, mConsonantsInUse[i]);
                /*
                if (mConsonantMediaPlayers[i] == null) {
                    result = false;
                    break;
                } else {
                    mConsonantMediaPlayers[i].setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                        public void onCompletion(MediaPlayer mp) {
                            mp.seekTo(0);
                            isPlaying = false;
                        }
                    });
                }
                */
            }
            resourcesAllocated = true;
        }

        if (!result) {
            resourcesAllocated = true;
            close();
            resourcesAllocated = false;
            return false;
        }

        return true;
    }

    /**
     * Free up all allocated resources.
     */
    public void close() {
        stopPlaying();
        if (resourcesAllocated) {
            for (int i = NUM_CONSONANTS-1; i >= 0; i--) {
                if (mConsonantMediaPlayers[i] != null) {
                    mConsonantMediaPlayers[i].release();
                }
            }
            resourcesAllocated = false;
        }
    }

    /**
     * Stop playing any sound.
     */
    public void stopPlaying() {
        if (isPlaying == true) {
            mConsonantMediaPlayers[currentSound].stop();
            /*
            try {
                mConsonantMediaPlayers[currentSound].prepare();
            } catch (IllegalStateException e) {
                // TODO Auto-generated catch block
                Log.e("SoundManager::stopPlaying()", "IllegalStateException in prepare().", e);
            } catch (IOException e) {
                // TODO Auto-generated catch block
                Log.e("SoundManager::stopPlaying()", "IOException in prepare().", e);
            }
            */
        }
        isPlaying = false;
    }

    /**
     * Play a random consonant.
     */
    public void playRandomConsonant() {
        playSound(mRNG.nextInt(NUM_CONSONANTS));
    }

    /**
     * Play a sound
     * @param index Index into the consonants array.  Must be < 8.
     */
    public void playSound(int index) {
        if ((resourcesAllocated == false) || (mConsonantMediaPlayers[index] == null)) {
            boolean result = initialize();
            if (!result) {
                resourcesAllocated = true;
                close();
                resourcesAllocated = false;
                initialize();
            }
        }

        try {
            mConsonantMediaPlayers[index].prepare();
        } catch (IllegalStateException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        mConsonantMediaPlayers[index].seekTo(0);
        mConsonantMediaPlayers[index].start();
        //mConsonantMediaPlayers[index].start();
        currentSound = index;
        isPlaying = true;
    }

    public boolean isresourcesAllocated() {
        return resourcesAllocated;
    }

    public boolean isPlaying() {
        return isPlaying;
    }

    public int getCurrentSound() {
        return currentSound;
    }

}
