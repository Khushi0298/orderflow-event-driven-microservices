import { useEffect, useMemo, useState } from "react";

const ORDER_API = "http://localhost:8081/api/orders";
const PRODUCT_API = "http://localhost:8082/api/products";

const ICON = { laptop: "💻", phone: "📱", earbuds: "🎧", keyboard: "⌨️" };
const STAGES = ["Order placed", "Stock reserved", "Payment", "Confirmed"];
const FILTERS = ["All", "In progress", "Confirmed", "Cancelled"];

const rupees = (n) => "₹" + Number(n).toLocaleString("en-IN");
const clock = () => new Date().toLocaleTimeString("en-IN", { hour12: false });
const paymentFailed = (o) => o.reason?.startsWith("Payment");

// Which saga step each stage of the pipeline is in.
function stepStates(o) {
  switch (o.status) {
    case "PENDING": return ["done", "active", "idle", "idle"];
    case "STOCK_RESERVED": return ["done", "done", "active", "idle"];
    case "CONFIRMED": return ["done", "done", "done", "done"];
    default: return paymentFailed(o) ? ["done", "done", "failed", "idle"] : ["done", "failed", "idle", "idle"];
  }
}

function describe(o) {
  const id = o.id.slice(0, 8);
  switch (o.status) {
    case "PENDING": return { tone: "info", text: `${id} placed. OrderCreated published to Kafka.` };
    case "STOCK_RESERVED": return { tone: "info", text: `${id} stock reserved. Charging payment.` };
    case "CONFIRMED": return { tone: "ok", text: `${id} payment completed. Order confirmed.` };
    default:
      return paymentFailed(o)
        ? { tone: "bad", text: `${id} payment declined. Stock released (compensation).` }
        : { tone: "bad", text: `${id} rejected by inventory: ${o.reason}.` };
  }
}

function Pipeline({ order }) {
  const states = stepStates(order);
  return (
    <ol className="pipeline" aria-label="Order progress">
      {STAGES.map((label, i) => (
        <li key={label} className={states[i]}>
          <span className="dot" aria-hidden="true" />
          <span className="step-label">{label}</span>
        </li>
      ))}
    </ol>
  );
}

