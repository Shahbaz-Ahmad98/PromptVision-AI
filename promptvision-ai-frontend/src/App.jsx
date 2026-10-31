
import { useState } from "react";
import AuthPanel from "./components/AuthPanel";
import ChatHistory from "./components/ChatHistory";
import ImageHistory from "./components/ImageHistory";
import MyRecipes from "./components/MyRecipes";
import "./App.css";

const API = "/api";

function App() {
  // ==========================================
  // AUTHENTICATION
  // ==========================================

  const [auth, setAuth] = useState(null);

  // ==========================================
  // NAVIGATION AND LOADING
  // ==========================================

  const [tab, setTab] = useState("chat");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  // ==========================================
  // IMAGE STATES
  // ==========================================

  const [imagePrompt, setImagePrompt] = useState("");
  const [quality, setQuality] = useState("standard");
  const [n, setN] = useState(1);
  const [images, setImages] = useState([]);

  // ==========================================
  // RECIPE STATES
  // ==========================================

  const [ingredients, setIngredients] = useState("");
  const [cuisine, setCuisine] = useState("Indian");
  const [diet, setDiet] = useState("None");
  const [recipe, setRecipe] = useState("");
  const [recipeTitle, setRecipeTitle] = useState("");
  const [recipeSaved, setRecipeSaved] = useState(false);

  // ==========================================
  // LOGIN
  // ==========================================

  function handleLogin(data) {
    setAuth(data);
    setTab("chat");
    setError("");
    setLoading(false);
  }

  // ==========================================
  // LOGOUT
  // ==========================================

  function handleLogout() {
    setAuth(null);
    setTab("chat");
    setLoading(false);
    setError("");

    setImagePrompt("");
    setQuality("standard");
    setN(1);
    setImages([]);

    setIngredients("");
    setCuisine("Indian");
    setDiet("None");
    setRecipe("");
    setRecipeTitle("");
    setRecipeSaved(false);
  }

  // ==========================================
  // API ERROR HANDLING
  // ==========================================

  async function handleApiError(response) {
    if (response.ok) return;

    if (response.status === 401) {
      throw new Error(
        "Your session has expired. Please log out and log in again."
      );
    }

    if (response.status === 403) {
      throw new Error(
        "You do not have permission to access this feature."
      );
    }

    if (response.status === 429) {
      throw new Error(
        "Gemini has reached its request limit. Please try again later."
      );
    }

    if (response.status === 503 || response.status === 502) {
      throw new Error(
        "The AI service is temporarily unavailable. Please try again shortly."
      );
    }

    if (response.status === 504) {
      throw new Error(
        "The AI service took too long to respond. Please try again."
      );
    }

    if (response.status === 400) {
      throw new Error(
        "Invalid request. Please check your inputs and try again."
      );
    }

    if (response.status >= 500) {
      throw new Error(
        "Something went wrong on the server. Please try again."
      );
    }

    throw new Error(`API error: ${response.status}`);
  }

  // ==========================================
  // JWT-PROTECTED GET REQUEST
  // ==========================================

  async function getResponse(endpoint, params = {}) {
    if (!auth?.token) {
      throw new Error("Please log in to continue.");
    }

    const query = new URLSearchParams(params).toString();

    const url = query
      ? `${API}${endpoint}?${query}`
      : `${API}${endpoint}`;

    const response = await fetch(url, {
      method: "GET",
      headers: {
        Authorization: `Bearer ${auth.token}`,
      },
    });

    await handleApiError(response);

    return response;
  }

  // ==========================================
  // GENERATE AI IMAGES
  // ==========================================

  async function generateImages(e) {
    e.preventDefault();

    if (loading) return;

    setLoading(true);
    setError("");
    setImages([]);

    try {
      const params = {
        prompt: imagePrompt,
        quality,
        width: 1024,
        height: 1024,
        steps: quality === "high" ? 25 : 20,
        cfgScale: 7,
        seed: 0,
      };

      if (n === 1) {
        const response = await getResponse(
          "/generate-image",
          params
        );

        const blob = await response.blob();

        const image = await new Promise((resolve, reject) => {
          const reader = new FileReader();

          reader.onload = () => resolve(reader.result);

          reader.onerror = () =>
            reject(
              new Error("Failed to read generated image")
            );

          reader.readAsDataURL(blob);
        });

        setImages([image]);
      } else {
        const response = await getResponse(
          "/generate-images",
          {
            ...params,
            n,
          }
        );

        const result = await response.json();

        if (!Array.isArray(result)) {
          throw new Error("Invalid image response");
        }

        setImages(result);
      }
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }

  // ==========================================
  // GENERATE AI RECIPE
  // ==========================================

  async function generateRecipe(e) {
    e.preventDefault();

    if (loading) return;

    setLoading(true);
    setError("");
    setRecipe("");
    setRecipeTitle("");
    setRecipeSaved(false);

    try {
      const response = await getResponse(
        "/generate-recipe",
        {
          ingredients,
          cuisine,
          dietaryRestrictions: diet,
        }
      );

      const generatedRecipe = await response.text();

      setRecipe(generatedRecipe);

      setRecipeTitle(
        `${cuisine} Recipe - ${ingredients.trim().slice(0, 60)}`
      );
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }

  // ==========================================
  // SAVE RECIPE TO POSTGRESQL
  // ==========================================

  async function saveRecipe() {
    if (!recipe || !auth?.token || recipeSaved || loading) {
      return;
    }

    if (!recipeTitle.trim()) {
      setError("Please enter a recipe title.");
      return;
    }

    setLoading(true);
    setError("");

    try {
      const response = await fetch(`${API}/recipes`, {
        method: "POST",
        headers: {
          Authorization: `Bearer ${auth.token}`,
          "Content-Type": "application/json",
        },
        body: JSON.stringify({
          title: recipeTitle.trim(),
          ingredients,
          cuisine,
          dietaryRestrictions: diet,
          content: recipe,
        }),
      });

      await handleApiError(response);

      setRecipeSaved(true);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }

  // ==========================================
  // CHANGE TAB
  // ==========================================

  function changeTab(nextTab) {
    setTab(nextTab);
    setError("");
  }

  // ==========================================
  // LOGIN / REGISTER SCREEN
  // ==========================================

  if (!auth) {
    return <AuthPanel onLogin={handleLogin} />;
  }

  // ==========================================
  // MAIN APPLICATION
  // ==========================================

  return (
    <div className="app">

      {/* HEADER */}
      <header className="header">
        <div className="logo">
          ✦ PromptVision AI
        </div>

        <div className="user-bar">
          <span>Welcome, {auth.name}</span>

          <button
            type="button"
            className="logout-btn"
            onClick={handleLogout}
          >
            Logout
          </button>
        </div>

        <h1>Your AI Creative Workspace</h1>

        <p>
          Chat with AI, generate images, discover recipes,
          and revisit your creations.
        </p>
      </header>

      {/* NAVIGATION */}
      <nav className="tabs">

        <button
          type="button"
          className={tab === "chat" ? "active" : ""}
          onClick={() => changeTab("chat")}
        >
          AI Chat
        </button>

        <button
          type="button"
          className={tab === "image" ? "active" : ""}
          onClick={() => changeTab("image")}
        >
          AI Image
        </button>

        <button
          type="button"
          className={tab === "history" ? "active" : ""}
          onClick={() => changeTab("history")}
        >
          My Images
        </button>

        <button
          type="button"
          className={tab === "recipe" ? "active" : ""}
          onClick={() => changeTab("recipe")}
        >
          AI Recipe
        </button>

        <button
          type="button"
          className={tab === "recipes" ? "active" : ""}
          onClick={() => changeTab("recipes")}
        >
          My Recipes
        </button>

      </nav>

      {/* MAIN PANEL */}
      <main className="panel">

        {error && (
          <div className="error" role="alert">
            {error}
          </div>
        )}

        {/* CHAT HISTORY */}
        {tab === "chat" && (
          <ChatHistory
            key={auth.token}
            token={auth.token}
          />
        )}

        {/* IMAGE GENERATOR */}
        {tab === "image" && (
          <section>
            <h2>AI Image Generator</h2>
            <p>Turn your imagination into images.</p>

            <form onSubmit={generateImages}>
              <label>Describe your image</label>

              <textarea
                placeholder="A warrior riding a massive black dragon..."
                value={imagePrompt}
                onChange={(e) => setImagePrompt(e.target.value)}
                required
              />

              <div className="fields">
                <div>
                  <label>Quality</label>
                  <select
                    value={quality}
                    onChange={(e) => setQuality(e.target.value)}
                  >
                    <option value="standard">Standard</option>
                    <option value="high">High</option>
                  </select>
                </div>

                <div>
                  <label>Number of images</label>
                  <select
                    value={n}
                    onChange={(e) => setN(Number(e.target.value))}
                  >
                    <option value={1}>1 image</option>
                    <option value={2}>2 images</option>
                    <option value={3}>3 images</option>
                    <option value={4}>4 images</option>
                  </select>
                </div>
              </div>

              <button type="submit" disabled={loading}>
                {loading ? "Generating..." : "Generate Images"}
              </button>
            </form>

            {images.length > 0 && (
              <div className="result">
                <h3>Generated Images</h3>

                <div className="image-grid">
                  {images.map((image, index) => {
                    const extension =
                      image.startsWith("data:image/webp")
                        ? "webp"
                        : image.startsWith("data:image/png")
                        ? "png"
                        : "jpg";

                    return (
                      <div className="image-card" key={index}>
                        <img
                          src={image}
                          alt={`Generated image ${index + 1}`}
                        />

                        <a
                          href={image}
                          download={`ai-image-${index + 1}.${extension}`}
                        >
                          Download Image
                        </a>
                      </div>
                    );
                  })}
                </div>
              </div>
            )}
          </section>
        )}

        {/* IMAGE HISTORY */}
        {tab === "history" && (
          <ImageHistory token={auth.token} />
        )}

        {/* AI RECIPE GENERATOR */}
        {tab === "recipe" && (
          <section>
            <h2>AI Recipe Generator</h2>
            <p>Make something delicious with AI.</p>

            <form onSubmit={generateRecipe}>
              <label>Ingredients</label>

              <textarea
                placeholder="Chicken, rice, onion, tomato..."
                value={ingredients}
                onChange={(e) => {
                  setIngredients(e.target.value);
                  setRecipe("");
                  setRecipeTitle("");
                  setRecipeSaved(false);
                }}
                required
              />

              <div className="fields">
                <div>
                  <label>Cuisine</label>
                  <select
                    value={cuisine}
                    onChange={(e) => {
                      setCuisine(e.target.value);
                      setRecipe("");
                      setRecipeTitle("");
                      setRecipeSaved(false);
                    }}
                  >
                    <option>Indian</option>
                    <option>Chinese</option>
                    <option>Italian</option>
                    <option>Mexican</option>
                    <option>Any</option>
                  </select>
                </div>

                <div>
                  <label>Dietary Restrictions</label>
                  <input
                    value={diet}
                    onChange={(e) => {
                      setDiet(e.target.value);
                      setRecipe("");
                      setRecipeTitle("");
                      setRecipeSaved(false);
                    }}
                    placeholder="None, Vegetarian, Vegan..."
                  />
                </div>
              </div>

              <button type="submit" disabled={loading}>
                {loading
                  ? "Cooking up a recipe..."
                  : "Generate Recipe"}
              </button>
            </form>

            {/* GENERATED RECIPE AND SAVE */}
            {recipe && (
              <div className="result">
                <h3>Your Recipe</h3>

                <p style={{ whiteSpace: "pre-wrap" }}>
                  {recipe}
                </p>

                <label htmlFor="recipe-title">
                  Recipe Title
                </label>

                <input
                  id="recipe-title"
                  value={recipeTitle}
                  maxLength={200}
                  onChange={(e) => setRecipeTitle(e.target.value)}
                  placeholder="Enter your recipe title"
                  disabled={recipeSaved}
                />

                <button
                  type="button"
                  onClick={saveRecipe}
                  disabled={loading || recipeSaved}
                >
                  {recipeSaved
                    ? "✓ Recipe Saved"
                    : loading
                    ? "Saving..."
                    : "Save Recipe"}
                </button>
              </div>
            )}
          </section>
        )}

        {/* SAVED RECIPES */}
        {tab === "recipes" && (
          <MyRecipes token={auth.token} />
        )}
      </main>

      {/* FOOTER */}
      <footer>
        PromptVision AI — Powered by React, Spring Boot,
        Google Gemini and AI Horde
      </footer>
    </div>
  );
}

export default App;
