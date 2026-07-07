(function() {
  if (window.FermataAdBlock) return;

  const HIDE_CSS = [
    // Desktop
    'ytd-display-ad-renderer', 'ytd-ad-slot-renderer', 'ytd-in-feed-ad-layout-renderer',
    'ytd-banner-promo-renderer', 'ytd-statement-banner-renderer', 'ytd-brand-video-shelf-renderer',
    '#masthead-ad', '#player-ads', 'ytd-promoted-sparkles-web-renderer',
    // Mobile (m.youtube.com)
    'ytm-companion-ad-renderer', 'ytm-promoted-video-renderer', 'ytm-promoted-sparkles-web-renderer',
    'ytm-brand-video-singleton-renderer', 'ytm-brand-video-shelf-renderer',
    'ytm-statement-banner-renderer', 'ytm-in-feed-ad-layout-renderer',
    'ad-slot-renderer', '.masthead-ad',
    // Player overlays
    '.ytp-ad-overlay-container', '.ytp-ad-text-overlay', '.ytp-paid-content-overlay',
    '.ytp-featured-product'
  ].join(',') + '{display:none !important}';

  const SKIP_BUTTONS = [
    '.ytp-skip-ad-button', '.ytp-ad-skip-button', '.ytp-ad-skip-button-modern',
    'button.videoAdUiSkipButton', '.skip-button'
  ].join(',');

  const state = {
    config: {enabled: false},
    styleEl: null,
    intervalId: null,
    saved: null // {muted, rate} saved before fast-forwarding an ad
  };

  function isAdShowing() {
    return document.querySelector(
        '.ad-showing, .ad-interrupting, .ytp-ad-player-overlay-layout') != null;
  }

  function applyCss() {
    if (state.styleEl != null) return;
    const el = document.createElement('style');
    el.id = 'fermata-adblock-css';
    el.textContent = HIDE_CSS;
    (document.head || document.documentElement).appendChild(el);
    state.styleEl = el;
  }

  function removeCss() {
    if (state.styleEl == null) return;
    state.styleEl.remove();
    state.styleEl = null;
  }

  function restoreVideo(video) {
    if (state.saved == null) return;
    if (video != null) {
      video.muted = state.saved.muted;
      video.playbackRate = state.saved.rate;
    }
    state.saved = null;
  }

  function tick() {
    if (!state.config.enabled) return;

    const video = document.querySelector('video');

    if (!isAdShowing()) {
      restoreVideo(video);
      return;
    }

    // Try the skip button first
    const skip = document.querySelector(SKIP_BUTTONS);
    if (skip != null) skip.click();

    if (video == null) return;

    // Mute and fast-forward through the ad
    if (state.saved == null) {
      state.saved = {muted: video.muted, rate: video.playbackRate};
    }
    video.muted = true;
    try {
      video.playbackRate = 16;
    } catch (err) {
      console.debug('AdBlock: playbackRate rejected', err);
    }
    if (Number.isFinite(video.duration) && (video.duration > 0)) {
      video.currentTime = video.duration;
    }
  }

  window.FermataAdBlock = {
    configure(config) {
      state.config = Object.assign({}, state.config, config);

      if (state.config.enabled) {
        applyCss();
        if (state.intervalId == null) state.intervalId = setInterval(tick, 500);
        tick();
      } else {
        removeCss();
        restoreVideo(document.querySelector('video'));
        if (state.intervalId != null) {
          clearInterval(state.intervalId);
          state.intervalId = null;
        }
      }
    }
  };
})();
