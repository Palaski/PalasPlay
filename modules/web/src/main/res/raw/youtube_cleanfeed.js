(function() {
  if (window.FermataCleanFeed) return;

  const SHORTS_CSS = [
    // Shelves / sections
    'ytd-reel-shelf-renderer', 'ytm-reel-shelf-renderer',
    'ytd-rich-shelf-renderer[is-shorts]', 'grid-shelf-view-model',
    'ytd-rich-section-renderer:has(ytd-rich-shelf-renderer[is-shorts])',
    // Individual items
    'ytm-shorts-lockup-view-model', 'ytm-shorts-lockup-view-model-v2',
    'ytd-rich-item-renderer:has(a[href^="/shorts/"])',
    'ytm-rich-item-renderer:has(a[href^="/shorts/"])',
    'ytd-video-renderer:has(a[href^="/shorts/"])',
    'ytm-video-with-context-renderer:has(a[href^="/shorts/"])',
    'ytm-item-section-renderer:has(> lazy-list > ytm-reel-shelf-renderer)',
    // Shorts tab in the mobile bottom bar
    'ytm-pivot-bar-item-renderer:has(.pivot-shorts)'
  ].join(',') + '{display:none !important}';

  const POSTS_CSS = [
    // Community posts, polls, quizzes
    'ytd-post-renderer', 'ytd-shared-post-renderer', 'ytd-poll-renderer',
    'ytm-post-renderer', 'ytm-backstage-post-renderer',
    'ytm-shared-post-renderer', 'ytm-poll-renderer',
    // Containers wrapping them in the feed
    'ytd-rich-item-renderer:has(ytd-post-renderer)',
    'ytd-rich-item-renderer:has(ytd-shared-post-renderer)',
    'ytd-rich-section-renderer:has(ytd-post-renderer)',
    'ytd-rich-section-renderer:has(ytd-shared-post-renderer)',
    'ytm-rich-item-renderer:has(ytm-post-renderer)',
    'ytm-rich-item-renderer:has(ytm-backstage-post-renderer)',
    'ytm-rich-section-renderer:has(ytm-post-renderer)',
    'ytm-rich-section-renderer:has(ytm-backstage-post-renderer)',
    'ytm-item-section-renderer:has(> lazy-list > ytm-post-renderer)',
    'ytm-item-section-renderer:has(> lazy-list > ytm-backstage-post-renderer)'
  ].join(',') + '{display:none !important}';

  const state = {styles: {}};

  function setCss(key, css, enabled) {
    let el = state.styles[key];
    if (enabled) {
      if (el != null) return;
      el = document.createElement('style');
      el.id = 'fermata-cleanfeed-' + key;
      el.textContent = css;
      (document.head || document.documentElement).appendChild(el);
      state.styles[key] = el;
    } else if (el != null) {
      el.remove();
      delete state.styles[key];
    }
  }

  window.FermataCleanFeed = {
    configure(config) {
      setCss('shorts', SHORTS_CSS, !!config.shorts);
      setCss('posts', POSTS_CSS, !!config.posts);
    }
  };
})();
