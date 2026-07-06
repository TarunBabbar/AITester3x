# AgenticAI RAG Workflow

## Overview

This n8n workflow implements a **Retrieval-Augmented Generation (RAG)** system that allows users to upload documents and ask questions about them using AI. The system leverages OpenAI's language models and Pinecone's vector database to provide intelligent, document-grounded responses.

## How It Works

### Two Main Stages

#### Stage 1: Document Ingestion & Vectorization

This stage processes uploaded documents and prepares them for retrieval:

1. **File Upload** (`On form submission`)
   - Accepts multiple file formats: `.pdf`, `.txt`, `.csv`, `.docx`, `.json`
   - Captures file metadata (filename, upload timestamp)

2. **Document Loading** (`Default Data Loader`)
   - Loads and parses the uploaded files
   - Extracts text content from various file types

3. **Text Chunking** (`Recursive Character Text Splitter`)
   - Splits large documents into smaller, manageable chunks (with 200-character overlap)
   - Ensures context preservation between chunks

4. **Embedding Generation** (`Embeddings OpenAI - Small`)
   - Converts text chunks into vector embeddings using OpenAI's embedding model
   - Creates numerical representations of the document content

5. **Vector Storage** (`Pinecone Vector Store - Save PDF Data`)
   - Stores embeddings in Pinecone vector database under the "testpdf" index
   - Enables fast semantic search and retrieval

#### Stage 2: RAG Pipeline (Retrieval, Augment, Generate)

This stage handles user queries and generates intelligent responses:

1. **Chat Interface** (`When chat message received`)
   - Receives user questions via chat webhook
   - Triggers the RAG pipeline

2. **AI Agent** (`AI Agent - RAG Agent`)
   - Orchestrates the retrieval and generation process
   - System prompt instructs it to answer only based on retrieved documents
   - Uses available tools to search for relevant information

3. **Language Model** (`OpenAI Chat Model`)
   - Uses GPT-5-mini to generate responses
   - Receives context from retrieved documents
   - Maintains conversation context with memory

4. **Conversation Memory** (`Simple Memory`)
   - Keeps track of the last 3 interactions
   - Enables coherent multi-turn conversations

5. **Vector Retrieval Tool** (`Pinecone Vector Store`)
   - Retrieves the top 3 most relevant documents for each query
   - Provides semantic search capabilities based on similarity to the user's question
   - Passes retrieved context to the AI agent

6. **Query Embeddings** (`Embeddings OpenAI`)
   - Converts user queries into embeddings
   - Ensures queries are in the same vector space as stored documents

## Workflow Connections

```
Document Upload → Load → Split → Embed → Store in Pinecone
                                           ↓
User Query → Chat Trigger → AI Agent ← Retrieve from Pinecone
                ↓                           ↓
        Use Retrieved Docs → Generate Answer → User
```

## Key Features

✅ **Multi-Format Support**: Handles PDFs, text files, CSV, DOCX, and JSON  
✅ **Semantic Search**: Uses vector embeddings for intelligent document retrieval  
✅ **Context-Aware Responses**: Grounds answers in uploaded documents  
✅ **Conversation Memory**: Maintains context across multiple interactions  
✅ **Safety Guardrails**: AI only answers based on retrieved documents  
✅ **Scalable**: Pinecone backend supports large document collections

## Use Cases

- **Document Q&A**: Ask questions about uploaded PDFs or documents
- **Knowledge Base**: Build a searchable index of internal documentation
- **Intelligent Chat**: Create chatbots that reference specific documents
- **Research Assistant**: Extract information from multiple documents efficiently

## Configuration Details

| Component  | Model/Service              | Purpose                                |
| ---------- | -------------------------- | -------------------------------------- |
| Embeddings | OpenAI Embeddings          | Convert text to vectors                |
| LLM        | GPT-5-mini                 | Generate responses                     |
| Vector DB  | Pinecone (testpdf index)   | Store and retrieve document embeddings |
| Memory     | Simple Buffer (3 messages) | Track conversation history             |
| Retrieval  | Top-K (k=3)                | Return most relevant documents         |

## Prerequisites

- **n8n** workflow engine
- **OpenAI API Key** (for embeddings and chat model)
- **Pinecone Account** with active API key
- **Pinecone Index**: "testpdf" (pre-configured)

## Error Handling

If the AI cannot find relevant information in the documents, it responds with:

> "I cannot find the relevant information from the document."

This ensures users understand when queries fall outside the document scope.

## Future Enhancements

- Support for more vector databases (Weaviate, Milvus)
- Multiple index management
- Document metadata filtering
- Configurable chunk size and overlap
- Response feedback loop for model improvement
