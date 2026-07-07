package me.aap.fermata.addon.web.yt;

import static me.aap.fermata.BuildConfig.AUTO;

import android.content.Context;

import androidx.annotation.IdRes;
import androidx.annotation.Keep;
import androidx.annotation.NonNull;

import java.util.List;

import me.aap.fermata.FermataApplication;
import me.aap.fermata.addon.AddonInfo;
import me.aap.fermata.addon.FermataAddon;
import me.aap.fermata.addon.web.R;
import me.aap.fermata.addon.web.WebBrowserAddon;
import me.aap.fermata.ui.activity.MainActivityPrefs;
import me.aap.utils.function.BooleanSupplier;
import me.aap.utils.function.IntSupplier;
import me.aap.utils.function.Supplier;
import me.aap.utils.misc.ChangeableCondition;
import me.aap.utils.pref.PreferenceSet;
import me.aap.utils.pref.PreferenceStore;
import me.aap.utils.ui.fragment.ActivityFragment;

/**
 * @author Andrey Pavlenko
 */
@Keep
@SuppressWarnings("unused")
public class YoutubeAddon extends WebBrowserAddon implements PreferenceStore.Listener {
	@NonNull
	private static final AddonInfo info = FermataAddon.findAddonInfo(YoutubeAddon.class.getName());
	public static final int YT_DARK_MODE_DISABLED = 0;
	public static final int YT_DARK_MODE_ENABLED = 1;
	public static final int YT_DARK_MODE_AUTO = 2;
	private static final Pref<IntSupplier> YT_DARK_MODE = Pref.i("YT_DARK_MODE", YT_DARK_MODE_AUTO);
	private static final Pref<BooleanSupplier> YT_DESKTOP_VERSION = Pref.b("YT_DESKTOP_VERSION", false);
	private static final Pref<Supplier<String[]>> YT_BOOKMARKS = Pref.sa("YT_BOOKMARKS");
	private static final Pref<Supplier<String>> VIDEO_SCALE = Pref.s("VIDEO_SCALE", VideoScale.CONTAIN::prefName);
	private static final Pref<BooleanSupplier> YT_OPEN_ON_START = Pref.b("YT_OPEN_ON_START", false);
	private static final Pref<BooleanSupplier> YT_AUTO_HIGHEST_QUALITY =
			Pref.b("YT_AUTO_HIGHEST_QUALITY", false);
	private static final Pref<Supplier<String>> YT_PREFERRED_QUALITY =
			Pref.s("YT_PREFERRED_QUALITY", "auto");
	// Ordered list of (pref value, label). Values match YouTube player quality tokens.
	private static final String[][] QUALITY_LEVELS = {
			{"highest", null}, // label resolved from resources
			{"hd2160", "2160p (4K)"},
			{"hd1440", "1440p"},
			{"hd1080", "1080p (Full HD)"},
			{"hd720", "720p"},
			{"large", "480p"},
			{"medium", "360p"},
	};
	private static final Pref<BooleanSupplier> YT_SKIP_ADD = AUTO ? Pref.b("YT_SKIP_ADD", true) : null;
	private boolean ignorePrefChange;

	@IdRes
	@Override
	public int getAddonId() {
		return me.aap.fermata.R.id.youtube_fragment;
	}

	@NonNull
	public AddonInfo getInfo() {
		return info;
	}

	@NonNull
	@Override
	public ActivityFragment createFragment() {
		return new YoutubeFragment();
	}

	@Override
	public Pref<IntSupplier> getForceDarkPref() {
		return YT_DARK_MODE;
	}

	@Override
	public Pref<BooleanSupplier> getDesktopVersionPref() {
		return YT_DESKTOP_VERSION;
	}

	@Override
	public Pref<Supplier<String[]>> getBookmarksPref() {
		return YT_BOOKMARKS;
	}

	boolean skipAd() {
		return AUTO && getPreferenceStore().getBooleanPref(YT_SKIP_ADD);
	}

