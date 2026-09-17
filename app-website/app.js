/* Fills the download section (and the hero button) from releases.json, which the
   Pages workflow regenerates from the GitHub Releases API at deploy time.
   Everything degrades to the plain GitHub releases page if it's unavailable. */
(() => {
  "use strict";

  const REPO = "artchsh/kimep-mobile-clients";
  const RELEASES_URL = `https://github.com/${REPO}/releases`;
  const LATEST_URL = `${RELEASES_URL}/latest`;

  const heroBtn = document.getElementById("download-apk");
  const latest = document.getElementById("latest");
  const list = document.getElementById("release-list");

  const esc = (s) =>
    String(s).replace(/[&<>"']/g, (c) => ({
      "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;",
    })[c]);

  const size = (n) =>
    typeof n === "number" ? `${(n / 1024 / 1024).toFixed(1)} MB` : null;

  const date = (iso) =>
    iso ? new Date(iso).toLocaleDateString(undefined, {
      year: "numeric", month: "short", day: "numeric",
    }) : null;

  const join = (...parts) => parts.filter(Boolean).join(" · ");

  function renderLatest(release) {
    latest.innerHTML = `
      <div>
        <p class="latest__title">${esc(release.tag)}</p>
        <p class="meta">${esc(join(
          date(release.publishedAt),
          size(release.apkSize),
          release.prerelease ? "pre-release" : null,
          "Android 8.0+",
        ))}</p>
      </div>
      <a class="btn btn--primary" href="${esc(release.apkUrl)}" rel="noopener">Download APK</a>`;
  }

  function renderLatestFallback() {
    latest.innerHTML = `
      <div>
        <p class="latest__title">Releases</p>
        <p class="meta">hosted on GitHub</p>
      </div>
      <a class="btn btn--quiet" href="${RELEASES_URL}" rel="noopener">Open releases</a>`;
  }

  function renderList(releases) {
    if (!releases.length) {
      list.innerHTML = `<p class="meta">No releases published yet.</p>`;
      return;
    }
    list.innerHTML = releases.map((r) => {
      const meta = join(date(r.publishedAt), size(r.apkSize), r.prerelease ? "pre-release" : null);
      const action = r.apkUrl
        ? `<a href="${esc(r.apkUrl)}" rel="noopener">Download</a>`
        : `<a href="${esc(r.htmlUrl || RELEASES_URL)}" rel="noopener">Details</a>`;
      return `
        <div class="release-row">
          <span class="release-row__tag">${esc(r.tag)}</span>
          <span class="release-row__meta">${esc(meta)}</span>
          ${action}
        </div>`;
    }).join("");
  }

  fetch("releases.json", { cache: "no-store" })
    .then((r) => (r.ok ? r.json() : Promise.reject(new Error(String(r.status)))))
    .then((data) => {
      const releases = Array.isArray(data.releases) ? data.releases : [];
      const downloadable = releases.filter((r) => r.apkUrl);
      if (downloadable.length) {
        renderLatest(downloadable[0]);
        if (heroBtn) heroBtn.href = downloadable[0].apkUrl;
      } else {
        renderLatestFallback();
      }
      renderList(releases);
    })
    .catch(() => {
      renderLatestFallback();
      list.innerHTML = `<p class="meta">Could not load releases — <a href="${RELEASES_URL}" rel="noopener">open them on GitHub</a>.</p>`;
      if (heroBtn) heroBtn.href = LATEST_URL;
    });
})();
