package me.aap.fermata.addon.web.yt;

import static java.nio.charset.StandardCharsets.UTF_8;

import android.content.Context;
import android.content.res.Resources;

import androidx.annotation.RawRes;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import me.aap.utils.log.Log;

/**
 * Helper for loading JavaScript snippets from raw resources.
 */
final class YoutubeScripts {

	private YoutubeScripts() {
	}

	static String loadRawScript(Context ctx, @RawRes int res) {
		try (InputStream in = ctx.getResources().openRawResource(res);
				 ByteArrayOutputStream out = new ByteArrayOutputStream(16 * 1024)) {
			byte[] buf = new byte[4096];
			for (int n = in.read(buf); n != -1; n = in.read(buf)) {
				out.write(buf, 0, n);
			}
			return new String(out.toByteArray(), UTF_8);
		} catch (Resources.NotFoundException | IOException ex) {
			Log.e(ex, "Failed to load script resource");
			return "";
		}
	}

	static void appendJsonString(StringBuilder sb, String s) {
		sb.append('"');
		for (int i = 0, n = s.length(); i < n; i++) {
			char c = s.charAt(i);
			switch (c) {
				case '"' -> sb.append("\\\"");
				case '\\' -> sb.append("\\\\");
				case '\n' -> sb.append("\\n");
				case '\r' -> sb.append("\\r");
				case '\t' -> sb.append("\\t");
				default -> sb.append(c);
			}
		}
		sb.append('"');
	}
}
