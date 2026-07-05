# AI Tester 3x – Daily Learnings

Daily notes and assignments from the AI Testing course.

**Course:** [The Testing Academy](https://courses.thetestingacademy.com/mycourses)

---

## Project Overview

**AITester3x** is a comprehensive learning repository demonstrating the intersection of **AI-powered test automation** with **traditional QA practices** and **modern LLM applications**. This project covers:

- Building enterprise-grade REST API testing frameworks
- Creating AI agents for test automation workflows
- Integrating LLMs and automation tools (n8n, Langflow)
- Building full-stack RAG (Retrieval-Augmented Generation) applications
- Practical hands-on implementation of modern test architecture and AI applications

---

## Progress

| Day/Project               | Topic                                                                 |
| ------------------------- | --------------------------------------------------------------------- |
| [Day 1](./Day1/README.md) | Installation and Set Up – Visual Studio, Antigravity, n8n             |
| [Day 2](./Day2/README.md) | LLM Basics, Models, Ollama, LM Studio, Tokenization                   |
| [Day 3](./Day3/)          | REST Assured API Framework, Langflow Automation Agents, n8n Workflows |
| [RAG Explorer](./BasicRAG/) | Full-stack RAG application – FastAPI backend + React frontend         |

---

## Day 3: REST Assured API Framework & AI Automation Agents

### 📁 Project Structure

```
Day3/
├── RestAssuredAPIFramework/          # Complete REST API testing framework
├── RestAssuredFramework/             # Alternate framework variant
├── n8n/
│   └── ai-video-editor.json          # n8n workflow for video processing
└── REST_ASSURED_API_FRAMEWORK_SKILL.md
```

### 🎯 Components Overview

#### 1. **REST Assured API Testing Framework**

- **Location:** `Day3/RestAssuredAPIFramework/`
- **Framework Stack:**
  - **Language:** Java 11+
  - **Build Tool:** Maven
  - **Test Framework:** TestNG
  - **API Testing:** Rest Assured 5.x
  - **Assertions:** Hamcrest + AssertJ
  - **Logging:** Log4j2
  - **Reporting:** ExtentReports

- **Architecture Layers:**

  ```
  src/main/java/com/apitests/
  ├── base/              BaseTest.java
  ├── config/            ConfigReader.java
  ├── constants/         ApiConstants.java, StatusCodes.java
  ├── models/            Post.java, User.java
  └── utils/             RestUtils.java, JsonUtils.java, ReportManager.java

  src/test/java/com/apitests/
  ├── get/               GetPostsTest.java
  ├── post/              CreatePostTest.java
  ├── put/               UpdatePostTest.java
  ├── delete/            DeletePostTest.java
  └── endtoend/          CrudFlowTest.java
  ```

- **Test Coverage:**
  - **GET:** Retrieve resources, handle 200/404 responses
  - **POST:** Create resources with 201/400 validation
  - **PUT:** Update resources with full-object replacement
  - **DELETE:** Remove resources with cleanup verification
  - **CRUD Flow:** End-to-end workflow testing

- **Data Source:** JSONPlaceholder API (`https://jsonplaceholder.typicode.com`)

- **Key Features:**
  - Layered framework architecture following SOLID principles
  - Data-driven testing with TestNG `@DataProvider`
  - Request/Response logging and validation
  - Comprehensive error handling
  - Extensible base test class for code reusability

#### 2. **Langflow Automation Agents**

- **Location:** `Day3/Langflow/`
- **6 Pre-configured AI Agents:**
  1.  **bug-triage-agent.json** - Automatically categorizes and prioritizes bugs using LLM
  2.  **flaky-test-analyzer.json** - Analyzes flaky test patterns and suggests improvements
  3.  **rca-bot.json** - Root Cause Analysis bot for test failures
  4.  **simple-agent.json** - Basic conversational agent for general Q&A
  5.  **test-case-creator.json** - Auto-generates test cases from requirements
  6.  **test-plan-generator.json** - Creates comprehensive test plans using AI

- **Purpose:** Demonstrates integration of LLMs into test automation workflows using Langflow visual editor

#### 3. **n8n Workflow: AI Video Editor**

- **Location:** `Day3/n8n/ai-video-editor.json`
- **Workflow Components:**
  - Video scanning and metadata extraction
  - Frame extraction and quality analysis using GPT-4o
  - Intelligent clip trimming based on quality scores
  - Multi-clip ranking and merging
  - Final video validation

- **Integration:** OpenAI GPT-4o for AI-powered video quality assessment
- **Processing:** FFmpeg for video manipulation tasks

### 📊 What Was Accomplished

✅ **Framework Development:**

- Enterprise-grade REST API test framework from scratch
- Complete CRUD operation test coverage
- Multi-layer architecture with separation of concerns
- Configuration-driven test execution
- Integrated logging and reporting

✅ **AI/Automation Integration:**

- Created 6 production-ready Langflow agents
- Configured n8n workflow for complex video processing with AI
- Demonstrated GenAI integration in QA workflows

✅ **Code Quality:**

- Zero-defect, production-ready Java code
- SOLID principles applied throughout
- Comprehensive error handling
- Reusable utility functions
- Enterprise naming conventions

### 🚀 How to Use

#### Running REST Assured Tests:

```bash
cd Day3/RestAssuredAPIFramework
mvn clean test
mvn test -Dgroups=smoke
```

#### Using Langflow Agents:

1. Import JSON files into Langflow
2. Configure API keys and endpoints
3. Deploy agents for automation

#### Using n8n Workflow:

1. Import the JSON file into n8n
2. Set up video directory and output paths
3. Configure OpenAI API credentials
4. Deploy and trigger the workflow

### 📚 Learning Outcomes

By completing Day 3, you've learned:

- How to design scalable REST API testing frameworks
- Integration of LLMs with QA automation
- Advanced workflow automation using n8n
- Visual workflow design with Langflow
- AI-assisted test generation and analysis

---

## RAG Explorer – Retrieval-Augmented Generation Application

### 📁 Project Structure

```
BasicRAG/
├── backend/                          # Python FastAPI server
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
│   └── package.json                  # Node.js dependencies
│
├── data/
│   ├── pdf/                          # 📥 Drop PDFs here for auto-ingestion
│   └── chroma_db/                    # Vector database (auto-created)
│
├── .gitignore                        # Git exclusion rules
└── README.md                         # Project documentation
```

### 🎯 Project Features

**RAG Explorer** is a modern full-stack application demonstrating Retrieval-Augmented Generation:

- **Semantic Search** - Convert documents to embeddings and retrieve relevant context
- **LLM Integration** - Query Groq API for intelligent responses based on retrieved context
- **Real-time Pipeline** - Visualize each stage of the RAG process (retrieval, context assembly, LLM response)
- **Auto-ingestion** - Automatically process new PDFs dropped into the data folder
- **Visual Dashboard** - Dark-themed UI showing indexed chunks and document sources

### 🛠️ Technology Stack

**Backend:**
- Python 3.13 + FastAPI
- ChromaDB for vector storage
- Sentence-Transformers for embeddings
- Groq API for LLM integration
- Watchdog for file monitoring
- PyPDF for PDF processing

**Frontend:**
- React 18 with Vite build tool
- Tailwind CSS for styling
- Fetch API for backend communication

### 🚀 Quick Start

```bash
# Backend Setup
cd BasicRAG/backend
python -m venv .venv
.\.venv\Scripts\activate  # Windows
pip install -r requirements.txt

# Create .env file
echo GROQ_API_KEY=your_key_here > .env

# Run backend server
..\.venv\Scripts\python -m uvicorn main:app --reload

# Frontend Setup (in new terminal)
cd BasicRAG/frontend
npm install
npm run dev

# Access at http://localhost:5173
```

### 📊 RAG Pipeline Flow

1. User submits query → Frontend sends to backend
2. Query converted to embedding using Sentence-Transformers
3. Semantic search retrieves top-4 relevant chunks from ChromaDB
4. Retrieved context assembled and sent to Groq API
5. LLM generates intelligent response with document context
6. Pipeline visualization displays all stages in frontend UI

### 🔌 API Endpoints

- `GET /api/health` - Health check
- `GET /api/stats` - Get indexing statistics (chunks, sources)
- `POST /api/query` - Submit query and get RAG response
- `POST /api/reindex` - Manually trigger PDF reprocessing

### ✅ Key Accomplishments

- ✅ Built production-grade RAG system with async processing
- ✅ Implemented auto-ingestion pipeline for PDF documents
- ✅ Integrated Groq LLM API for intelligent responses
- ✅ Created responsive React UI with pipeline visualization
- ✅ Set up CORS and proper error handling
- ✅ Comprehensive documentation and troubleshooting guide

---


