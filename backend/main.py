from fastapi import FastAPI, HTTPException, status
from fastapi.middleware.cors import CORSMiddleware
from schemas import AnalyzeFormRequest, AiAnalysisResponse
from gemini_service import GeminiService

app = FastAPI(
    title="Pulse AI Exercise Form & Validation API",
    description="Authoritative exercise recognition, rep verification, and form analysis backend powered by Gemini 3.7 Flash.",
    version="1.0.0"
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

gemini_service = GeminiService()


@app.get("/")
def health_check():
    return {
        "status": "online",
        "service": "Pulse AI Exercise Form Analyzer",
        "model": gemini_service.model_name
    }


@app.post("/analyze-form", response_model=AiAnalysisResponse)
def analyze_form(request: AnalyzeFormRequest):
    if not request.exercise:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Exercise field is required."
        )
    if not request.frames_base64:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="frames_base64 array cannot be empty."
        )

    response = gemini_service.analyze_sequence(
        exercise=request.exercise.lower(),
        frames_base64=request.frames_base64
    )
    return response
