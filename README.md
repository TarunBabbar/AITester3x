# AITester3x – AI-Powered Test Automation Platform

A comprehensive platform demonstrating the integration of **AI-powered test automation** with **modern LLM applications** and **enterprise QA practices**.

---

## 📋 Project Overview

**AITester3x** is a complete implementation repository containing:

- ✅ **Enterprise-grade REST API testing frameworks** (Java + Maven)
- ✅ **AI-powered automation agents** (Langflow, LLMs)
- ✅ **Advanced workflow automation** (n8n)
- ✅ **Full-stack RAG application** (FastAPI + React)
- ✅ **Comprehensive installation and setup guides**
- ✅ **LLM fundamentals and tooling documentation**

---

## 📁 Project Structure

````
AITester3x/
│
├── BasicRAG/                                      # Full-stack RAG Application
│   ├── backend/                                   # Python FastAPI server
│   │   ├── main.py                               # API entry point
│   │   ├── config.py                             # Configuration management
│   │   ├── chunker.py                            # PDF parsing & chunking
│   │   ├── embedder.py                           # Text embeddings (Sentence-Transformers)
│   │   ├── vector_store.py                       # ChromaDB integration
│   │   ├── ingestion_watcher.py                  # Auto-detect & ingest PDFs
│   │   ├── llm_service.py                        # Groq LLM API integration
│   │   └── requirements.txt
│   ├── frontend/                                  # React + Vite application
│   │   ├── src/
│   │   │   ├── App.jsx                           # Main UI component
│   │   │   ├── api.js                            # API client
│   │   │   └── index.css                         # Tailwind styling
│   │   ├── index.html
│   │   ├── vite.config.js
│   │   └── package.json
│   ├── data/
│   │   ├── pdf/                                  # PDF ingestion folder
│   │   └── chroma_db/                            # Vector database
│   ├── .gitignore
│   └── README.md
│
├── RestAssuredAPIFramework/                      # Enterprise REST API Testing Framework
│   ├── src/
│   │   ├── main/java/com/apitests/
│   │   │   ├── base/                             # Base test classes
│   │   │   ├── config/                           # Configuration management
│   │   │   ├── constants/                        # API constants
│   │   │   ├── models/                           # Request/Response models
│   │   │   └── utils/                            # Utility functions
│   │   └── test/java/com/apitests/
│   │       ├── get/                              # GET endpoint tests
│   │       ├── post/                             # POST endpoint tests
│   │       ├── put/                              # PUT endpoint tests
│   │       ├── delete/                           # DELETE endpoint tests
│   │       └── endtoend/                         # CRUD flow tests
│   ├── pom.xml                                   # Maven configuration
│   ├── README.md
│   └── target/                                   # Build artifacts
│
├── RestAssuredFramework/                         # Alternate Framework Implementation
│   ├── src/
│   ├── pom.xml
│   └── target/
│
├── Langflow/                                     # AI Automation Agents
│   ├── bug-triage-agent.json                     # Bug categorization agent
│   ├── flaky-test-analyzer.json                  # Test flakiness analyzer
│   ├── rca-bot.json                              # Root cause analysis bot
│   ├── simple-agent.json                         # General Q&A agent
│   ├── test-case-creator.json                    # Test case generator
│   └── test-plan-generator.json                  # Test plan generator
│
├── n8n/                                          # Workflow Automation
│   └── ai-video-editor.json                      # AI-powered video processing workflow
│
├── n8nBasicRAGWorkflow/                          # n8n RAG Pipeline (Ingestion + Retrieval)
│   ├── AgenticAI_RAGWorkflow.json                # n8n RAG workflow definition
│   └── README.md                                 # Workflow documentation
│
├── Installation and Set Up/                      # Setup documentation
│   └── README.md
│
├── LLM Basics and AI Testing Tools/              # LLM fundamentals
│   └── README.md
│
├── REST_ASSURED_API_FRAMEWORK_SKILL.md           # Framework documentation
├── README.md                                     # This file
└── .gitignore

---

## 🚀 Projects & Components

### 1️⃣ RAG Explorer – Full-Stack RAG Application

**Location:** [`BasicRAG/`](./BasicRAG/)

A complete Retrieval-Augmented Generation system combining semantic search with LLM intelligence.

