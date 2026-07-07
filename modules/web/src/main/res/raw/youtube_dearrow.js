(function() {
  if (window.FermataDeArrow) return;

  const state = {
    config: {
      enabled: false,
      titles: true,
      thumbnails: true,
      endpoint: 'https://sponsor.ajay.app/api/branding',
      thumbEndpoint: 'https://dearrow-thumb.ajay.app/api/v1/getThumbnail'
    },
    cache: new Map(),   // videoId -> {title: string|null, thumbTime: number|null} or 'pending'
    intervalId: null
  };

  const TITLE_SELECTORS = [
    // Desktop
    '#video-title', 'yt-formatted-string#video-title',
    '.yt-lockup-metadata-view-model-wiz__title > span',
    // Mobile (m.youtube.com)
    '.media-item-headline .yt-core-attributed-string',
    '.compact-media-item-headline .yt-core-attributed-string',
    'h3.media-item-headline', '.details .yt-core-attributed-string'
  ].join(',');

  const WATCH_TITLE_SELECTORS = [
    'h1.ytd-watch-metadata yt-formatted-string',
    '.slim-video-information-title .yt-core-attributed-string',
    'h2.slim-video-information-title'
  ].join(',');

  function isVideoId(id) {
    return (typeof id === 'string') && /^[A-Za-z0-9_-]{11}$/.test(id);
  }

  function videoIdFromHref(href) {
    try {
      const parsed = new URL(href, location.href);
      const direct = parsed.searchParams.get('v');
      if (isVideoId(direct)) return direct;
      const parts = parsed.pathname.split('/').filter(Boolean);
      const idx = parts.findIndex((p) => ['shorts', 'embed', 'v'].includes(p));
      if ((idx !== -1) && isVideoId(parts[idx + 1])) return parts[idx + 1];
    } catch (err) { /* ignore */ }
    return null;
  }

  async function sha256Prefix(value) {
    const digest = await window.crypto.subtle.digest('SHA-256', new TextEncoder().encode(value));
    const bytes = new Uint8Array(digest);
    const hex = '0123456789abcdef';
    let result = '';
    for (let i = 0; i < bytes.length; i++) {
      result += hex[(bytes[i] >> 4) & 15] + hex[bytes[i] & 15];
    }
    return result.substring(0, 4);
  }

  // DeArrow titles use '>' to force original word casing; strip the markers.
  function formatTitle(title) {
    return title.replace(/(^|\s)>(\S)/g, '$1$2').trim();
  }

  function extractBranding(entry) {
    const result = {title: null, thumbTime: null};
    if (!entry) return result;

    const t = Array.isArray(entry.titles) ? entry.titles[0] : null;
    if (t && t.title && !t.original && (t.locked || (t.votes >= 0))) {
      result.title = formatTitle(t.title);
    }

    const th = Array.isArray(entry.thumbnails) ? entry.thumbnails[0] : null;
    if (th && !th.original && (th.locked || (th.votes >= 0)) &&
        Number.isFinite(th.timestamp)) {
      result.thumbTime = th.timestamp;
    }

    return result;
  }

  async function loadBranding(videoId) {
    if (state.cache.has(videoId)) return;
    state.cache.set(videoId, 'pending');

    try {
      const prefix = await sha256Prefix(videoId);
      const response = await fetch(state.config.endpoint + '/' + prefix + '?service=YouTube',
          {credentials: 'omit'});

      if (response.ok) {
        const data = await response.json();
        state.cache.set(videoId, extractBranding(data && data[videoId]));
      } else if (response.status === 404) {
        state.cache.set(videoId, {title: null, thumbTime: null});
      } else {
        state.cache.delete(videoId); // retry later
      }
    } catch (err) {
      state.cache.delete(videoId);
    }
  }

  function replaceTitle(el, branding, videoId) {
    if (!state.config.titles || (branding.title == null)) return;
    if (el.getAttribute('data-fermata-dearrow') === videoId) return;
    if (el.getAttribute('data-fermata-original') == null) {
      el.setAttribute('data-fermata-original', el.textContent);
    }
    el.textContent = branding.title;
    if (el.hasAttribute('title')) el.setAttribute('title', branding.title);
    el.setAttribute('data-fermata-dearrow', videoId);
  }

  function replaceThumb(img, branding, videoId) {
    if (!state.config.thumbnails || (branding.thumbTime == null)) return;
    if (img.getAttribute('data-fermata-dearrow') === videoId) return;
    if (img.getAttribute('data-fermata-original') == null) {
      img.setAttribute('data-fermata-original', img.src);
    }
    img.src = state.config.thumbEndpoint + '?videoID=' + videoId +
        '&time=' + branding.thumbTime;
    img.removeAttribute('srcset');
    img.setAttribute('data-fermata-dearrow', videoId);
  }

  function processItem(anchor, videoId) {
    const branding = state.cache.get(videoId);
    if ((branding == null) || (branding === 'pending')) {
      loadBranding(videoId);
      return;
    }

    const container = anchor.closest(
        'ytd-rich-item-renderer, ytd-video-renderer, ytd-compact-video-renderer, ' +
        'ytd-grid-video-renderer, ytm-media-item, ytm-compact-video-renderer, ' +
        'ytm-video-with-context-renderer, ytm-rich-item-renderer, yt-lockup-view-model') || anchor;

    for (const el of container.querySelectorAll(TITLE_SELECTORS)) {
      replaceTitle(el, branding, videoId);
    }
    for (const img of container.querySelectorAll('img')) {
      const src = img.getAttribute('data-fermata-original') || img.src || '';
      if (src.indexOf('/vi/' + videoId + '/') !== -1 ||
          src.indexOf('/vi_webp/' + videoId + '/') !== -1) {
        replaceThumb(img, branding, videoId);
      }
    }
  }

  function processWatchTitle() {
    if (!state.config.titles) return;
    const videoId = videoIdFromHref(location.href);
    if (videoId == null) return;

    const branding = state.cache.get(videoId);
    if ((branding == null) || (branding === 'pending')) {
      loadBranding(videoId);
      return;
    }
    if (branding.title == null) return;

    for (const el of document.querySelectorAll(WATCH_TITLE_SELECTORS)) {
      replaceTitle(el, branding, videoId);
    }
  }

  function restoreAll() {
    for (const el of document.querySelectorAll('[data-fermata-dearrow]')) {
      const original = el.getAttribute('data-fermata-original');
      if (original != null) {
        if (el.tagName === 'IMG') el.src = original;
        else el.textContent = original;
      }
      el.removeAttribute('data-fermata-dearrow');
      el.removeAttribute('data-fermata-original');
    }
  }

  function tick() {
    if (!state.config.enabled) return;

    for (const anchor of document.querySelectorAll(
        'a[href*="watch?v="], a[href^="/shorts/"]')) {
      const videoId = videoIdFromHref(anchor.getAttribute('href'));
      if (videoId == null) continue;
      processItem(anchor, videoId);
    }

    processWatchTitle();
  }

  window.FermataDeArrow = {
    configure(config) {
      state.config = Object.assign({}, state.config, config);

      if (state.config.enabled) {
        if (state.intervalId == null) state.intervalId = setInterval(tick, 1000);
        tick();
      } else {
        if (state.intervalId != null) {
          clearInterval(state.intervalId);
          state.intervalId = null;
        }
        restoreAll();
      }
    }
  };
})();
