package me.aap.fermata.addon.web.yt;

import android.content.Context;

import java.util.List;

import me.aap.fermata.addon.web.R;
import me.aap.utils.function.BooleanSupplier;
import me.aap.utils.misc.ChangeableCondition;
import me.aap.utils.pref.PreferenceSet;
import me.aap.utils.pref.PreferenceStore;
import me.aap.utils.pref.PreferenceStore.Pref;

/**
 * Hides non-video content from the YouTube feed: Shorts, community posts and polls.
 */
final class YoutubeCleanFeed {
	private static final Pref<BooleanSupplier> HIDE_SHORTS = Pref.b("YT_CF_SHORTS", true);
	private static final Pref<BooleanSupplier> HIDE_POSTS = Pref.b("YT_CF_POSTS", true);
	private static String script;

	private YoutubeCleanFeed() {
	}

	static void contributeSettings(PreferenceStore ps, PreferenceSet set,
																 ChangeableCondition visibility) {
		set.addBooleanPref(o -> {
			o.store = ps;
			o.pref = HIDE_SHORTS;
			o.title = R.string.hide_shorts;
			o.subtitle = R.string.hide_shorts_sub;
			o.visibility = visibility.copy();
		});
		set.addBooleanPref(o -> {
			o.store = ps;
			o.pref = HIDE_POSTS;
			o.title = R.string.hide_posts;
			o.subtitle = R.string.hide_posts_sub;
			o.visibility = visibility.copy();
		});
	}

	static boolean isPreferenceChanged(List<Pref<?>> prefs) {
		return prefs.contains(HIDE_SHORTS) || prefs.contains(HIDE_POSTS);
	}

	static boolean isEnabled(PreferenceStore ps) {
		return ps.getBooleanPref(HIDE_SHORTS) || ps.getBooleanPref(HIDE_POSTS);
	}

	static String getConfigJson(PreferenceStore ps) {
		return "{\"shorts\":" + ps.getBooleanPref(HIDE_SHORTS) +
				",\"posts\":" + ps.getBooleanPref(HIDE_POSTS) + "}";
	}

	static String getScript(Context ctx, PreferenceStore ps) {
		if (!isEnabled(ps)) return "";
		String s = script;
		if (s != null) return s;
		return script = YoutubeScripts.loadRawScript(ctx, R.raw.youtube_cleanfeed);
	}
}
