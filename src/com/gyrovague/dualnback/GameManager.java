/**
 * Tracks the state of the game and determines what visuals/sounds to produce, the current
 * difficulty, game statistics.
 *
 * Keep in mind that the main activity can be interrupted at any time, and an interruption
 * constitutes "losing" the current difficulty but doesn't mean the user gets penalised
 * otherwise.
 *
 * Also note that the original study methodology is vague about what the user response should
 * be when both visual and auditory targets are present.  In my implementation I assume that
 * a distinct, third response is required (i.e. the possible user responses are four-fold:
 * no repetition, audio repetition, visual repetition, and both audio and visual repetition).
 *
 * Reference: "Improving fluid intelligence with training on working memory",
 * PNAS May 13, 2008 vol. 105 no. 19 6829-6833.  Here is the most
 * useful excerpt:
 *
 * "Materials. Training task. For the training task, we used the same material as described by Jaeggi et al. (33),
 * which was a dual n-back task where squares at eight different locations were presented sequentially on a computer
 * screen at a rate of 3 s (stimulus length, 500 ms; interstimulus interval, 2,500 ms). Simultaneously with the
 * presentation of the squares, one of eight consonants was presented sequentially through headphones. A response
 * was required whenever one of the presented stimuli matched the one presented n positions back in the sequence.
 * The value of n was the same for both streams of stimuli. There were six auditory and six visual targets per block
 * (four appearing in only one modality, and two appearing in both modalities simultaneously), and their positions were
 * determined randomly. Participants made responses manually by pressing on the letter `A` of a standard keyboard with
 * their left index finger for visual targets, and on the letter `L` with their right index finger for auditory targets.
 * No responses were required for non-targets.
 *
 * In this task, the level of difficulty was varied by changing the level of n (34), which we used to track the
 * participants' performance. After each block, the participants' individual performance was analyzed, and in the
 * following block, the level of n was adapted accordingly: If the participant made fewer than three mistakes per
 * modality, the level of n increased by 1. It was decreased by 1 if more than five mistakes were made, and in all
 * other cases, n remained unchanged.
 *
 * One training session comprised 20 blocks consisting of 20 + n trials resulting in a daily training time of 25 min."
 */
package com.gyrovague.dualnback;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import android.os.Handler;
import android.util.Log;
import ec.util.MersenneTwister;

/**
 * @author asimihsan
 *
 */
public class GameManager {
    private static final String TAG = "GameManager";
    public static final int NINTERVAL_MIN = 2;
    private int mNInterval = NINTERVAL_MIN;
    private double mRate = 0.0;
    private int mCurrentBlock;
    private int mCurrentTrial;
    private int mCurrentGuess;
    private int mCurrentWrongs;
    private int mCurrentRights;
    private int mnFallBackSessions;
    private MersenneTwister mRNG;
    private ArrayList<Integer> mHistoryVisual;
    private ArrayList<Integer> mHistoryAudio;
    private Handler mHandlerUI;
    private static final double THRESHOLD_ADVANCE = 0.8;
    private static final double THRESHOLD_FALLBACK = 0.5;
    private static final int THRESHOLD_FALLBACK_SESSIONS = 3;
    private static final int BLOCK_SIZE = 20;
    private static final int AUDIO_TARGETS_PER_BLOCK = 6;
    private static final int VISUAL_TARGETS_PER_BLOCK = 6;
    private static final int BOTH_MODES_PER_BLOCK = 2;
    private static final int NON_TARGETS_PER_BLOCK = BLOCK_SIZE - AUDIO_TARGETS_PER_BLOCK - VISUAL_TARGETS_PER_BLOCK + BOTH_MODES_PER_BLOCK;
    private static final int NUM_SQUARES = 8;
    private static final int NUM_CONSONANTS = 8;
    private static final int BLOCKS_PER_DAY = 20;

    GameManager(MersenneTwister mRNG) {
        this.mRNG = mRNG;
        commonConstructor();
    }

    GameManager(MersenneTwister RNG, int nInterval) {
        this.mRNG = RNG;
        this.mNInterval = nInterval;
        commonConstructor();
    }

