
import { useCallback, useEffect, useState } from "react";

export default function ChatHistory({ token }) {
  const [conversations, setConversations] = useState([]);
  const [activeId, setActiveId] = useState(null);
  const [messages, setMessages] = useState([]);
  const [question, setQuestion] = useState("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  const request = useCallback(
    async (path, options = {}) => {
      const response = await fetch(`/api/chats${path}`, {
        ...options,
        headers: {
          Authorization: `Bearer ${token}`,
          ...(options.body
            ? { "Content-Type": "application/json" }
            : {}),
        },
      });

      if (!response.ok) {
        if (response.status === 401) {
          throw new Error("Session expired. Please log in again.");
        }
        if (response.status === 503 || response.status === 502) {
          throw new Error("Gemini is busy. Please try again.");
        }
        throw new Error(`Chat API error: ${response.status}`);
      }

      if (response.status === 204) return null;
      return response.json();
    },
    [token]
  );

  const loadConversations = useCallback(async () => {
    const data = await request("");
    setConversations(data);
  }, [request]);

  useEffect(() => {
    let cancelled = false;

    request("")
      .then((data) => {
        if (!cancelled) setConversations(data);
      })
      .catch((err) => {
        if (!cancelled) setError(err.message);
      });

    return () => {
      cancelled = true;
    };
  }, [request]);

  async function openChat(id) {
    if (loading) return;

    setError("");

    try {
      const data = await request(`/${id}`);
      setActiveId(id);
      setMessages(data.messages);
      setQuestion("");
    } catch (err) {
      setError(err.message);
    }
  }

  function newChat() {
    if (loading) return;

    setActiveId(null);
    setMessages([]);
    setQuestion("");
    setError("");
  }

  async function sendMessage(e) {
    e.preventDefault();

    const text = question.trim();
    if (!text || loading) return;

    setLoading(true);
    setError("");

    try {
      let chatId = activeId;

      if (!chatId) {
        const conversation = await request("", {
          method: "POST",
        });
        chatId = conversation.id;
        setActiveId(chatId);
      }

      const reply = await request(`/${chatId}/messages`, {
        method: "POST",
        body: JSON.stringify({ message: text }),
      });

      setMessages((previous) => [
        ...previous,
        {
          id: `user-${Date.now()}`,
          role: "USER",
          content: text,
        },
        reply,
      ]);

      setQuestion("");
      await loadConversations();
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }

  async function deleteChat(id) {
    if (loading) return;
    if (!window.confirm("Delete this conversation?")) return;

    setLoading(true);
    setError("");

    try {
      await request(`/${id}`, { method: "DELETE" });

      if (activeId === id) {
        setActiveId(null);
        setMessages([]);
        setQuestion("");
      }

      await loadConversations();
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }

  return (
    <div
      style={{
        display: "grid",
        gridTemplateColumns: "minmax(180px, 250px) minmax(0, 1fr)",
        gap: "16px",
        width: "100%",
      }}
    >
      <aside
        style={{
          minWidth: 0,
          border: "1px solid #555",
          borderRadius: "12px",
          padding: "12px",
        }}
      >
        <h3>Chat History</h3>

        <button
          type="button"
          onClick={newChat}
          disabled={loading}
          style={{ width: "100%", marginBottom: "14px" }}
        >
          + New Chat
        </button>

        {conversations.map((chat) => (
          <div
            key={chat.id}
            style={{
              padding: "8px",
              marginBottom: "8px",
              border: "1px solid #666",
              borderRadius: "8px",
              background:
                activeId === chat.id ? "#30465d" : "transparent",
            }}
          >
            <button
              type="button"
              onClick={() => openChat(chat.id)}
              disabled={loading}
              style={{
                width: "100%",
                overflowWrap: "anywhere",
              }}
            >
              {chat.title}
            </button>

            <button
              type="button"
              onClick={() => deleteChat(chat.id)}
              disabled={loading}
              style={{ marginTop: "6px" }}
            >
              Delete
            </button>
          </div>
        ))}

        {conversations.length === 0 && (
          <p>No previous conversations.</p>
        )}
      </aside>

      <section style={{ minWidth: 0 }}>
        <h2>AI Chat Assistant</h2>

        {error && (
          <p role="alert" style={{ color: "#f87171" }}>
            {error}
          </p>
        )}

        <div
          style={{
            minHeight: "250px",
            maxHeight: "460px",
            overflowY: "auto",
            border: "1px solid #555",
            borderRadius: "12px",
            padding: "12px",
            marginBottom: "16px",
          }}
        >
          {messages.length === 0 && (
            <p>Start a new conversation with Gemini.</p>
          )}

          {messages.map((message) => (
            <div
              key={message.id}
              style={{
                marginBottom: "14px",
                padding: "12px",
                borderRadius: "10px",
                background:
                  message.role === "USER"
                    ? "#243e57"
                    : "#303039",
                whiteSpace: "pre-wrap",
                overflowWrap: "anywhere",
              }}
            >
              <strong>
                {message.role === "USER" ? "You" : "Gemini"}
              </strong>

              <p style={{ marginBottom: 0 }}>{message.content}</p>
            </div>
          ))}
        </div>

        <form onSubmit={sendMessage}>
          <textarea
            placeholder="Ask Gemini anything..."
            value={question}
            onChange={(e) => setQuestion(e.target.value)}
            required
            disabled={loading}
            rows={4}
            style={{ width: "100%" }}
          />

          <button type="submit" disabled={loading || !question.trim()}>
            {loading ? "Thinking..." : "Send Message"}
          </button>
        </form>
      </section>
    </div>
  );
}
