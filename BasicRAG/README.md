# RAG Explorer - Retrieval-Augmented Generation Application

A full-stack application that enables users to upload PDFs, explore the RAG pipeline visually, and get intelligent answers from a Large Language Model (LLM) using semantic search and context retrieval.

## 🎯 Project Overview

**RAG Explorer** is a modern web application demonstrating Retrieval-Augmented Generation (RAG), combining:

- **Semantic Search** - Convert documents to embeddings and retrieve relevant context
- **LLM Integration** - Query Groq API for intelligent responses based on retrieved context
- **Real-time Pipeline** - Visualize each stage of the RAG process (retrieval, context assembly, LLM response)
- **Auto-ingestion** - Automatically process new PDFs dropped into the data folder

---

## 🏗️ Project Architecture

```
BasicRAG/
├── backend/                          # Python FastAPI server
│   ├── __init__.py
│   ├── main.py                       # FastAPI application entry point
│   ├── config.py                     # Configuration & environment variables
│   ├── chunker.py                    # PDF parsing & text chunking
│   ├── embedder.py                   # Text-to-embeddings conversion
│   ├── vector_store.py               # ChromaDB wrapper for semantic search
│   ├── ingestion_watcher.py          # Auto-detect & process new PDFs
│   ├── llm_service.py                # Groq LLM API integration
│   └── requirements.txt              # Python dependencies
│
├── frontend/                         # React + Vite application
│   ├── src/
│   │   ├── App.jsx                   # Main React component with pipeline UI
│   │   ├── api.js                    # Backend API client
│   │   ├── main.jsx                  # React entry point
│   │   └── index.css                 # Tailwind CSS styling
│   ├── index.html                    # HTML template
│   ├── vite.config.js                # Vite build configuration
│   ├── package.json                  # Node.js dependencies
│   └── package-lock.json
│
├── data/
│   ├── pdf/                          # 📥 Drop PDFs here for auto-ingestion
│   ├── chroma_db/                    # Vector database (auto-created)
│   └── VWO Login Dashboard Test Plan.docx  # Sample document
│
├── .gitignore                        # Git exclusion rules
└── README.md                         # This file
```

---

## 🚀 Quick Start

### Prerequisites

