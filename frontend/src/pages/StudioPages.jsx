import { useEffect, useState } from "react";
import { Link, useNavigate, useParams, useSearchParams } from "react-router-dom";
import { mediaUrl, request, useApi } from "../api.js";
import { useAuth } from "../auth.jsx";
import { ArtworkGrid, EmptyState, PageState } from "../components.jsx";
import { AUCTION_DURATIONS, formatDate, formatPrice } from "../format.js";
import { ExhibitionCard } from "./ExhibitionPages.jsx";

const TABS = [
  { id: "artworks", label: "My artworks" },
  { id: "sales", label: "Sales" },
  { id: "exhibitions", label: "My exhibitions" },
];

export function StudioPage() {
  const { user } = useAuth();
  const [params, setParams] = useSearchParams();
  const tab = params.get("tab") ?? "artworks";
  const artworks = useApi("/api/me/artworks");
  const sales = useApi("/api/me/sales");
  const exhibitions = useApi(`/api/artists/${user.id}`);
  const [actionError, setActionError] = useState("");

  const pendingSales = sales.data?.filter((order) => order.status === "PAID").length ?? 0;
  const revenue = sales.data?.filter((order) => order.status === "PAID" || order.status === "COMPLETED")
    .reduce((sum, order) => sum + order.price, 0) ?? 0;

  async function deleteArtwork(artwork) {
    if (!window.confirm(`Delete "${artwork.title}"? This can't be undone.`)) return;
    setActionError("");
    try {
      await request(`/api/artworks/${artwork.id}`, { method: "DELETE" });
      artworks.setData((current) => current.filter((item) => item.id !== artwork.id));
    } catch (requestError) {
      setActionError(requestError.message);
    }
  }

  async function completeSale(order) {
    setActionError("");
    try {
      const updated = await request(`/api/orders/${order.id}/complete`, { method: "POST" });
      sales.setData((current) => current.map((item) => (item.id === updated.id ? updated : item)));
    } catch (requestError) {
      setActionError(requestError.message);
    }
  }

  return (
    <section className="container page-section">
      <div className="section-header">
        <div>
          <h2 className="mb-1">Welcome to your studio, {user.displayName}</h2>
          <p className="lead-muted mb-0">Manage your listings, sales and exhibitions.</p>
        </div>
        <div className="d-flex flex-wrap gap-2">
          <Link to="/studio/upload" className="btn btn-primary">Upload artwork</Link>
          <Link to="/exhibitions/new" className="btn btn-outline-dark">New exhibition</Link>
        </div>
      </div>

      <div className="stat-row">
        <Stat label="Artworks" value={artworks.data?.length ?? "–"} />
        <Stat label="For sale" value={artworks.data?.filter((a) => a.status === "AVAILABLE").length ?? "–"} />
        <Stat label="Paid, to deliver" value={sales.data ? pendingSales : "–"} />
        <Stat label="Revenue received" value={sales.data ? formatPrice(revenue) : "–"} />
      </div>

      <div className="filter-buttons justify-content-start" role="tablist">
        {TABS.map((item) => (
          <button key={item.id} type="button" role="tab" aria-selected={tab === item.id}
            className={`btn ${tab === item.id ? "btn-primary" : "btn-outline-dark"}`}
            onClick={() => setParams({ tab: item.id })}>
            {item.label}{item.id === "sales" && pendingSales > 0 ? ` (${pendingSales})` : ""}
          </button>
        ))}
      </div>
      {actionError && <p className="text-danger" role="alert">{actionError}</p>}

      {tab === "artworks" && (
        <PageState loading={artworks.loading} error={artworks.error}>
          <ArtworkGrid
            artworks={artworks.data}
            empty="You haven't uploaded any artwork yet."
            actions={(artwork) => (
              <>
                {artwork.status === "AVAILABLE" && <Link to={`/studio/artworks/${artwork.id}/edit`} className="btn btn-outline-dark btn-sm">Edit</Link>}
                <button type="button" className="btn btn-danger btn-sm" onClick={() => deleteArtwork(artwork)}>Delete</button>
              </>
            )}
          />
        </PageState>
      )}

      {tab === "sales" && (
        <PageState loading={sales.loading} error={sales.error}>
          {sales.data?.length ? (
            <div className="table-responsive glass-panel">
              <table className="table table-hover align-middle mb-0">
                <thead><tr><th>Artwork</th><th>Buyer</th><th>Price</th><th>Type</th><th>Date</th><th>Status</th><th /></tr></thead>
                <tbody>
                  {sales.data.map((order) => (
                    <tr key={order.id}>
                      <td>{order.artworkId ? <Link to={`/artworks/${order.artworkId}`}>{order.artworkTitle}</Link> : order.artworkTitle}</td>
                      <td>
                        {order.buyerName}
                        {order.shippingAddress && order.status !== "CANCELLED" && (
                          <small className="d-block text-muted">Ship to {order.shippingAddress.city}, {order.shippingAddress.state} {order.shippingAddress.postalCode}</small>
                        )}
                      </td>
                      <td>{formatPrice(order.price)}</td>
                      <td>{order.source === "AUCTION" ? "Auction" : "Purchase"}</td>
                      <td>{formatDate(order.createdAt)}</td>
                      <td><OrderStatus status={order.status} /></td>
                      <td className="text-end text-nowrap">
                        {order.invoiceNumber && (
                          <Link to={`/checkouts/${order.checkoutId}/invoice`} className="btn btn-outline-dark btn-sm me-1">Invoice</Link>
                        )}
                        {order.status === "PAID" && (
                          <button type="button" className="btn btn-success btn-sm" onClick={() => completeSale(order)}>Mark delivered</button>
                        )}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          ) : <EmptyState>No sales yet.</EmptyState>}
        </PageState>
      )}

      {tab === "exhibitions" && (
        <PageState loading={exhibitions.loading} error={exhibitions.error}>
          {exhibitions.data?.exhibitions.length
            ? <div className="exhibition-grid">{exhibitions.data.exhibitions.map((e) => <ExhibitionCard key={e.id} exhibition={e} />)}</div>
            : <EmptyState>You haven't curated any exhibitions yet.</EmptyState>}
        </PageState>
      )}
    </section>
  );
}

function Stat({ label, value }) {
  return (
    <div className="stat">
      <span className="stat-value">{value}</span>
      <span className="stat-label">{label}</span>
    </div>
  );
}

const ORDER_STATUS = {
  AWAITING_PAYMENT: { label: "Awaiting payment", style: "bg-warning text-dark" },
  PAID: { label: "Paid", style: "bg-info text-dark" },
  COMPLETED: { label: "Delivered", style: "bg-success" },
  CANCELLED: { label: "Cancelled", style: "bg-secondary" },
};

export function OrderStatus({ status }) {
  const { label, style } = ORDER_STATUS[status] ?? { label: status, style: "bg-secondary" };
  return <span className={`badge ${style}`}>{label}</span>;
}

const EMPTY_FORM = {
  title: "", description: "", category: "", price: "", tags: "", medium: "", dimensions: "", year: "",
  saleType: "FIXED", auctionDurationHours: "72",
};

/** Upload a new artwork, or edit one when the route has an :id. */
export function ArtworkFormPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const categories = useApi("/api/categories");
  const existing = useApi(id ? `/api/artworks/${encodeURIComponent(id)}` : null);
  const [form, setForm] = useState(EMPTY_FORM);
  const [image, setImage] = useState(null);
  const [preview, setPreview] = useState("");
  const [error, setError] = useState("");
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    const artwork = existing.data;
    if (!artwork) return;
    setForm({
      title: artwork.title, description: artwork.description, category: artwork.category,
      price: String(artwork.price), tags: artwork.tags.join(", "), medium: artwork.medium ?? "",
      dimensions: artwork.dimensions ?? "", year: artwork.year ? String(artwork.year) : "",
      saleType: artwork.saleType, auctionDurationHours: "",
    });
  }, [existing.data]);

  useEffect(() => {
    if (!image) return undefined;
    const url = URL.createObjectURL(image);
    setPreview(url);
    return () => URL.revokeObjectURL(url);
  }, [image]);

  const change = (field) => (event) => setForm((current) => ({ ...current, [field]: event.target.value }));
  const biddingStarted = existing.data?.bidCount > 0;

  async function submit(event) {
    event.preventDefault();
    setSaving(true);
    setError("");
    try {
      let saved;
      if (id) {
        saved = await request(`/api/artworks/${id}`, {
          method: "PUT",
          json: {
            title: form.title, description: form.description, category: form.category, price: Number(form.price),
            tags: form.tags, medium: form.medium, dimensions: form.dimensions, year: form.year ? Number(form.year) : null,
          },
        });
      } else {
        const body = new FormData();
        Object.entries(form).forEach(([key, value]) => {
          if (key === "auctionDurationHours" && form.saleType !== "AUCTION") return;
          if (value !== "") body.append(key, value);
        });
        body.append("image", image);
        saved = await request("/api/artworks", { method: "POST", body });
      }
      navigate(`/artworks/${saved.id}`);
    } catch (requestError) {
      setError(requestError.message);
    } finally {
      setSaving(false);
    }
  }

  return (
    <section className="container page-section">
      <h2>{id ? "Edit artwork" : "Upload new artwork"}</h2>
      <PageState loading={existing.loading} error={existing.error}>
        <form className="glass-panel form-panel" onSubmit={submit}>
          <div className="row g-4">
            <div className="col-lg-7">
              <label className="form-label" htmlFor="title">Title</label>
              <input className="form-control mb-3" id="title" maxLength={120} required value={form.title} onChange={change("title")} />

              <label className="form-label" htmlFor="description">Description</label>
              <textarea className="form-control mb-3" id="description" rows="4" maxLength={2000} required value={form.description} onChange={change("description")} />

              <div className="row g-3 mb-3">
                <div className="col-sm-6">
                  <label className="form-label" htmlFor="category">Category</label>
                  <select className="form-select" id="category" required value={form.category} onChange={change("category")}>
                    <option value="" disabled>Choose a category</option>
                    {(categories.data ?? []).map((category) => <option key={category} value={category}>{category}</option>)}
                  </select>
                </div>
                <div className="col-sm-6">
                  <label className="form-label" htmlFor="tags">Tags <small>(comma separated)</small></label>
                  <input className="form-control" id="tags" placeholder="landscape, blue, oil" value={form.tags} onChange={change("tags")} />
                </div>
                <div className="col-sm-4">
                  <label className="form-label" htmlFor="medium">Medium</label>
                  <input className="form-control" id="medium" placeholder="Oil on canvas" maxLength={100} value={form.medium} onChange={change("medium")} />
                </div>
                <div className="col-sm-4">
                  <label className="form-label" htmlFor="dimensions">Dimensions</label>
                  <input className="form-control" id="dimensions" placeholder="60 × 90 cm" maxLength={100} value={form.dimensions} onChange={change("dimensions")} />
                </div>
                <div className="col-sm-4">
                  <label className="form-label" htmlFor="year">Year</label>
                  <input className="form-control" id="year" type="number" min="1000" max="2100" value={form.year} onChange={change("year")} />
                </div>
              </div>

              {!id && (
                <fieldset className="mb-3">
                  <legend className="form-label">How do you want to sell it?</legend>
                  <div className="sale-type-options">
                    <label className={form.saleType === "FIXED" ? "active" : ""}>
                      <input type="radio" name="saleType" value="FIXED" checked={form.saleType === "FIXED"} onChange={change("saleType")} />
                      <strong>Fixed price</strong><span>Buyers purchase instantly.</span>
                    </label>
                    <label className={form.saleType === "AUCTION" ? "active" : ""}>
                      <input type="radio" name="saleType" value="AUCTION" checked={form.saleType === "AUCTION"} onChange={change("saleType")} />
                      <strong>Auction</strong><span>Highest bid when time runs out wins.</span>
                    </label>
                  </div>
                </fieldset>
              )}

              <div className="row g-3">
                <div className="col-sm-6">
                  <label className="form-label" htmlFor="price">{form.saleType === "AUCTION" ? "Starting bid (₹)" : "Price (₹)"}</label>
                  <input className="form-control" id="price" type="number" min="1" step="0.01" required disabled={biddingStarted} value={form.price} onChange={change("price")} />
                  {biddingStarted && <small>The starting bid is locked once bidding starts.</small>}
                </div>
                {!id && form.saleType === "AUCTION" && (
                  <div className="col-sm-6">
                    <label className="form-label" htmlFor="duration">Auction length</label>
                    <select className="form-select" id="duration" value={form.auctionDurationHours} onChange={change("auctionDurationHours")}>
                      {AUCTION_DURATIONS.map((option) => <option key={option.hours} value={option.hours}>{option.label}</option>)}
                    </select>
                  </div>
                )}
              </div>
            </div>

            <div className="col-lg-5">
              {id ? (
                <>
                  <p className="form-label">Image</p>
                  <img className="upload-preview" src={mediaUrl(existing.data?.imageUrl)} alt="" />
                </>
              ) : (
                <>
                  <label className="form-label" htmlFor="image">Artwork image <small>(max 10 MB)</small></label>
                  <input className="form-control mb-3" id="image" type="file" accept="image/*" required onChange={(event) => setImage(event.target.files[0] ?? null)} />
                  {preview ? <img className="upload-preview" src={preview} alt="Preview of the selected image" /> : <div className="upload-placeholder">Image preview</div>}
                </>
              )}
            </div>
          </div>

          {error && <p className="text-danger mt-3" role="alert">{error}</p>}
          <div className="d-flex gap-2 mt-4">
            <button className="btn btn-primary" type="submit" disabled={saving}>
              {saving ? (id ? "Saving..." : "Uploading...") : (id ? "Save changes" : "Publish artwork")}
            </button>
            <Link to="/studio" className="btn btn-outline-dark">Cancel</Link>
          </div>
        </form>
      </PageState>
    </section>
  );
}
