### Go Backend (Target)
*   **Root:** `go-backend/`
*   **Run:** `cd go-backend && go run main.go`
*   **Dependencies:** Managed via `go.mod`.

## Implementation Status
*   [x] **Project Structure:** Hertz framework set up.
*   [x] **Configuration:** Loading `text_providers.yaml` and `image_providers.yaml`.
*   [x] **Generators:**
    *   [x] `generators/base.go`: Interfaces defined.
    *   [x] `generators/openai.go`: Uses `github.com/openai/openai-go` SDK.
    *   [x] `generators/gemini.go`: Uses `google.golang.org/genai` SDK.
    *   [x] `generators/factory.go`: Factory for creating generators.
*   [x] **Services:**
    *   [x] `service/outline.go`: Outline generation logic ported.
    *   [x] `service/image.go`: Basic image generation logic ported.
    *   [x] `service/init.go`: Service initialization.
*   [x] **API Handlers:**
    *   [x] `api/handlers.go`: `GenerateOutline` implemented. Stubs for others.
    *   [x] `main.go`: Static file serving added for Docker support.
*   [x] **Docker:** `Dockerfile` updated to build and run Go backend.
*   [ ] **Pending:**
    *   Full Image Service features (queue, concurrency, history).
    *   History management APIs.

## Key Tasks (Migration)
1.  **Port Logic:** Translate Python logic from `backend/generators` and `backend/services` to `go-backend`. (In Progress)
2.  **API Parity:** Ensure `go-backend` endpoints in `api/router.go` match the behavior of `backend/routes/api.py`. (In Progress)