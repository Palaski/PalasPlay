package me.aap.fermata.addon.web.yt;

import android.content.Context;
import android.net.Uri;

import java.util.List;

import me.aap.fermata.addon.web.R;
import me.aap.utils.function.BooleanSupplier;
import me.aap.utils.misc.ChangeableCondition;
import me.aap.utils.pref.PreferenceSet;
import me.aap.utils.pref.PreferenceStore;
import me.aap.utils.pref.PreferenceStore.Pref;

/**
 * Blocks and skips YouTube ads: hides static ad elements, fast-forwards video ads
 * and blocks requests to known ad-serving hosts.
 */
final class YoutubeAdBlock {
	private static final Pref<BooleanSupplier> ENABLED = Pref.b("YT_ADBLOCK", false);
	private static final String[] BLOCKED_HOSTS = {
			"doubleclick.net",
			"googlesyndication.com",
			"googleadservices.com",
			"adservice.google.com",
	};
	private static String script;

	private YoutubeAdBlock() {
	}

	static void contributeSettings(PreferenceStore ps, PreferenceSet set,
																 ChangeableCondition visibility) {
		set.addBooleanPref(o -> {
			o.store = ps;
			o.pref = ENABLED;
			o.title = R.string.adblock_enable;
			o.subtitle = R.string.adblock_enable_sub;
			o.visibility = visibility.copy();
		});
	}

	static boolean isPreferenceChanged(List<Pref<?>> prefs) {
		return prefs.contains(ENABLED);
	}

	static boolean isEnabled(PreferenceStore ps) {
		return ps.getBooleanPref(ENABLED);
	}

	static String getConfigJson(PreferenceStore ps) {
		return "{\"enabled\":" + isEnabled(ps) + "}";
	}

	static String getScript(Context ctx, PreferenceStore ps) {
		if (!isEnabled(ps)) return "";
		String s = script;
		if (s != null) return s;
		return script = YoutubeScripts.loadRawScript(ctx, R.raw.youtube_adblock);
	}

	static boolean shouldBlockRequest(PreferenceStore ps, Uri uri) {
		if (!isEnabled(ps)) return false;
		String host = uri.getHost();
		if (host == null) return false;

		for (String blocked : BLOCKED_HOSTS) {
			if (host.equals(blocked) || host.endsWith('.' + blocked)) return true;
		}

		String path = uri.getPath();
		return (path != null) && (path.startsWith("/pagead/") || path.startsWith("/ptracking"));
	}
}
