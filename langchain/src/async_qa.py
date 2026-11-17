import asyncio
import os

from dotenv import load_dotenv
from langchain_openai import ChatOpenAI

load_dotenv()

llm = ChatOpenAI(
    model=os.getenv("LLM_MODEL"),
    api_key=os.getenv("OPENROUTER_API_KEY"),
    base_url=os.getenv("OPENROUTER_BASE_URL"),
)


async def ask(question: str) -> str:
    response = await llm.ainvoke(question)
    return response.content


async def main() -> None:
    question = input("Question: ")
    print(await ask(question))


asyncio.run(main())
