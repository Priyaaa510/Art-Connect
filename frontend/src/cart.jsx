import { createContext, useCallback, useContext, useEffect, useMemo, useState } from "react";
import { request } from "./api.js";
import { useAuth } from "./auth.jsx";

const CartContext = createContext(null);

const EMPTY_CART = { items: [], total: 0, availableCount: 0 };

/** The signed-in user's cart, shared by the header badge, artwork pages and the cart page. */
export function CartProvider({ children }) {
  const { user } = useAuth();
  const [cart, setCart] = useState(EMPTY_CART);
  const [loading, setLoading] = useState(true);
  const userId = user?.id;

  const reload = useCallback(async () => {
    if (!userId) {
      setCart(EMPTY_CART);
      setLoading(false);
      return EMPTY_CART;
    }
    setLoading(true);
    try {
      const result = await request("/api/cart");
      setCart(result);
      return result;
    } catch {
      return null; // keep showing what we had; the cart page shows its own errors
    } finally {
      setLoading(false);
    }
  }, [userId]);

  useEffect(() => {
    reload();
  }, [reload]);

  const add = useCallback(async (artworkId) => {
    const result = await request("/api/cart/items", { method: "POST", json: { artworkId } });
    setCart(result);
    return result;
  }, []);

  const remove = useCallback(async (artworkId) => {
    const result = await request(`/api/cart/items/${artworkId}`, { method: "DELETE" });
    setCart(result);
    return result;
  }, []);

  const value = useMemo(() => ({
    cart,
    loading,
    count: cart.items.length,
    contains: (artworkId) => cart.items.some((item) => item.artwork.id === artworkId),
    add,
    remove,
    reload,
  }), [cart, loading, add, remove, reload]);

  return <CartContext.Provider value={value}>{children}</CartContext.Provider>;
}

export function useCart() {
  return useContext(CartContext);
}