    private void commonConstructor() {
        reset();
    }

    private void reset() {
        mCurrentBlock = 0;
        mCurrentGuess = Guess.NONE;
        mnFallBackSessions = 0;
    }

    public Trial getCurrentTrial() {
        int current_audio = mHistoryAudio.get(mCurrentTrial);
        int current_visual = mHistoryVisual.get(mCurrentTrial);
        boolean guessable = (mCurrentTrial > (mNInterval - 1)) ? true : false;
        return new Trial(current_audio, current_visual, guessable);
    }

    private int getCurrentCorrectAnswer() {
        int previous_audio = mHistoryAudio.get(mCurrentTrial - mNInterval);
        int previous_visual = mHistoryVisual.get(mCurrentTrial - mNInterval);
        int current_audio = mHistoryAudio.get(mCurrentTrial);
        int current_visual = mHistoryVisual.get(mCurrentTrial);
        int correct_answer = Guess.NONE;
        if (current_visual == previous_visual) {
            correct_answer |= Guess.VISUAL;
        } 
        if (current_audio == previous_audio) {
            correct_answer |= Guess.AUDIO;
        }
        return correct_answer;

    } // private int getCurrentCorrectAnswer()

    public boolean evaluatePartialGuess(int guess) {
        mCurrentGuess |= guess;
        
        int correct_answer = getCurrentCorrectAnswer();
        int common = guess & correct_answer;
        boolean is_guess_correct = (common != 0);
        
        return is_guess_correct;
    }

    public boolean evaluateGuess() {
        final String SUB_TAG = "::evaluateGuess()";
        Log.d(TAG + SUB_TAG, "entry.  guess: " + mCurrentGuess);
        boolean guessable = (mCurrentTrial > (mNInterval - 1)) ? true : false;
        Log.d(TAG + SUB_TAG, "mCurrentTrial: " + mCurrentTrial + ", mNInterval: " + mNInterval + ", guessable: " + guessable);
        if (!guessable) {
            mCurrentTrial++;
            return true;
        }

        int correct_answer = getCurrentCorrectAnswer();
        
        boolean is_guess_correct = (mCurrentGuess == correct_answer);
        
        int common = mCurrentGuess & correct_answer;
        if ((common & Guess.VISUAL) != 0) {
            mCurrentRights ++;
        }
        if ((common & Guess.AUDIO) != 0) {
            mCurrentRights ++;
        }
        
        int delta = mCurrentGuess ^ correct_answer;
        if ((delta & Guess.VISUAL) != 0) {
            mCurrentWrongs ++;
        }
        if ((delta & Guess.AUDIO) != 0) {
            mCurrentWrongs ++;
        }
        
        mCurrentGuess = Guess.NONE;
        mCurrentTrial ++;
        return is_guess_correct;
    }

    public void advanceBlock() {
        mRate = 1.0 * mCurrentRights / (1.0 * mCurrentWrongs + 1.0 * mCurrentRights);
        if (mRate >= THRESHOLD_ADVANCE) {
            mNInterval ++;
            mnFallBackSessions = 0;
        } else if (mRate < THRESHOLD_FALLBACK) {
            if (mnFallBackSessions < THRESHOLD_FALLBACK_SESSIONS) {
              // do not fallback right away
              mnFallBackSessions ++;
            }
            else {
              // fallback
              setnInterval(mNInterval - 1);
              mnFallBackSessions = 0;
            }
        }
        mCurrentBlock ++;
        prepareCurrentBlock();
    } // public void advanceBlock()

    public boolean isCurrentBlockFinished() {
        return (mCurrentTrial >= (BLOCK_SIZE + mNInterval));
    }

    public boolean isCurrentDayFinished() {
        return (mCurrentBlock >= (BLOCKS_PER_DAY));
    }

