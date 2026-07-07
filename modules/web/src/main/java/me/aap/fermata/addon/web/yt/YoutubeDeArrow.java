package me.aap.fermata.addon.web.yt;

import android.content.Context;

import java.util.List;

import me.aap.fermata.addon.web.R;
import me.aap.utils.function.BooleanSupplier;
import me.aap.utils.misc.ChangeableCondition;
import me.aap.utils.pref.PrefCondition;
import me.aap.utils.pref.PreferenceSet;
import me.aap.utils.pref.PreferenceStore;
import me.aap.utils.pref.PreferenceStore.Pref;

/**
 * DeArrow integration: replaces clickbait titles and thumbnails with
 * community-submitted alternatives (https://dearrow.ajay.app).
 */
final class YoutubeDeArrow {
	private static final Pref<BooleanSupplier> ENABLED = Pref.b("YT_DEARROW", false);
	private static final Pref<BooleanSupplier> TITLES = Pref.b("YT_DEARROW_TITLES", true);
	private static final Pref<BooleanSupplier> THUMBNAILS = Pref.b("YT_DEARROW_THUMBS", true);
	private static String script;

	private YoutubeDeArrow() {
	}

	static void contributeSettings(PreferenceStore ps, PreferenceSet set,
																 ChangeableCondition visibility) {
		PreferenceSet deArrow = set.subSet(o -> {
			o.title = R.string.dearrow;
			o.subtitle = R.string.dearrow_sub;
			o.visibility = visibility.copy();
		});

		deArrow.addBooleanPref(o -> {
			o.store = ps;
			o.pref = ENABLED;
			o.title = R.string.dearrow_enable;
			o.subtitle = R.string.dearrow_enable_sub;
			o.visibility = visibility.copy();
		});
		deArrow.addBooleanPref(o -> {
			o.store = ps;
			o.pref = TITLES;
			o.title = R.string.dearrow_titles;
			o.visibility = visibility.copy().and(PrefCondition.create(ps, ENABLED));
		});
		deArrow.addBooleanPref(o -> {
			o.store = ps;
			o.pref = THUMBNAILS;
			o.title = R.string.dearrow_thumbnails;
			o.visibility = visibility.copy().and(PrefCondition.create(ps, ENABLED));
		});
	}

	static boolean isPreferenceChanged(List<Pref<?>> prefs) {
		return prefs.contains(ENABLED) || prefs.contains(TITLES) || prefs.contains(THUMBNAILS);
	}

	static boolean isEnabled(PreferenceStore ps) {
		return ps.getBooleanPref(ENABLED);
	}

	static String getConfigJson(PreferenceStore ps) {
		return "{\"enabled\":" + isEnabled(ps) +
				",\"titles\":" + ps.getBooleanPref(TITLES) +
				",\"thumbnails\":" + ps.getBooleanPref(THUMBNAILS) + "}";
	}

	static String getScript(Context ctx, PreferenceStore ps) {
		if (!isEnabled(ps)) return "";
		String s = script;
		if (s != null) return s;
		return script = YoutubeScripts.loadRawScript(ctx, R.raw.youtube_dearrow);
	}
}