	@Override
	public void contributeSettings(Context ctx, PreferenceStore store, PreferenceSet set,
																 ChangeableCondition visibility) {
		super.contributeSettings(ctx, store, set, visibility);
		getPreferenceStore().addBroadcastListener(this);
		MainActivityPrefs.get().addBroadcastListener(this);
		FermataApplication.get().getPreferenceStore().addBroadcastListener(this);

		set.addBooleanPref(o -> {
			o.store = getPreferenceStore();
			o.pref = YT_OPEN_ON_START;
			o.title = R.string.open_on_start;
			o.visibility = visibility;
		});
		set.addListPref(o -> {
			var values = new java.util.ArrayList<android.util.Pair<String, String>>(
					QUALITY_LEVELS.length + 1);
			values.add(new android.util.Pair<>("auto", ctx.getString(R.string.preferred_quality_auto)));
			for (String[] level : QUALITY_LEVELS) {
				values.add(new android.util.Pair<>(level[0], (level[1] != null) ? level[1]
						: ctx.getString(R.string.preferred_quality_highest)));
			}
			o.setStringValues(getPreferenceStore(), YT_PREFERRED_QUALITY, values);
			o.title = R.string.preferred_video_quality;
			o.subtitle = me.aap.fermata.R.string.string_format;
			o.formatSubtitle = true;
			o.visibility = visibility;
		});

		if (AUTO) {
			set.addBooleanPref(o -> {
				o.store = getPreferenceStore();
				o.pref = YT_SKIP_ADD;
				o.title = R.string.try_to_skip_ad;
				o.visibility = visibility;
			});
		}

		YoutubeAdBlock.contributeSettings(getPreferenceStore(), set, visibility);
		YoutubeSponsorBlock.contributeSettings(getPreferenceStore(), set, visibility);
		YoutubeDeArrow.contributeSettings(getPreferenceStore(), set, visibility);
	}

	@Override
	public void onPreferenceChanged(PreferenceStore store, List<Pref<?>> prefs) {
		if (ignorePrefChange) return;
		ignorePrefChange = true;

		if (prefs.contains(getInfo().enabledPref)) {
			if (!store.getBooleanPref(getInfo().enabledPref)) {
				MainActivityPrefs ap = MainActivityPrefs.get();
				getPreferenceStore().applyBooleanPref(YT_OPEN_ON_START, false);
				if (getInfo().className.equals(ap.getShowAddonOnStartPref()))
					ap.setShowAddonOnStartPref(null);
			}
		} else if (prefs.contains(YT_OPEN_ON_START)) {
			MainActivityPrefs ap = MainActivityPrefs.get();
			if (store.getBooleanPref(YT_OPEN_ON_START)) {
				ap.setShowAddonOnStartPref(getInfo().className);
			} else if (getInfo().className.equals(ap.getShowAddonOnStartPref())) {
				ap.setShowAddonOnStartPref(null);
			}
		} else if (prefs.contains(MainActivityPrefs.SHOW_ADDON_ON_START)) {
			getPreferenceStore().applyBooleanPref(YT_OPEN_ON_START,
					getInfo().className.equals(MainActivityPrefs.get().getShowAddonOnStartPref()));
		}

		ignorePrefChange = false;
	}

	@Override
	public void uninstall() {
		getPreferenceStore().removeBroadcastListener(this);
		MainActivityPrefs.get().removeBroadcastListener(this);
		FermataApplication.get().getPreferenceStore().removeBroadcastListener(this);
	}

	VideoScale getScale() {
		switch (getPreferenceStore().getStringPref(VIDEO_SCALE)) {
			case "fill":
				return VideoScale.FILL;
			case "contain":
				return VideoScale.CONTAIN;
			case "cover":
				return VideoScale.COVER;
			default:
				return VideoScale.NONE;
		}
	}

	void setScale(VideoScale scale) {
		getPreferenceStore().applyStringPref(VIDEO_SCALE, scale.prefName());
	}

	boolean autoHighestQuality() {
		return getPreferredQuality() != null;
	}

	/**
	 * @return the preferred quality token ("highest", "hd1080", ...) or null if disabled (auto).
	 */
	String getPreferredQuality() {
		String q = getPreferenceStore().getStringPref(YT_PREFERRED_QUALITY);
		if ((q != null) && !q.isEmpty() && !"auto".equals(q)) return q;
		// Backward compatibility with the old boolean preference
		return getPreferenceStore().getBooleanPref(YT_AUTO_HIGHEST_QUALITY) ? "highest" : null;
	}

	boolean autoHighestQualityChanged(List<Pref<?>> prefs) {
		return prefs.contains(YT_AUTO_HIGHEST_QUALITY) || prefs.contains(YT_PREFERRED_QUALITY);
	}

	enum VideoScale {
		FILL, CONTAIN, COVER, NONE;

		String prefName() {
			return name().toLowerCase();
		}
	}
}
