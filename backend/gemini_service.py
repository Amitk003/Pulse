import os
import io
import base64
import json
from typing import List, Optional
from PIL import Image
from dotenv import load_dotenv
from schemas import AiAnalysisResponse, BodyVisibilityEnum, CurrentFormEnum, RepStateEnum, FormChecks

load_dotenv()

SYSTEM_PROMPT = """You are the authoritative movement and exercise-form judge for Pulse.
Analyze the provided sequential movement frames representing a short temporal window (~1 to 2 seconds).

CORE RULES:
1. Movement alone DOES NOT constitute a repetition.
2. Distinguish intentional exercise movement from unrelated movements.
   - SITTING ON A COUCH, STANDING FROM A COUCH, BENDING OVER, HEAD MOVEMENTS, LOOKING DOWN, WALKING, OR RANDOM ARM/BODY MOVEMENTS ARE NOT REPS (flag anomaly_detected=true).
3. Check body visibility carefully:
   - If required body parts for the exercise are not visible or out of frame, set body_visibility="INSUFFICIENT" or "PARTIALLY_VISIBLE", camera_usable=false, and give clear feedback (e.g., "Move farther back so your full body is visible").
4. Verify Exercise Identity:
   - Check if the person is executing the requested exercise (squat, push-up, hinge, lunge, or jumping-jack).
   - If the person is doing a different exercise or non-exercise movement, set exercise_match=false.
5. Validate Repetition Completeness:
   - A valid rep requires: READY phase -> DESCENDING/OPENING phase -> BOTTOM/PEAK depth -> ASCENDING/CLOSING phase -> RETURN TO STARTING POSITION.
   - Partial/shallow depth or incomplete return = rep_complete=false, rep_state="INVALID" or "CANDIDATE".
6. Form Quality:
   - Evaluate range_of_motion, alignment, stability, and tempo.
   - Provide concise, actionable feedback.

You MUST respond ONLY with a valid JSON object matching this schema:
{
  "exercise": "requested exercise name",
  "exercise_confidence": float (0.0 to 1.0),
  "person_visible": boolean,
  "body_visibility": "FULL_BODY_VISIBLE" | "PARTIALLY_VISIBLE" | "INSUFFICIENT" | "UNKNOWN",
  "camera_usable": boolean,
  "camera_warning": string or null,
  "movement_detected": boolean,
  "movement_phase": "READY" | "DESCENDING" | "BOTTOM" | "ASCENDING" | "COMPLETED" | "UNKNOWN",
  "exercise_match": boolean,
  "exercise_match_confidence": float (0.0 to 1.0),
  "current_form": "GOOD" | "WARNING" | "BAD" | "UNKNOWN",
  "form_score": int (0 to 100),
  "form_confidence": float (0.0 to 1.0),
  "rep_state": "CANDIDATE" | "COMPLETED" | "INVALID",
  "rep_likelihood": float (0.0 to 1.0),
  "rep_complete": boolean,
  "anomaly_detected": boolean,
  "anomaly_type": string or null (e.g., "couch_sitting", "head_movement", "random_motion"),
  "form_checks": {
    "range_of_motion": "GOOD" | "PARTIAL" | "POOR",
    "alignment": "GOOD" | "WARNING" | "POOR",
    "stability": "GOOD" | "UNSTABLE",
    "tempo": "GOOD" | "TOO_FAST" | "TOO_SLOW"
  },
  "errors": [string],
  "feedback": "Concise coach advice"
}
"""

class GeminiService:

    def __init__(self):
        self.api_key = os.getenv("GEMINI_API_KEY", "")
        self.model_name = os.getenv("GEMINI_MODEL", "gemini-3.7-flash")
        self._client = None

    def _get_client(self):
        if not self._client:
            if not self.api_key:
                raise ValueError("GEMINI_API_KEY is not set in backend environment (.env).")
            try:
                from google import genai
                self._client = genai.Client(api_key=self.api_key)
            except ImportError:
                # Fallback if google-genai package is loaded differently
                import google.generativeai as legacy_genai
                legacy_genai.configure(api_key=self.api_key)
                self._client = legacy_genai
        return self._client

    def analyze_sequence(self, exercise: str, frames_base64: List[str]) -> AiAnalysisResponse:
        if not frames_base64:
            return AiAnalysisResponse(
                exercise=exercise,
                person_visible=False,
                body_visibility=BodyVisibilityEnum.INSUFFICIENT,
                camera_usable=False,
                camera_warning="No camera frames provided.",
                errors=["empty_frames_input"],
                feedback="No video frames received."
            )

        images = []
        for idx, b64_str in enumerate(frames_base64):
            try:
                if "," in b64_str:
                    b64_str = b64_str.split(",", 1)[1]
                img_bytes = base64.b64decode(b64_str)
                img = Image.open(io.BytesIO(img_bytes))
                images.append(img)
            except Exception as e:
                print(f"Error decoding image frame {idx}: {e}")

        if not images:
            return AiAnalysisResponse(
                exercise=exercise,
                person_visible=False,
                body_visibility=BodyVisibilityEnum.UNKNOWN,
                camera_usable=False,
                camera_warning="Invalid image encoding.",
                errors=["image_decode_failed"],
                feedback="Failed to process camera images."
            )

        prompt = f"Requested Exercise: {exercise}\nNumber of Sequential Frames: {len(images)}\nAnalyze this movement sequence against the system rules."
        
        models_to_try = [
            self.model_name,
            "gemini-3.6-flash",
            "gemini-2.5-flash"
        ]
        # Remove duplicates while preserving order
        seen = set()
        models_to_try = [m for m in models_to_try if not (m in seen or seen.add(m))]

        last_exception = None
        for model in models_to_try:
            try:
                client = self._get_client()
                if hasattr(client, "models"):
                    contents = [SYSTEM_PROMPT, prompt] + images
                    response = client.models.generate_content(
                        model=model,
                        contents=contents,
                        config={"response_mime_type": "application/json"}
                    )
                    text_response = response.text
                else:
                    gen_model = client.GenerativeModel(
                        model_name=model,
                        system_instruction=SYSTEM_PROMPT
                    )
                    contents = [prompt] + images
                    response = gen_model.generate_content(contents)
                    text_response = response.text

                return self._parse_response(exercise, text_response)

            except Exception as e:
                last_exception = e
                print(f"Gemini API model {model} warning: {e}")

        # If all models failed, return graceful degraded schema
        return AiAnalysisResponse(
            exercise=exercise,
            person_visible=True,
            body_visibility=BodyVisibilityEnum.UNKNOWN,
            camera_usable=True,
            camera_warning=f"AI service temporarily degraded: {str(last_exception)}",
            errors=[str(last_exception)],
            feedback="AI service currently unavailable."
        )

    def _parse_response(self, exercise: str, raw_text: str) -> AiAnalysisResponse:
        try:
            cleaned = raw_text.strip()
            if cleaned.startswith("```json"):
                cleaned = cleaned[7:]
            if cleaned.startswith("```"):
                cleaned = cleaned[3:]
            if cleaned.endswith("```"):
                cleaned = cleaned[:-3]
            cleaned = cleaned.strip()

            data = json.loads(cleaned)
            return AiAnalysisResponse(**data)
        except Exception as parse_err:
            print(f"Error parsing Gemini response JSON: {parse_err}. Raw text: {raw_text[:200]}")
            return AiAnalysisResponse(
                exercise=exercise,
                person_visible=True,
                body_visibility=BodyVisibilityEnum.UNKNOWN,
                camera_usable=True,
                errors=[f"json_parse_error: {str(parse_err)}"],
                feedback="AI response format invalid."
            )
