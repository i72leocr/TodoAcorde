package com.tuguitar.todoacorde.songs.domain;

/**
 * Claves y valores para SharedPreferences usadas por SongViewModel.
 */
public final class PrefsKeys {
    private PrefsKeys() {}
    public static final String KEY_SHOW_FAV    = "show_favorites_only";
    public static final String KEY_SORT_CRIT   = "sort_criterion";
    public static final String KEY_SORT_ASC    = "sort_order";
    public static final String VAL_SORT_TITLE  = "title";
    public static final String VAL_SORT_DIFF   = "diff";
}