export default function App() {
  const [products, setProducts] = useState([]);
  const [orders, setOrders] = useState([]);
  const [qty, setQty] = useState({});
  const [feed, setFeed] = useState([]);
  const [filter, setFilter] = useState("All");
  const [live, setLive] = useState(false);
  const [error, setError] = useState("");

  const loadProducts = () =>
    fetch(PRODUCT_API).then((r) => r.json()).then(setProducts)
      .catch(() => setError("Inventory service is not reachable on port 8082."));

  useEffect(() => {
    loadProducts();
    fetch(ORDER_API).then((r) => r.json()).then(setOrders)
      .catch(() => setError("Order service is not reachable on port 8081."));

    const stream = new EventSource(ORDER_API + "/stream");
    stream.onopen = () => setLive(true);
    stream.onerror = () => setLive(false);
    stream.addEventListener("order", (e) => {
      const o = JSON.parse(e.data);
      setOrders((prev) => (prev.some((x) => x.id === o.id) ? prev.map((x) => (x.id === o.id ? o : x)) : [o, ...prev]));
      setFeed((prev) => [{ key: Math.random(), time: clock(), ...describe(o) }, ...prev].slice(0, 25));
      loadProducts();
      setTimeout(loadProducts, 1000);
    });
    return () => stream.close();
  }, []);

  const place = async (p, quantity) => {
    setError("");
    try {
      const res = await fetch(ORDER_API, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ productId: p.id, quantity, amount: p.price * quantity }),
      });
      if (!res.ok) throw new Error();
    } catch {
      setError("Could not place the order. Check that the order service is running.");
    }
  };

  const step = (id, d) => setQty((q) => ({ ...q, [id]: Math.min(99, Math.max(1, (q[id] ?? 1) + d)) }));

  const stats = useMemo(() => {
    const confirmed = orders.filter((o) => o.status === "CONFIRMED");
    const cancelled = orders.filter((o) => o.status === "CANCELLED");
    return {
      total: orders.length,
      inProgress: orders.length - confirmed.length - cancelled.length,
      confirmed: confirmed.length,
      cancelled: cancelled.length,
      revenue: confirmed.reduce((s, o) => s + Number(o.amount), 0),
    };
  }, [orders]);

  const visible = orders.filter((o) =>
    filter === "All" ? true
    : filter === "Confirmed" ? o.status === "CONFIRMED"
    : filter === "Cancelled" ? o.status === "CANCELLED"
    : o.status === "PENDING" || o.status === "STOCK_RESERVED");

  const nameOf = (id) => products.find((p) => p.id === id)?.name ?? id;
  const laptop = products.find((p) => p.id === "laptop");

  return (
    <div className="app">
      <header className="top">
        <div>
          <h1>OrderFlow</h1>
          <p>Orders move through inventory and payment as Kafka events. Every change on this page is pushed live from the services.</p>
        </div>
        <span className={"live " + (live ? "on" : "off")} role="status">{live ? "Live" : "Reconnecting"}</span>
      </header>

      {error && <div className="error" role="alert">{error}</div>}

      <section className="stats" aria-label="Order statistics">
        <div><b>{stats.total}</b><span>Total orders</span></div>
        <div><b>{stats.inProgress}</b><span>In progress</span></div>
        <div className="ok"><b>{stats.confirmed}</b><span>Confirmed</span></div>
        <div className="bad"><b>{stats.cancelled}</b><span>Cancelled</span></div>
        <div><b>{rupees(stats.revenue)}</b><span>Confirmed revenue</span></div>
      </section>

      <div className="grid">
        <section>
          <div className="section-head">
            <h2>Products</h2>
            {laptop && (
              <button className="ghost" onClick={() => place(laptop, 2)}>
                Demo: failed payment (2 laptops)
              </button>
            )}
          </div>
          <ul className="products">
            {products.map((p) => (
              <li key={p.id}>
                <div className="icon" aria-hidden="true">{ICON[p.id] ?? "📦"}</div>
                <div className="info">
                  <strong>{p.name}</strong>
                  <span className="price">{rupees(p.price)}</span>
                  <span className={"stock" + (p.stock === 0 ? " out" : p.stock <= 3 ? " low" : "")}>
                    {p.stock === 0 ? "Out of stock" : p.stock <= 3 ? `Only ${p.stock} left` : `${p.stock} in stock`}
                  </span>
                </div>
                <div className="buy">
                  <div className="stepper">
                    <button aria-label={"Decrease quantity of " + p.name} onClick={() => step(p.id, -1)}>−</button>
                    <output>{qty[p.id] ?? 1}</output>
                    <button aria-label={"Increase quantity of " + p.name} onClick={() => step(p.id, 1)}>+</button>
                  </div>
                  <button className="primary" onClick={() => place(p, qty[p.id] ?? 1)}>Place order</button>
                </div>
              </li>
            ))}
          </ul>
        </section>

        <aside>
          <h2>Event stream</h2>
          <ul className="feed" aria-live="polite">
            {feed.length === 0 && <li className="empty">Waiting for events. Place an order to see Kafka messages arrive.</li>}
            {feed.map((f) => (
              <li key={f.key} className={f.tone}>
                <time>{f.time}</time>
                <span>{f.text}</span>
              </li>
            ))}
          </ul>
        </aside>
      </div>

      <section>
        <div className="section-head">
          <h2>Orders</h2>
          <div className="tabs" role="tablist">
            {FILTERS.map((f) => (
              <button key={f} role="tab" aria-selected={filter === f} className={filter === f ? "on" : ""} onClick={() => setFilter(f)}>
                {f}
              </button>
            ))}
          </div>
        </div>
        {visible.length === 0 ? (
          <p className="empty">No orders here yet. Place one from the products above.</p>
        ) : (
          <ul className="orders">
            {visible.map((o) => (
              <li key={o.id}>
                <div className="meta">
                  <span className="id">#{o.id.slice(0, 8)}</span>
                  <strong>{nameOf(o.productId)} × {o.quantity}</strong>
                  <span className="price">{rupees(o.amount)}</span>
                </div>
                <Pipeline order={o} />
                {o.reason && <p className="reason">{o.reason}</p>}
              </li>
            ))}
          </ul>
        )}
      </section>
    </div>
  );
}
