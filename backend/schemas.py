from enum import Enum
from typing import List, Optional, Dict
from pydantic import BaseModel, Field


class BodyVisibilityEnum(str, Enum):
    FULL_BODY_VISIBLE = "FULL_BODY_VISIBLE"
    PARTIALLY_VISIBLE = "PARTIALLY_VISIBLE"
    INSUFFICIENT = "INSUFFICIENT"
    UNKNOWN = "UNKNOWN"


class CurrentFormEnum(str, Enum):
    GOOD = "GOOD"
    WARNING = "WARNING"
    BAD = "BAD"
    UNKNOWN = "UNKNOWN"


class RepStateEnum(str, Enum):
    CANDIDATE = "CANDIDATE"
    COMPLETED = "COMPLETED"
    INVALID = "INVALID"


class FormChecks(BaseModel):
    range_of_motion: str = "UNKNOWN"
    alignment: str = "UNKNOWN"
    stability: str = "UNKNOWN"
    tempo: str = "UNKNOWN"


class AiAnalysisResponse(BaseModel):
    exercise: str
    exercise_confidence: float = Field(default=0.0, ge=0.0, le=1.0)
    person_visible: bool = False
    body_visibility: BodyVisibilityEnum = BodyVisibilityEnum.UNKNOWN
    camera_usable: bool = True
    camera_warning: Optional[str] = None
    movement_detected: bool = False
    movement_phase: str = "UNKNOWN"
    exercise_match: bool = False
    exercise_match_confidence: float = Field(default=0.0, ge=0.0, le=1.0)
    current_form: CurrentFormEnum = CurrentFormEnum.UNKNOWN
    form_score: int = Field(default=0, ge=0, le=100)
    form_confidence: float = Field(default=0.0, ge=0.0, le=1.0)
    rep_state: RepStateEnum = RepStateEnum.INVALID
    rep_likelihood: float = Field(default=0.0, ge=0.0, le=1.0)
    rep_complete: bool = False
    anomaly_detected: bool = False
    anomaly_type: Optional[str] = None
    form_checks: FormChecks = Field(default_factory=FormChecks)
    errors: List[str] = Field(default_factory=list)
    feedback: str = ""


class AnalyzeFormRequest(BaseModel):
    exercise: str
    frames_base64: List[str]
    timestamp_ms: Optional[int] = None
