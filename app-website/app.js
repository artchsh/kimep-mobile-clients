/* Populates the download card and release list from releases.json,
   which the Pages workflow regenerates from the GitHub Releases API at deploy
   time. Everything degrades to a plain link if the file is missing. */
(() => {
  "use strict";

  const REPO = "artchsh/kimep-mobile-clients";
  const RELEASES_URL = `https://github.com/${REPO}/releases`;

  const card = document.getElementById("download-card");
  const list = document.getElementById("release-list");

  const esc = (s) =>
    String(s).replace(/[&<>"']/g, (c) => ({
      "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;",
    })[c]);

  const bytes = (n) =>
    typeof n === "number" ? `${(n / 1024 / 1024).toFixed(1)} MB` : "—";

  const date = (iso) =>
    iso ? new Date(iso).toLocaleDateString(undefined, {
      year: "numeric", month: "short", day: "numeric",
    }) : "—";

  function fallbackCard() {
    card.innerHTML = `
      <p class="release__tag">Builds</p>
      <p class="release__name">Latest release</p>
      <p class="release__meta"><span>hosted on GitHub Releases</span></p>
      <a class="btn btn--primary" href="${RELEASES_URL}" rel="noopener">Open releases page</a>
      <p class="download__hint">Unofficial build — KIMEP University is not involved.</p>`;
  }

  function renderCard(release) {
    const pre = release.prerelease
      ? `<span class="badge-pre">pre-release</span>` : "";
    card.innerHTML = `
      <p class="release__tag">Latest build${pre}</p>
      <p class="release__name">${esc(release.name || release.tag)}</p>
      <p class="release__meta">
        <span>${esc(release.tag)}</span>
        <span>${date(release.publishedAt)}</span>
        <span>${bytes(release.apkSize)}</span>
        <span>Android 8.0+</span>
      </p>
      <a class="btn btn--primary" href="${esc(release.apkUrl)}" rel="noopener">Download APK</a>
      <p class="download__hint">
        Unofficial build, signed by an individual. KIMEP University has nothing to do with it.
      </p>`;
  }

  function renderList(releases) {
    if (!releases.length) {
      list.innerHTML = `<p class="skeleton">No releases published yet.</p>`;
      return;
    }
    list.innerHTML = releases.map((r) => `
      <div class="release-row">
        <div>
          <span class="release-row__tag">${esc(r.tag)}</span>
          ${r.prerelease ? `<span class="badge-pre">pre</span>` : ""}
        </div>
        <div class="release-row__meta">${date(r.publishedAt)} · ${bytes(r.apkSize)}</div>
        ${r.apkUrl
          ? `<a class="release-row__link" href="${esc(r.apkUrl)}" rel="noopener">Download</a>`
          : `<a class="release-row__link" href="${esc(r.htmlUrl || RELEASES_URL)}" rel="noopener">Details</a>`}
      </div>`).join("");
  }

  fetch("releases.json", { cache: "no-store" })
    .then((r) => (r.ok ? r.json() : Promise.reject(new Error(r.status))))
    .then((data) => {
      const releases = Array.isArray(data.releases) ? data.releases : [];
      const downloadable = releases.filter((r) => r.apkUrl);
      if (downloadable.length) renderCard(downloadable[0]);
      else fallbackCard();
      renderList(releases);
    })
    .catch(() => {
      fallbackCard();
      list.innerHTML = `<p class="skeleton">Could not load releases — <a href="${RELEASES_URL}" rel="noopener">open them on GitHub</a>.</p>`;
    });
})();
