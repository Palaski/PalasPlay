package me.aap.fermata.addon.web.yt;

import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;

import androidx.annotation.NonNull;

import java.io.ByteArrayInputStream;

import me.aap.fermata.addon.web.FermataWebClient;
import me.aap.fermata.addon.web.WebBrowserFragment;
import me.aap.fermata.ui.activity.MainActivityDelegate;
import me.aap.utils.log.Log;

/**
 * @author Andrey Pavlenko
 */
public class YoutubeWebClient extends FermataWebClient {

	@Override
	public WebResourceResponse shouldInterceptRequest(@NonNull WebView view,
																										@NonNull WebResourceRequest request) {
		if ((view instanceof YoutubeWebView ytView) && YoutubeAdBlock.shouldBlockRequest(
				ytView.getAddon().getPreferenceStore(), request.getUrl())) {
			return new WebResourceResponse("text/plain", "utf-8",
					new ByteArrayInputStream(new byte[0]));
		}
		return super.shouldInterceptRequest(view, request);
	}

	@Override
	public boolean shouldOverrideUrlLoading(@NonNull WebView view, @NonNull WebResourceRequest request) {
		if (!isYoutubeUri(request.getUrl())) {
			MainActivityDelegate a = MainActivityDelegate.get(view.getContext());

			try {
				if (!(a.showFragment(
						me.aap.fermata.R.id.web_browser_fragment) instanceof WebBrowserFragment f))
					return false;
				f.loadUrl(request.getUrl().toString());
				return true;
			} catch (IllegalArgumentException ex) {
				Log.d(ex);
			}
		}

		return false;
	}
}
