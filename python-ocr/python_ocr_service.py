import base64
import os

import requests
from fastapi import FastAPI, HTTPException
from pydantic import BaseModel


API_URL = os.getenv("PADDLE_LAYOUT_API_URL", "https://95q8z9fflep806s7.aistudio-app.com/layout-parsing")
TOKEN = os.getenv("PADDLE_LAYOUT_API_TOKEN", "9bc4642331c4a2bf69057eacd5f651d475813fcc")
REQUEST_TIMEOUT = int(os.getenv("PADDLE_LAYOUT_TIMEOUT", "120"))

app = FastAPI(title="Python OCR Service")


class ParseRequest(BaseModel):
    file: str
    fileName: str | None = None
    contentType: str | None = None


def detect_file_type(file_name: str, content_type: str | None) -> int:
    lower_name = (file_name or "").lower()
    normalized_type = (content_type or "").lower()
    if lower_name.endswith(".pdf") or normalized_type == "application/pdf":
        return 0
    return 1


def merge_markdown_pages(layout_results: list[dict]) -> str:
    pages = []
    for result in layout_results:
        markdown = result.get("markdown", {}) or {}
        text = (markdown.get("text") or "").strip()
        if text:
            pages.append(text)
    return "\n\n---\n\n".join(pages).strip()


def call_layout_api(file_bytes: bytes, file_type: int) -> dict:
    if not TOKEN:
        raise HTTPException(status_code=500, detail="Missing PADDLE_LAYOUT_API_TOKEN")

    payload = {
        "file": base64.b64encode(file_bytes).decode("ascii"),
        "fileType": file_type,
        "useDocOrientationClassify": False,
        "useDocUnwarping": False,
        "useChartRecognition": False,
    }
    headers = {
        "Authorization": f"token {TOKEN}",
        "Content-Type": "application/json",
    }
    response = requests.post(API_URL, json=payload, headers=headers, timeout=REQUEST_TIMEOUT)
    response.raise_for_status()
    body = response.json()
    result = body.get("result")
    if not result:
        raise HTTPException(status_code=502, detail="Python OCR upstream returned empty result")
    return result


@app.get("/health")
def health() -> dict:
    return {"ok": True}


@app.post("/parse")
async def parse_document(request: ParseRequest) -> dict:
    try:
        file_bytes = base64.b64decode(request.file)
        result = call_layout_api(file_bytes, detect_file_type(request.fileName or "", request.contentType))
        layout_results = result.get("layoutParsingResults") or []
        markdown = merge_markdown_pages(layout_results)
        return {
            "success": True,
            "text": markdown,
            "markdown": markdown,
            "pageCount": len(layout_results),
            "fileName": request.fileName,
        }
    except requests.HTTPError as ex:
        detail = ex.response.text if ex.response is not None else str(ex)
        raise HTTPException(status_code=502, detail=detail) from ex
    except HTTPException:
        raise
    except Exception as ex:
        raise HTTPException(status_code=500, detail=str(ex)) from ex


if __name__ == "__main__":
    import uvicorn

    uvicorn.run(app, host="127.0.0.1", port=8000)
