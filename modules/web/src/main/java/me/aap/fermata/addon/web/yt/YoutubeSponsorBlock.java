package me.aap.fermata.addon.web.yt;

import static java.nio.charset.StandardCharsets.UTF_8;

import android.content.Context;
import android.content.res.Resources;

import androidx.annotation.StringRes;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import me.aap.fermata.addon.web.R;
import me.aap.utils.function.BooleanSupplier;
import me.aap.utils.log.Log;
import me.aap.utils.misc.ChangeableCondition;
import me.aap.utils.pref.PrefCondition;
import me.aap.utils.pref.PreferenceSet;
import me.aap.utils.pref.PreferenceStore;
import me.aap.utils.pref.PreferenceStore.Pref;

final class YoutubeSponsorBlock {
	private static final Pref<BooleanSupplier> ENABLED = Pref.b("YT_SPONSOR_BLOCK", false);
	private static final Pref<BooleanSupplier> SHOW_TOAST = Pref.b("YT_SB_TOAST", true);
	private static final Category[] CATEGORIES = {
			new Category("sponsor", Pref.b("YT_SB_SPONSOR", true), R.string.sponsorblock_cat_sponsor),
			new Category("selfpromo", Pref.b("YT_SB_SELFPROMO", true), R.string.sponsorblock_cat_selfpromo),
			new Category("interaction", Pref.b("YT_SB_INTERACTION", true), R.string.sponsorblock_cat_interaction),
			new Category("intro", Pref.b("YT_SB_INTRO", true), R.string.sponsorblock_cat_intro),
			new Category("outro", Pref.b("YT_SB_OUTRO", true), R.string.sponsorblock_cat_outro),
			new Category("preview", Pref.b("YT_SB_PREVIEW", true), R.string.sponsorblock_cat_preview),
			new Category("hook", Pref.b("YT_SB_HOOK", true), R.string.sponsorblock_cat_hook),
			new Category("music_offtopic", Pref.b("YT_SB_MUSIC_OFFTOPIC", true),
					R.string.sponsorblock_cat_music_offtopic),
			new Category("filler", Pref.b("YT_SB_FILLER", false), R.string.sponsorblock_cat_filler,
					R.string.sponsorblock_cat_filler_sub)
	};
	private static String script;

	private YoutubeSponsorBlock() {
	}

	static void contributeSettings(PreferenceStore ps, PreferenceSet set, ChangeableCondition visibility) {
		PreferenceSet sponsorBlock = set.subSet(o -> {
			o.title = R.string.sponsorblock;
			o.subtitle = R.string.sponsorblock_sub;
			o.visibility = visibility.copy();
		});

		sponsorBlock.addBooleanPref(o -> {
			o.store = ps;
			o.pref = ENABLED;
			o.title = R.string.sponsorblock_enable;
			o.subtitle = R.string.sponsorblock_enable_sub;
			o.visibility = visibility.copy();
		});

		sponsorBlock.addBooleanPref(o -> {
			o.store = ps;
			o.pref = SHOW_TOAST;
			o.title = R.string.sponsorblock_toast;
			o.subtitle = R.string.sponsorblock_toast_sub;
			o.visibility = sponsorBlockVisibility(visibility, ps);
		});

		for (Category c : CATEGORIES) {
			sponsorBlock.addBooleanPref(o -> {
				o.store = ps;
				o.pref = c.pref;
				o.title = c.title;
				o.subtitle = c.subtitle;
				o.visibility = sponsorBlockVisibility(visibility, ps);
			});
		}
	}

	static boolean isPreferenceChanged(List<Pref<?>> prefs) {
		if (prefs.contains(ENABLED) || prefs.contains(SHOW_TOAST)) return true;
		for (Category c : CATEGORIES) {
			if (prefs.contains(c.pref)) return true;
		}
		return false;
	}

	static String getConfigJson(Context ctx, PreferenceStore ps) {
		StringBuilder sb = new StringBuilder(512);
		sb.append("{\"enabled\":").append(ps.getBooleanPref(ENABLED))
				.append(",\"categories\":").append(getCategoriesJson(ps))
				.append(",\"actionTypes\":[\"skip\"]")
				.append(",\"showToast\":").append(ps.getBooleanPref(SHOW_TOAST))
				.append(",\"toastText\":");
		YoutubeScripts.appendJsonString(sb, ctx.getString(R.string.sponsorblock_skipped));
		sb.append(",\"toastLabels\":{");
		for (int i = 0; i < CATEGORIES.length; i++) {
			if (i > 0) sb.append(',');
			Category c = CATEGORIES[i];
			YoutubeScripts.appendJsonString(sb, c.name);
			sb.append(':');
			YoutubeScripts.appendJsonString(sb, ctx.getString(c.title));
		}
		return sb.append("}}").toString();
	}

	static String getScript(Context ctx, PreferenceStore ps) {
		if (!ps.getBooleanPref(ENABLED)) return "";

		String s = script;
		if (s != null) return s;

		try (InputStream in = ctx.getResources().openRawResource(R.raw.youtube_sponsorblock);
				 ByteArrayOutputStream out = new ByteArrayOutputStream(16 * 1024)) {
			byte[] buf = new byte[4096];
			for (int n = in.read(buf); n != -1; n = in.read(buf)) {
				out.write(buf, 0, n);
			}
			return script = new String(out.toByteArray(), UTF_8);
		} catch (Resources.NotFoundException | IOException ex) {
			Log.e(ex, "Failed to load SponsorBlock script");
			return script = "";
		}
	}

	private static String getCategoriesJson(PreferenceStore ps) {
		StringBuilder sb = new StringBuilder("[");
		for (Category c : CATEGORIES) {
			if (!ps.getBooleanPref(c.pref)) continue;
			if (sb.length() > 1) sb.append(',');
			sb.append('\"').append(c.name).append('\"');
		}
		return sb.append(']').toString();
	}

	private static ChangeableCondition sponsorBlockVisibility(ChangeableCondition visibility, PreferenceStore ps) {
		return visibility.copy().and(PrefCondition.create(ps, ENABLED));
	}

	private static final class Category {
		final String name;
		final Pref<BooleanSupplier> pref;
		@StringRes
		final int title;
		@StringRes
		final int subtitle;

		Category(String name, Pref<BooleanSupplier> pref, int title) {
			this(name, pref, title, 0);
		}

		Category(String name, Pref<BooleanSupplier> pref, int title, int subtitle) {
			this.name = name;
			this.pref = pref;
			this.title = title;
			this.subtitle = subtitle;
		}
	}
}