- **Python 3.13+** with pip
- **Node.js 18+** with npm
- **Groq API Key** (free at [console.groq.com](https://console.groq.com))
- **Git** for version control

### 1️⃣ Backend Setup

```bash
# Navigate to project root
cd BasicRAG

# Create Python virtual environment
python -m venv .venv

# Activate virtual environment
# Windows:
.\.venv\Scripts\activate
# macOS/Linux:
source .venv/bin/activate

# Install Python dependencies
pip install -r backend/requirements.txt
```

### 2️⃣ Environment Configuration

Create a `.env` file in the backend directory:

```bash
cd backend
```

Create `.env` file with:

```env
GROQ_API_KEY=your_groq_api_key_here
```

### 3️⃣ Frontend Setup

```bash
# From project root
cd frontend
npm install
```

### 4️⃣ Run the Application

**Terminal 1 - Backend Server:**

```bash
cd backend
..\.venv\Scripts\python -m uvicorn main:app --host 0.0.0.0 --port 8000 --reload
```

**Terminal 2 - Frontend Dev Server:**

```bash
cd frontend
npm run dev
```

### 5️⃣ Access the Application

- **Frontend:** http://localhost:5173
- **Backend API:** http://localhost:8000
- **API Docs:** http://localhost:8000/docs

---

## 📚 Features

### ✨ Core Functionality

| Feature                    | Description                                                  |
| -------------------------- | ------------------------------------------------------------ |
| **PDF Upload**             | Drop PDFs in `data/pdf/` folder for automatic processing     |
| **Auto-Ingestion**         | Watchdog monitors folder and processes new PDFs in real-time |
| **Semantic Search**        | Query PDFs using natural language, not just keywords         |
| **LLM Integration**        | Get intelligent answers with context from your documents     |
| **Pipeline Visualization** | See each stage: Retrieve → Context → LLM Response            |
| **Stats Dashboard**        | View total chunks indexed and document sources               |

### 🔄 RAG Pipeline Flow

1. **User Query** → Entered in frontend textbox
2. **Embedding Generation** → Query converted to vector using Sentence-Transformers
3. **Semantic Retrieval** → Top-4 relevant chunks retrieved from ChromaDB
4. **Context Assembly** → Retrieved chunks combined as LLM context
5. **LLM Response** → Groq API generates answer based on context
6. **Visual Display** → Each stage shown with expandable sections in frontend

---

## 🛠️ Technology Stack

### Backend

- **Framework:** FastAPI (async web framework)
- **Server:** Uvicorn (ASGI server)
- **Vector Database:** ChromaDB (local persistent storage)
- **Embeddings:** Sentence-Transformers (`all-MiniLM-L6-v2` model)
- **PDF Processing:** PyPDF (text extraction)
- **LLM:** Groq API (`llama-3.1-8b-instant` model)
- **File Monitoring:** Watchdog (filesystem events)
- **Data Validation:** Pydantic

### Frontend

- **Framework:** React 18 (UI library)
- **Build Tool:** Vite (fast dev server & bundler)
- **Styling:** Tailwind CSS (utility-first CSS)
- **HTTP Client:** Fetch API (native)
- **Package Manager:** npm

---

## 📝 API Documentation

### Endpoints

#### `GET /api/health`

Health check endpoint.

**Response:**

```json
{
  "status": "ok"
}
```

---

#### `GET /api/stats`

Get indexing statistics.

**Response:**

```json
{
  "total_chunks": 156,
  "sources": ["document1.pdf", "document2.pdf"]
}
```

---

#### `POST /api/query`

Query the RAG system.

**Request:**

```json
{
  "query": "What is the login process?"
}
```

**Response:**

```json
{
  "query": "What is the login process?",
  "query_embedding": [0.123, -0.456, ...],
  "retrieved_docs": [
    {
      "text": "Login requires username and password...",
      "source": "document1.pdf",
      "page": 1,
      "similarity": 0.89
    }
  ],
  "context": "Login requires username and password...\n\nAdditional context...",
  "llm_response": "To login, you need to...",
  "timestamp": "2024-07-05T10:30:45Z"
}
```

---

#### `POST /api/reindex`

Manually trigger PDF folder reprocessing.

**Response:**

```json
{
  "status": "reindexing started",
  "pdfs_found": 3
}
```

---

## 🔑 Configuration

### Backend Configuration (`backend/config.py`)

| Variable          | Default              | Purpose                                |
| ----------------- | -------------------- | -------------------------------------- |
| `GROQ_API_KEY`    | Env var              | API key for Groq LLM                   |
| `PDF_DIR`         | `../data/pdf/`       | Directory to monitor for PDFs          |
| `CHROMA_DIR`      | `../data/chroma_db/` | Vector database storage location       |
| `CHUNK_SIZE`      | 500                  | Characters per chunk                   |
| `OVERLAP`         | 100                  | Character overlap between chunks       |
| `TOP_K`           | 4                    | Number of chunks to retrieve per query |
| `MODEL_NAME`      | `all-MiniLM-L6-v2`   | Sentence-Transformers model            |
| `COLLECTION_NAME` | `rag_documents`      | ChromaDB collection name               |

### Frontend Configuration (`frontend/vite.config.js`)

- API calls proxied to `http://localhost:8000`
- Tailwind CSS configured for styling
- React 18 with Hot Module Replacement (HMR)

---

## 📂 File Structure Details

### Backend Modules

**main.py**

- FastAPI application setup
- CORS configuration for frontend
- Route definitions for all endpoints
- Auto-startup of ingestion watcher

**config.py**

- Centralized configuration management
- Environment variable loading
- Path definitions
- Model and API settings

**chunker.py**

- PDFChunker class for PDF processing
- Sentence-aware text splitting
- Metadata preservation (source, page number)

**embedder.py**

- TextEmbedder class for vector generation
- Batch embedding support
- Model caching for efficiency

**vector_store.py**

- ChromaDB wrapper class
- Collection management
- Semantic search implementation
- Error handling for missing collections

**ingestion_watcher.py**

- DocumentIngester class
- Watchdog observer for file system events
- Pipeline orchestration (chunk → embed → store)
- Auto-startup on app launch

**llm_service.py**

- LLMService class for Groq API integration
- Prompt construction with context
- Response generation

### Frontend Components

**App.jsx**

- Main React component
- Dark theme UI with Tailwind CSS
- Pipeline visualization with expandable sections
- State management with React hooks
- Stats fetching and display

**api.js**

- Backend communication module
- `getStats()` - Fetch indexing statistics
- `queryDocuments(query)` - Submit queries and retrieve responses

---

## 🧪 Testing

### Manual Testing Steps

1. **Check Backend Health:**

   ```bash
   curl http://localhost:8000/api/health
   ```

2. **Get Statistics:**

   ```bash
   curl http://localhost:8000/api/stats
   ```

3. **Submit Query:**

   ```bash
   curl -X POST http://localhost:8000/api/query \
     -H "Content-Type: application/json" \
     -d '{"query":"What is this document about?"}'
   ```

4. **Test Auto-Ingestion:**
   - Drop a PDF in `data/pdf/` folder
   - Monitor backend logs for processing
   - Verify stats endpoint shows increased chunk count

5. **Frontend Testing:**
   - Navigate to http://localhost:5173
   - Verify stats display correctly
   - Submit a query and observe pipeline visualization

---

## 🐛 Troubleshooting

### Issue: Backend won't start - "Module not found"

**Solution:** Ensure you're running from the `backend/` directory:

```bash
cd backend
..\.venv\Scripts\python -m uvicorn main:app --reload
```

### Issue: "ChromaDB collection not found"

**Solution:** The collection is auto-created on first request. If persisting, delete `data/chroma_db/` and restart.

### Issue: "GROQ_API_KEY not set"

**Solution:** Create `.env` file in `backend/` directory with your API key.

### Issue: Frontend shows "0 chunks, 0 documents"

**Solution:** This is normal if no PDFs have been ingested. Drop a PDF in `data/pdf/` and it will be automatically processed.

### Issue: Query returns empty results

**Solution:**

- Verify PDFs are in `data/pdf/`
- Check backend logs for ingestion errors
- Run `/api/reindex` endpoint to force reprocessing

---

## 🔐 Security Considerations

- **API Key:** Never commit `.env` file; add to `.gitignore` (already configured)
- **CORS:** Currently enabled for localhost; configure for production
- **Rate Limiting:** Consider adding for API endpoints in production
- **Input Validation:** Pydantic validates all inputs
- **Error Handling:** Sensitive details not exposed in API responses

---

## 📦 Dependencies

### Python Backend

See `backend/requirements.txt` for complete list:

- fastapi, uvicorn, pydantic, chromadb, sentence-transformers, groq, pypdf, watchdog, python-dotenv

### Node.js Frontend

See `frontend/package.json`:

- react, react-dom, vite, tailwindcss, autoprefixer

---

## 🚀 Deployment

### Considerations for Production

1. **Environment Variables:**
   - Store in secure environment (Azure Key Vault, AWS Secrets)
   - Never hardcode API keys

2. **Database:**
   - Consider cloud vector database (Pinecone, Weaviate)
   - Implement backup strategy for ChromaDB

3. **Scaling:**
   - Add database connection pooling
   - Implement caching layer (Redis)
   - Use async processing for large PDFs

4. **Monitoring:**
   - Add logging to Azure Monitor or similar
   - Track ingestion metrics
   - Monitor API latency

---

## 📝 License

Part of AITester3x learning repository.

---

## 📞 Support

For issues or questions:

1. Check the Troubleshooting section
2. Review API documentation at `http://localhost:8000/docs`
3. Check backend logs for detailed error messages

---

## 🙏 Acknowledgments

- Built as part of [The Testing Academy](https://courses.thetestingacademy.com/mycourses) AI Testing course
- Uses ChromaDB for vector storage
- Powered by Groq's fast LLM API
- Frontend styled with Tailwind CSS

---

**Last Updated:** July 5, 2024  
**Status:** ✅ Fully Functional
