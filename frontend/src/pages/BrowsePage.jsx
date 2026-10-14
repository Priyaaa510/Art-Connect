import { useEffect, useState } from "react";
import { useSearchParams } from "react-router-dom";
import { queryString, useApi } from "../api.js";
import { ArtworkGrid, PageState, Pagination } from "../components.jsx";
import { SORT_OPTIONS } from "../format.js";

/** The gallery, or with auctionsOnly the live-auction listing. Filters live in the URL so they can be shared. */
export default function BrowsePage({ auctionsOnly = false }) {
  const [params, setParams] = useSearchParams();
  const categories = useApi("/api/categories");
  const q = params.get("q") ?? "";
  const category = params.get("category") ?? "";
  const saleType = auctionsOnly ? "" : params.get("saleType") ?? "";
  const availableOnly = params.get("available") === "true";
  const sort = params.get("sort") ?? (auctionsOnly ? "ending_soon" : "newest");
  const page = Number(params.get("page") ?? 0);
  const [searchText, setSearchText] = useState(q);
  useEffect(() => setSearchText(q), [q]);

  const artworks = useApi(`/api/artworks${queryString({
    q, category, saleType, sort, page, size: 24,
    status: availableOnly ? "AVAILABLE" : "",
    liveAuctions: auctionsOnly,
  })}`);

  function update(changes) {
    const next = new URLSearchParams(params);
    Object.entries(changes).forEach(([key, value]) => {
      if (value === "" || value === false || value == null) next.delete(key);
      else next.set(key, value);
    });
    if (!("page" in changes)) next.delete("page");
    setParams(next);
  }

  return (
    <section className="container page-section">
      <h1 className="page-title">{auctionsOnly ? "Live auctions" : category || (q ? `Results for “${q}”` : "Original art for sale")}</h1>
      <p className="page-intro">
        {auctionsOnly
          ? "Place a bid before the clock runs out. Late bids extend the auction by two minutes."
          : "Browse original works by category, price and availability."}
      </p>

      <form className="filter-bar" role="search" onSubmit={(event) => { event.preventDefault(); update({ q: searchText.trim() }); }}>
        <input
          className="form-control"
          type="search"
          placeholder="Search titles, descriptions or artists"
          aria-label="Search artworks"
          value={searchText}
          onChange={(event) => setSearchText(event.target.value)}
        />
        {!auctionsOnly && (
          <select className="form-select" aria-label="Sale type" value={saleType} onChange={(event) => update({ saleType: event.target.value })}>
            <option value="">All listings</option>
            <option value="FIXED">Buy now</option>
            <option value="AUCTION">Auctions</option>
          </select>
        )}
        <select className="form-select" aria-label="Sort by" value={sort} onChange={(event) => update({ sort: event.target.value })}>
          {SORT_OPTIONS.filter((option) => auctionsOnly || option.value !== "ending_soon").map((option) => (
            <option key={option.value} value={option.value}>{option.label}</option>
          ))}
        </select>
        {!auctionsOnly && (
          <label className="form-check-label filter-check">
            <input type="checkbox" className="form-check-input me-2" checked={availableOnly} onChange={(event) => update({ available: event.target.checked })} />
            Available only
          </label>
        )}
        <button className="btn btn-primary" type="submit">Search</button>
      </form>

      <div className="filter-buttons" aria-label="Filter by category">
        <button type="button" className={`btn ${category === "" ? "btn-primary" : "btn-outline-dark"}`} onClick={() => update({ category: "" })}>All</button>
        {(categories.data ?? []).map((name) => (
          <button key={name} type="button" className={`btn ${category === name ? "btn-primary" : "btn-outline-dark"}`} onClick={() => update({ category: name })}>{name}</button>
        ))}
      </div>

      <PageState loading={artworks.loading && !artworks.data} error={artworks.error}>
        {artworks.data && (
          <>
            <p className="result-count">{artworks.data.totalItems} {artworks.data.totalItems === 1 ? "artwork" : "artworks"}</p>
            <ArtworkGrid
              artworks={artworks.data.items}
              empty={auctionsOnly ? "No live auctions match your filters." : "No artworks match your filters."}
            />
            <Pagination page={artworks.data.page} totalPages={artworks.data.totalPages} onChange={(next) => update({ page: next })} />
          </>
        )}
      </PageState>
    </section>
  );
}