    public void prepareCurrentBlock() {
        mCurrentTrial = 0;
        mCurrentWrongs = 0;
        mCurrentRights = 0;
        
        mHistoryVisual = new ArrayList<Integer>(BLOCK_SIZE);
        mHistoryAudio = new ArrayList<Integer>(BLOCK_SIZE);

        int numberNonTargets = NON_TARGETS_PER_BLOCK;
        int numberAudioTargets = AUDIO_TARGETS_PER_BLOCK - BOTH_MODES_PER_BLOCK;
        int numberVisualTargets = VISUAL_TARGETS_PER_BLOCK - BOTH_MODES_PER_BLOCK;
        int numberBothTargets = BOTH_MODES_PER_BLOCK;
        int marker = 0;
        mHistoryVisual.clear();
        mHistoryAudio.clear();

        // the first N entries will be completely random, since there can be
        // no repetitions.
        while (marker <= mNInterval) {
            mHistoryVisual.add(mRNG.nextInt(NUM_SQUARES));
            mHistoryAudio.add(mRNG.nextInt(NUM_CONSONANTS));
            marker += 1;
        }

        final int NON_TARGET = 0;
        final int AUDIO_TARGET = 1;
        final int VISUAL_TARGET = 2;
        final int BOTH_TARGETS = 3;
        Set<Integer> target_types = new HashSet<Integer>(Arrays.asList(new Integer[] {NON_TARGET, AUDIO_TARGET, VISUAL_TARGET, BOTH_TARGETS}));
        Integer target_types_list[] = new Integer[target_types.size()];
        target_types_list = target_types.toArray(target_types_list);
        boolean choice_invalid;
        final int limit = BLOCK_SIZE + mNInterval;

        while (marker <= limit) {
            choice_invalid = false;
            int previous_visual = mHistoryVisual.get(marker - mNInterval);
            int previous_audio = mHistoryAudio.get(marker - mNInterval);
            int choice = target_types_list[mRNG.nextInt(target_types_list.length)];
            switch(choice) {
            case NON_TARGET:
                // non-target
                if (numberNonTargets > 0) {
                    mHistoryVisual.add(nextIntExceptN(NUM_SQUARES, previous_visual));
                    mHistoryAudio.add(nextIntExceptN(NUM_CONSONANTS, previous_audio));
                    numberNonTargets--;
                    marker += 1;
                }
                if (numberNonTargets <= 0) {
                    choice_invalid = true;
                }

                break;

            case AUDIO_TARGET:
                // audio-only target
                if (numberAudioTargets > 0) {
                    mHistoryVisual.add(nextIntExceptN(NUM_SQUARES, previous_visual));
                    mHistoryAudio.add(previous_audio);
                    numberAudioTargets--;
                    marker += 1;
                }
                if (numberAudioTargets <= 0) {
                    choice_invalid = true;
                }
                break;

            case VISUAL_TARGET:
                // visual-only target
                if (numberVisualTargets > 0) {
                    mHistoryVisual.add(previous_visual);
                    mHistoryAudio.add(nextIntExceptN(NUM_CONSONANTS, previous_audio));
                    numberVisualTargets--;
                    marker += 1;
                }
                if (numberVisualTargets <= 0) {
                    choice_invalid = true;
                }
                break;

            case BOTH_TARGETS:
                // both target
                if (numberBothTargets > 0) {
                    mHistoryVisual.add(previous_visual);
                    mHistoryAudio.add(previous_audio);
                    numberBothTargets--;
                    marker += 1;
                }
                if (numberBothTargets <= 0) {
                    choice_invalid = true;
                }
                break;
            }

            if (choice_invalid && target_types.contains(choice)) {
                target_types.remove(choice);
                target_types_list = new Integer[target_types.size()];
                target_types_list = target_types.toArray(target_types_list);
            } // if (choice_invalid && target_types.contains(choice))
        } // while (marker <= limit)

    } // private void prepareCurrentBlock()

    private int nextIntExceptN(int limit, int n) {
        int result = n;
        while (result == n) {
            result = mRNG.nextInt(limit);
        }
        return result;
    }
    
    public double getRate() {
        return mRate;
    }

    public int getnInterval() {
        return mNInterval;
    }

    public void setnInterval(int nInterval) {
        mNInterval = Math.max(nInterval, NINTERVAL_MIN);
    }

    public void setHandlerUI(Handler handlerUI) {
        mHandlerUI = handlerUI;
    }

    public int getCurrentBlock() {
        return mCurrentBlock;
    }
    
}
