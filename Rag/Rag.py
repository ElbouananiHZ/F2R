from pypdf import PdfReader
from langchain.text_splitter import RecursiveCharacterTextSplitter
from langchain_chroma import Chroma
from langchain_huggingface import HuggingFaceEmbeddings
from langchain_ollama import OllamaLLM
from langchain.chains import RetrievalQA
from langchain.prompts import PromptTemplate
from dotenv import load_dotenv
import os
from typing import List
from pathlib import Path
import smtplib
from email.mime.text import MIMEText
from email.mime.multipart import MIMEMultipart

# Load environment variables
load_dotenv()

class RAGSystem:
    def __init__(
        self,
        model_name: str = "phi",
        embedding_model: str = "all-MiniLM-L6-v2",
        persist_directory: str = "./chroma_db",
        chunk_size: int = 500,
        chunk_overlap: int = 100
    ):
        self.model_name = model_name
        self.embedding_model = embedding_model
        self.persist_directory = Path(persist_directory)
        self.chunk_size = chunk_size
        self.chunk_overlap = chunk_overlap
        self.db = None
        self.qa_chain = None

        self.sender_email = os.getenv("SENDER_EMAIL")
        self.sender_password = os.getenv("SENDER_PASSWORD")
        self.receiver_email = os.getenv("RECEIVER_EMAIL")

        if not all([self.sender_email, self.sender_password, self.receiver_email]):
            raise EnvironmentError("Missing email credentials in .env file.")

        self.persist_directory.mkdir(parents=True, exist_ok=True)

        print(f"Initializing embedding model: {self.embedding_model}")
        self.embedding = HuggingFaceEmbeddings(model_name=self.embedding_model)

        self.splitter = RecursiveCharacterTextSplitter(
            chunk_size=self.chunk_size,
            chunk_overlap=self.chunk_overlap,
            length_function=len,
            separators=["\n\n", "\n", ".", "!", "?", ",", " ", ""]
        )

    def extract_text_from_pdf(self, pdf_path: str) -> str:
        if not os.path.exists(pdf_path):
            raise FileNotFoundError(f"PDF file not found: {pdf_path}")

        print(f"Reading PDF: {pdf_path}")
        reader = PdfReader(pdf_path)
        
        if len(reader.pages) == 0:
            raise ValueError(f"PDF file is empty: {pdf_path}")

        print(f"Number of pages in PDF: {len(reader.pages)}")
        text = ""
        for i, page in enumerate(reader.pages):
            try:
                page_text = page.extract_text()
                if not page_text.strip():
                    print(f"Warning: Page {i+1} extracted empty text")
                text += page_text + "\n"
            except Exception as e:
                print(f"Error extracting text from page {i+1}: {str(e)}")
                continue

        if not text.strip():
            raise ValueError("No text could be extracted from the PDF")

        print(f"Successfully extracted {len(text)} characters of text")
        return text

    def train(self, pdf_paths: List[str]) -> None:
        all_documents = []
        for pdf_path in pdf_paths:
            try:
                text = self.extract_text_from_pdf(pdf_path)
                print("Splitting text into documents...")
                documents = self.splitter.create_documents([text])
                print(f"Created {len(documents)} document chunks")
                
                if not documents:
                    raise ValueError("No documents were created from the text")
                
                all_documents.extend(documents)
            except Exception as e:
                print(f"Error processing {pdf_path}: {str(e)}")
                raise

        print(f"Total documents to process: {len(all_documents)}")
        
        try:
            if os.path.exists(self.persist_directory):
                print("Loading existing database...")
                self.db = Chroma(
                    persist_directory=str(self.persist_directory),
                    embedding_function=self.embedding
                )
                print("Adding new documents...")
                self.db.add_documents(all_documents)
            else:
                print("Creating new database...")
                self.db = Chroma.from_documents(
                    all_documents,
                    self.embedding,
                    persist_directory=str(self.persist_directory)
                )
        except Exception as e:
            print(f"Error during database operations: {str(e)}")
            raise

        print("Initializing LLM...")
        llm = OllamaLLM(
            model=self.model_name,
            temperature=0.3,
            num_ctx=4096,
            num_predict=512,
            repeat_penalty=1.1
        )

        prompt_template = """You are a helpful AI assistant. Answer the question based ONLY on the provided context.
Rules:
1. Keep your answers concise and to the point
2. Only use information from the provided context
3. If you don't know the answer, just say "I don't know"
4. Do not repeat the entire context
5. Focus on the specific question
6. Use bullet points if needed
7. Max 2-3 sentences unless necessary

Context: {context}

Question: {question}

Answer: """

        PROMPT = PromptTemplate(
            template=prompt_template, input_variables=["context", "question"]
        )

        print("Setting up QA chain...")
        self.qa_chain = RetrievalQA.from_chain_type(
            llm=llm,
            retriever=self.db.as_retriever(search_kwargs={"k": 3}),
            chain_type="stuff",
            return_source_documents=False,
            chain_type_kwargs={"prompt": PROMPT}
        )
        print("Training complete!")

    def send_notification_email(self, question: str) -> None:
        try:
            smtp_server = "smtp.gmail.com"
            smtp_port = 587

            msg = MIMEMultipart()
            msg['From'] = self.sender_email
            msg['To'] = self.receiver_email
            msg['Subject'] = "Unanswered Question from User"

            body = f"""
A user asked a question that could not be answered by the assistant:

Question: {question}

Please check if more context should be added to the documents.
            """
            msg.attach(MIMEText(body, 'plain'))

            with smtplib.SMTP(smtp_server, smtp_port) as server:
                server.starttls()
                server.login(self.sender_email, self.sender_password)
                server.send_message(msg)
                print("Notification email sent.")

        except Exception as e:
            print(f"Failed to send notification email: {str(e)}")

    def query(self, question: str) -> str:
        if not self.qa_chain:
            raise ValueError("RAG system has not been trained yet!")

        result = self.qa_chain.invoke({"query": question})
        answer = result["result"]

        unknown_phrases = [
            "i don't know", "i do not know", "sorry", "no information", 
            "no answer", "cannot answer", "not enough information"
        ]

        if len(answer.strip()) < 5 or any(phrase in answer.lower() for phrase in unknown_phrases):
            self.send_notification_email(question)
            return "I don't know. Your question has been sent to the administration. Thank you for your patience."

        return answer

def main():
    rag = RAGSystem()

    pdf_paths = [
        r"C:\Users\hamza\Bureau\Rag\Rag2.pdf"
    ]

    try:
        print("Training the RAG system...")
        rag.train(pdf_paths)
        print("Training complete!")

        while True:
            question = input("\nQuestion: ").strip()
            if question.lower() in ['exit', 'quit']:
                print("Goodbye!")
                break

            if question.lower() in ['what is your name', 'who are you', 'hey', 'hi', 'hello']:
                print("Hello, I'm Find2Read assistant. How can I help you today?")
                continue

            if not question:
                continue

            try:
                answer = rag.query(question)
                print(f"Answer: {answer}")
            except Exception as e:
                print(f"Error during query: {str(e)}")

    except Exception as e:
        print(f"Error during training: {str(e)}")

if __name__ == "__main__":
    main()