**What's Been Built:**
- ✅ **FastAPI Backend** with auto-ingestion pipeline for PDFs
- ✅ **React Frontend** with Tailwind CSS and dark theme
- ✅ **ChromaDB Integration** for vector storage and semantic search
- ✅ **Groq LLM API** integration for intelligent responses
- ✅ **Watchdog File Monitoring** for automatic PDF processing
- ✅ **Pipeline Visualization** showing retrieval → context → response stages
- ✅ **Comprehensive Documentation** and troubleshooting guides

**Tech Stack:**
- Backend: Python 3.13, FastAPI, ChromaDB, Sentence-Transformers, Groq API, Watchdog
- Frontend: React 18, Vite, Tailwind CSS
- Database: ChromaDB (local vector storage)

**API Endpoints:**
- `GET /api/health` - Health check
- `GET /api/stats` - Indexing statistics (chunks, sources)
- `POST /api/query` - Submit query with RAG response
- `POST /api/reindex` - Manual PDF folder reprocessing

**Quick Start:**
```bash
# Backend
cd BasicRAG/backend
.\.venv\Scripts\python -m uvicorn main:app --reload

# Frontend (new terminal)
cd BasicRAG/frontend
npm run dev
````

**Access:** http://localhost:5173

**See:** [BasicRAG/README.md](./BasicRAG/README.md) for complete documentation

---

### 2️⃣ REST Assured API Testing Framework

**Location:** [`RestAssuredAPIFramework/`](./RestAssuredAPIFramework/)

Enterprise-grade REST API testing framework with layered architecture and comprehensive test coverage.

**What's Been Built:**

- ✅ **Multi-layered Architecture** (Base → Config → Utils → Tests)
- ✅ **Complete CRUD Test Coverage** (GET, POST, PUT, DELETE endpoints)
- ✅ **Data-driven Testing** with TestNG @DataProvider patterns
- ✅ **Comprehensive Logging** with Log4j2 configuration
- ✅ **Advanced Error Handling** and validation frameworks
- ✅ **End-to-end Workflow Tests** for business scenarios
- ✅ **SOLID Principles** applied throughout the architecture

**Tech Stack:**

- Language: Java 11+
- Build Tool: Maven
- Test Framework: TestNG
- API Library: Rest Assured 5.x
- Assertions: Hamcrest + AssertJ
- Logging: Log4j2
- Data Source: JSONPlaceholder API

**Test Coverage:**

- GET tests (200, 404 responses)
- POST tests (201, 400 validations)
- PUT tests (full-object updates)
- DELETE tests (removal with cleanup)
- End-to-end CRUD flows

**Run Tests:**

```bash
cd RestAssuredAPIFramework
mvn clean test
mvn test -Dgroups=smoke
```

**See:** [RestAssuredAPIFramework/README.md](./RestAssuredAPIFramework/README.md) for detailed documentation

**Architecture Details:**

- **Location:** `RestAssuredAPIFramework/`
- **Directory Structure:**

  ```
  src/main/java/com/apitests/
  ├── base/              # BaseTest.java
  ├── config/            # ConfigReader.java
  ├── constants/         # ApiConstants.java, StatusCodes.java
  ├── models/            # Post.java, User.java
  └── utils/             # RestUtils.java, JsonUtils.java, ReportManager.java

  src/test/java/com/apitests/
  ├── get/               # GET endpoint tests
  ├── post/              # POST endpoint tests
  ├── put/               # PUT endpoint tests
  ├── delete/            # DELETE endpoint tests
  └── endtoend/          # CrudFlowTest.java
  ```

---

### 3️⃣ Langflow AI Automation Agents

**Location:** [`Langflow/`](./Langflow/)

Pre-configured AI agents for test automation using LLM integration with Langflow visual editor.

**What's Been Built:**

| Agent                        | Purpose                                                  |
| ---------------------------- | -------------------------------------------------------- |
| **bug-triage-agent.json**    | Automatically categorizes and prioritizes bugs using LLM |
| **flaky-test-analyzer.json** | Analyzes flaky test patterns and suggests improvements   |
| **rca-bot.json**             | Root Cause Analysis bot for test failure investigation   |
| **simple-agent.json**        | General-purpose conversational Q&A agent                 |
| **test-case-creator.json**   | Auto-generates test cases from requirements              |
| **test-plan-generator.json** | Creates comprehensive test plans using AI                |

**Features:**

- ✅ Production-ready agent configurations
- ✅ LLM-powered test analysis and generation
- ✅ Visual workflow design support
- ✅ Integration-ready for automation pipelines

**How to Use:**

1. Import JSON files into Langflow
2. Configure API keys and LLM endpoints
3. Deploy agents for test automation workflows

---

### 4️⃣ n8n Workflow Automation

**Location:** [`n8n/`](./n8n/)

Advanced workflow automation for complex processes with AI integration.

**What's Been Built:**

**ai-video-editor.json** - AI-powered video processing workflow

**Features:**

- ✅ Automated video scanning and metadata extraction
- ✅ Frame extraction and quality analysis using GPT-4o
- ✅ Intelligent clip trimming based on quality scores
- ✅ Multi-clip ranking and merging
- ✅ Final video validation and output

**Technology:**

- Integration: OpenAI GPT-4o for video quality assessment
- Processing: FFmpeg for video manipulation
- Automation: n8n workflow orchestration

**How to Use:**

1. Import JSON workflow into n8n
2. Set up video input/output directories
3. Configure OpenAI API credentials
4. Deploy and trigger the workflow

---

### 5️⃣ n8n RAG Workflow – AgenticAI RAG Pipeline

**Location:** [`n8nBasicRAGWorkflow/`](./n8nBasicRAGWorkflow/)

A complete Retrieval-Augmented Generation (RAG) system built with n8n that combines document ingestion, semantic search, and AI-powered responses.

**What's Been Built:**

- ✅ **Document Ingestion Pipeline** - Upload multiple file formats (PDF, TXT, CSV, DOCX, JSON)
- ✅ **Automatic Text Chunking** - Recursive character-based splitting with overlap
- ✅ **Vector Embeddings** - OpenAI embeddings for semantic search
- ✅ **Pinecone Integration** - Scalable vector database storage and retrieval
- ✅ **AI Agent Orchestration** - RAG agent powered by GPT-5-mini
- ✅ **Conversation Memory** - 3-message buffer window for context retention
- ✅ **Document-Grounded Responses** - AI only answers based on uploaded documents
- ✅ **Safety Guardrails** - Proper handling when answers aren't in documents

**Architecture:**

**Two-Stage Pipeline:**

1. **Ingestion Stage:**
   - Form submission trigger for file uploads
   - Document parsing and text extraction
   - Text chunking with 200-character overlap
   - Embedding generation using OpenAI
   - Storage in Pinecone vector index

2. **RAG Stage:**
   - Chat interface for user queries
   - Query embedding for semantic matching
   - Retrieval of top-3 most relevant documents
   - AI agent response generation with context
   - Memory-aware multi-turn conversations

**Tech Stack:**

- **Vector DB:** Pinecone (testpdf index)
- **LLM:** OpenAI GPT-5-mini
- **Embeddings:** OpenAI Embeddings
- **Memory:** Simple Buffer (3-message window)
- **Retrieval:** Top-K semantic search (k=3)
- **Workflow Engine:** n8n

**Key Features:**

- 🔄 **Multi-format Support:** PDF, TXT, CSV, DOCX, JSON
- 🎯 **Semantic Search:** Intelligent document retrieval using vectors
- 💬 **Context-Aware:** Grounds all answers in uploaded documents
- 🧠 **Conversation Memory:** Multi-turn dialogue with context retention
- 🛡️ **Safety:** Clear error messages when documents don't contain answers
- 📈 **Scalable:** Handles large document collections via Pinecone

**Use Cases:**

- Document Q&A systems
- Knowledge base chatbots
- Internal documentation assistants
- Research and information extraction
- Customer support automation

**How to Use:**

1. Import `AgenticAI_RAGWorkflow.json` into n8n
2. Configure credentials:
   - OpenAI API key (for embeddings and chat)
   - Pinecone API key and index name
3. Deploy the workflow
4. Access upload form and chat interface via n8n webhooks
5. Upload documents and start asking questions

**See:** [n8nBasicRAGWorkflow/README.md](./n8nBasicRAGWorkflow/README.md) for complete workflow documentation

---

## 📚 Documentation & Resources

### Setup & Installation

- [Installation and Set Up Guide](./Installation%20and%20Set%20Up/README.md) - Complete environment setup

### Learning Resources

- [LLM Basics and AI Testing Tools](./LLM%20Basics%20and%20AI%20Testing%20Tools/README.md) - AI fundamentals and tooling

### Framework Documentation

- [REST Assured Framework Skill Documentation](./REST_ASSURED_API_FRAMEWORK_SKILL.md) - Detailed framework guide

---

## ✨ Key Accomplishments

### Architecture & Design

- ✅ Multi-project monorepo structure with clear separation of concerns
- ✅ Layered architecture patterns applied across frameworks
- ✅ Production-grade code following SOLID principles
- ✅ Comprehensive error handling and validation

### Full-Stack Development

- ✅ Backend systems: Python FastAPI, Java REST frameworks
- ✅ Frontend UI: Modern React with Vite and Tailwind CSS
- ✅ Database integration: ChromaDB vector storage
- ✅ API design: RESTful endpoints with proper HTTP status codes

### AI & Automation Integration

- ✅ LLM integration with Groq API
- ✅ Langflow agent configuration for test automation
- ✅ n8n workflow orchestration with AI capabilities
- ✅ Semantic search using embeddings

### DevOps & Quality

- ✅ Git version control with proper .gitignore configuration
- ✅ Maven build automation with dependency management
- ✅ npm package management for Node.js projects
- ✅ Comprehensive documentation and guides

---

## 🛠️ Getting Started

### Prerequisites

- **Java 11+** with Maven (for REST API framework)
- **Python 3.13+** with pip (for RAG Explorer)
- **Node.js 18+** with npm (for React frontend)
- **Git** for version control

### Quick Setup

**RAG Explorer:**

```bash
cd BasicRAG
# See BasicRAG/README.md for detailed setup
```

**REST Assured Tests:**

```bash
cd RestAssuredAPIFramework
mvn clean install
mvn test
```

**Langflow Agents:**

- Import JSON files from `Langflow/` directory
- Configure in Langflow UI
- Deploy and use

**n8n Workflows:**

- Import JSON from `n8n/` directory
- Configure credentials in n8n
- Deploy and trigger

---

## 📊 Technology Stack Summary

| Layer                   | Technology                            |
| ----------------------- | ------------------------------------- |
| **Backend (RAG)**       | Python 3.13, FastAPI, Uvicorn         |
| **Backend (API Tests)** | Java 11+, Maven, Rest Assured, TestNG |
| **Frontend**            | React 18, Vite, Tailwind CSS          |
| **Databases**           | ChromaDB (vectors)                    |
| **LLM**                 | Groq API, Sentence-Transformers       |
| **File Processing**     | PyPDF, Watchdog                       |
| **Automation**          | Langflow, n8n                         |
| **Deployment**          | Docker-ready (FastAPI)                |

---

## 📝 Project Status

| Component              | Status      | Version |
| ---------------------- | ----------- | ------- |
| RAG Explorer           | ✅ Complete | 1.0     |
| REST Assured Framework | ✅ Complete | 1.0     |
| Langflow Agents        | ✅ Complete | 1.0     |
| n8n AI Video Editor    | ✅ Complete | 1.0     |
| n8n AgenticAI RAG      | ✅ Complete | 1.0     |

---

## 🔗 Links & References

- [The Testing Academy](https://courses.thetestingacademy.com/mycourses) - Course reference
- [FastAPI Documentation](https://fastapi.tiangolo.com/)
- [Rest Assured](https://rest-assured.io/)
- [Langflow](https://www.langflow.org/)
- [n8n](https://n8n.io/)
- [React Documentation](https://react.dev/)
- [ChromaDB](https://www.trychroma.com/)
- [Groq API](https://console.groq.com/)

---

## 📞 Support & Troubleshooting

Each project includes detailed README and troubleshooting guides:

- [BasicRAG Troubleshooting](./BasicRAG/README.md#-troubleshooting)
- [REST Framework Guide](./RestAssuredAPIFramework/README.md)
- [n8n RAG Workflow Guide](./n8nBasicRAGWorkflow/README.md)
- [Installation Guide](./Installation%20and%20Set%20Up/README.md)

---

**Last Updated:** July 5, 2024  
**Repository:** [AITester3x on GitHub](https://github.com/TarunBabbar/AITester3x)  
**Status:** ✅ All projects operational and documented
