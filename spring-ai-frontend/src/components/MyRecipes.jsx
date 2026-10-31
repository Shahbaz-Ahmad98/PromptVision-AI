
import { useCallback, useEffect, useState } from "react";

export default function MyRecipes({ token }) {
  const [recipes, setRecipes] = useState([]);
  const [selected, setSelected] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  const loadRecipes = useCallback(async () => {
    const response = await fetch("/api/recipes", {
      headers: {
        Authorization: `Bearer ${token}`,
      },
    });

    if (!response.ok) {
      throw new Error(`Failed to load recipes: ${response.status}`);
    }

    return response.json();
  }, [token]);

  useEffect(() => {
    let active = true;

    loadRecipes()
      .then((data) => {
        if (active) setRecipes(data);
      })
      .catch((err) => {
        if (active) setError(err.message);
      })
      .finally(() => {
        if (active) setLoading(false);
      });

    return () => {
      active = false;
    };
  }, [loadRecipes]);

  async function deleteRecipe(id) {
    if (!window.confirm("Delete this saved recipe?")) return;

    setError("");

    try {
      const response = await fetch(`/api/recipes/${id}`, {
        method: "DELETE",
        headers: {
          Authorization: `Bearer ${token}`,
        },
      });

      if (!response.ok) {
        throw new Error(`Delete failed: ${response.status}`);
      }

      setRecipes((previous) =>
        previous.filter((recipe) => recipe.id !== id)
      );

      if (selected?.id === id) {
        setSelected(null);
      }
    } catch (err) {
      setError(err.message);
    }
  }

  return (
    <section>
      <h2>My Saved Recipes</h2>
      <p>Revisit your favorite AI-generated recipes.</p>

      {error && (
        <div className="error" role="alert">
          {error}
        </div>
      )}

      {loading && <p>Loading recipes...</p>}

      {!loading && recipes.length === 0 && (
        <p>You haven't saved any recipes yet.</p>
      )}

      {!loading && recipes.length > 0 && (
        <div className="image-grid">
          {recipes.map((item) => (
            <div className="image-card" key={item.id}>
              <h3>{item.title}</h3>
              <p>{item.cuisine}</p>
              <p>
                {new Date(item.createdAt).toLocaleDateString()}
              </p>

              <button
                type="button"
                onClick={() => setSelected(item)}
              >
                View Recipe
              </button>

              <button
                type="button"
                onClick={() => deleteRecipe(item.id)}
              >
                Delete
              </button>
            </div>
          ))}
        </div>
      )}

      {selected && (
        <div className="result">
          <h3>{selected.title}</h3>

          <p><strong>Ingredients:</strong> {selected.ingredients}</p>
          <p><strong>Cuisine:</strong> {selected.cuisine}</p>
          <p>
            <strong>Dietary restrictions:</strong>{" "}
            {selected.dietaryRestrictions}
          </p>

          <div style={{ whiteSpace: "pre-wrap" }}>
            {selected.content}
          </div>

          <button
            type="button"
            onClick={() => setSelected(null)}
          >
            Close Recipe
          </button>
        </div>
      )}
    </section>
  );
}
