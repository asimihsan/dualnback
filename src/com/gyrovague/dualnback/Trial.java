/**
 * Convenience class for storing a particular trial's state.
 */
package com.gyrovague.dualnback;

/**
 * @author asimihsan
 *
 */
public class Trial {
    public int audio;
    public int visual;
    public boolean guessable;

    /**
     * Constructor.
     * @param audio What audio choice to play.
     * @param visual What visual square to display.
     * @param guessable Whether this trial can be guessed.  If this is within the first N trials then the user
     * can't guess it yet.
     */
    Trial(int audio, int visual, boolean guessable) {
        this.audio = audio;
        this.visual = visual;
        this.guessable = guessable;
    }

    /**
     * Constructor for when the guessability of the trial is unimportant.
     * @param audio What audio choice to play or guessed.
     * @param visual What visual choice to play or guessed.
     */
    Trial(int audio, int visual) {
        this.audio = audio;
        this.visual = visual;
    }
}
