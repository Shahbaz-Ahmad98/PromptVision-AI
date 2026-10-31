
import { useEffect, useState } from "react";

const API = "/api";

function ImageHistory({ token }) {
  const [images, setImages] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [deletingId, setDeletingId] = useState(null);
  const [refreshKey, setRefreshKey] = useState(0);

  // ==========================================
  // LOAD SAVED IMAGES FROM BACKEND
  // ==========================================

  useEffect(() => {
    const controller = new AbortController();
    const objectUrls = [];

    async function loadHistory() {
      setLoading(true);
      setError("");

      try {
        const response = await fetch(
          `${API}/image-history`,
          {
            headers: {
              Authorization: `Bearer ${token}`,
            },
            signal: controller.signal,
          }
        );

        if (response.status === 401) {
          throw new Error(
            "Session expired. Please logout and login again."
          );
        }

        if (!response.ok) {
          throw new Error(
            `Failed to load image history: ${response.status}`
          );
        }

        const records = await response.json();

        if (!Array.isArray(records)) {
          throw new Error("Invalid image history response");
        }

        const savedImages = await Promise.all(
          records.map(async (record) => {
            const imageResponse = await fetch(
              `${API}/image-history/${record.id}`,
              {
                headers: {
                  Authorization: `Bearer ${token}`,
                },
                signal: controller.signal,
              }
            );

            if (!imageResponse.ok) {
              throw new Error(
                `Failed to load image ${record.id}`
              );
            }

            const blob = await imageResponse.blob();
            const url = URL.createObjectURL(blob);

            objectUrls.push(url);

            return {
              ...record,
              url,
            };
          })
        );

        if (!controller.signal.aborted) {
          setImages(savedImages);
        }
      } catch (err) {
        if (!controller.signal.aborted) {
          setError(err.message);
        }
      } finally {
        if (!controller.signal.aborted) {
          setLoading(false);
        }
      }
    }

    loadHistory();

    return () => {
      controller.abort();

      objectUrls.forEach((url) => {
        URL.revokeObjectURL(url);
      });
    };
  }, [token, refreshKey]);

  // ==========================================
  // DELETE SAVED IMAGE
  // ==========================================

  async function deleteImage(id) {
    const confirmed = window.confirm(
      "Are you sure you want to delete this image?"
    );

    if (!confirmed) {
      return;
    }

    setDeletingId(id);
    setError("");

    try {
      const response = await fetch(
        `${API}/image-history/${id}`,
        {
          method: "DELETE",
          headers: {
            Authorization: `Bearer ${token}`,
          },
        }
      );

      if (!response.ok) {
        throw new Error(
          `Failed to delete image: ${response.status}`
        );
      }

      // Refresh gallery after deleting.
      setRefreshKey((value) => value + 1);
    } catch (err) {
      setError(err.message);
    } finally {
      setDeletingId(null);
    }
  }

  // ==========================================
  // FILE EXTENSION
  // ==========================================

  function getExtension(contentType) {
    switch (contentType) {
      case "image/webp":
        return "webp";

      case "image/png":
        return "png";

      case "image/jpeg":
        return "jpg";

      default:
        return "jpg";
    }
  }

  // ==========================================
  // FORMAT CREATION DATE
  // ==========================================

  function formatDate(date) {
    if (!date) {
      return "Unknown";
    }

    const parsed = new Date(date);

    if (Number.isNaN(parsed.getTime())) {
      return date;
    }

    return parsed.toLocaleString();
  }

  // ==========================================
  // UI
  // ==========================================

  return (
    <section>
      <h2>My Image History</h2>

      <p>
        View, download, and manage your previously
        generated AI images.
      </p>

      <button
        type="button"
        disabled={loading}
        onClick={() => setRefreshKey((value) => value + 1)}
      >
        Refresh History
      </button>

      {error && (
        <div className="error" role="alert">
          {error}
        </div>
      )}

      {loading && (
        <p>Loading your saved images...</p>
      )}

      {!loading && !error && images.length === 0 && (
        <div className="result">
          <p>
            No images saved yet. Generate your first
            image to see it here.
          </p>
        </div>
      )}

      {!loading && images.length > 0 && (
        <div className="result">
          <h3>Saved Images ({images.length})</h3>

          <div className="image-grid">
            {images.map((image) => (
              <div className="image-card" key={image.id}>
                <img
                  src={image.url}
                  alt={image.prompt}
                  loading="lazy"
                  style={{ cursor: "pointer" }}
                  onClick={() => window.open(
                    image.url,
                    "_blank",
                    "noopener,noreferrer"
                  )}
                />

                <div className="history-info">
                  <p>
                    <strong>Prompt:</strong>{" "}
                    {image.prompt}
                  </p>

                  <p>
                    <strong>Created:</strong>{" "}
                    {formatDate(image.createdAt)}
                  </p>
                </div>

                <a
                  href={image.url}
                  download={`ai-image-${image.id}.${getExtension(
                    image.contentType
                  )}`}
                >
                  Download Image
                </a>

                <button
                  type="button"
                  disabled={deletingId !== null}
                  onClick={() => deleteImage(image.id)}
                >
                  {deletingId === image.id
                    ? "Deleting..."
                    : "Delete Image"}
                </button>
              </div>
            ))}
          </div>
        </div>
      )}
    </section>
  );
}

export default ImageHistory;
