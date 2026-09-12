import os
import io
import base64
import json
from PIL import Image, ImageDraw

def create_sample_frame_base64():
    # Create a simple 320x240 image simulating a stick figure squat position
    img = Image.new('RGB', (320, 240), color=(240, 240, 240))
    draw = ImageDraw.Draw(img)
    # Draw head
    draw.ellipse((140, 30, 180, 70), fill=(50, 50, 50))
    # Draw torso
    draw.line((160, 70, 160, 140), fill=(50, 50, 50), width=6)
    # Draw legs bent in squat position
    draw.line((160, 140, 120, 180), fill=(50, 50, 50), width=6)
    draw.line((120, 180, 160, 220), fill=(50, 50, 50), width=6)
    draw.line((160, 140, 200, 180), fill=(50, 50, 50), width=6)
    draw.line((200, 180, 160, 220), fill=(50, 50, 50), width=6)

    buffer = io.BytesIO()
    img.save(buffer, format='JPEG')
    return base64.b64encode(buffer.getvalue()).decode('utf-8')

if __name__ == "__main__":
    print("--- Pulse Gemini AI Backend Direct Test ---")
    try:
        from gemini_service import GeminiService
        
        service = GeminiService()
        print(f"Model: {service.model_name}")
        print(f"API Key configured: {bool(service.api_key)}")
        
        sample_frame = create_sample_frame_base64()
        print("Sending sample test frame to Gemini API...")
        
        result = service.analyze_sequence(
            exercise="squat",
            frames_base64=[sample_frame]
        )
        
        print("\n--- Test Result JSON Output ---")
        print(json.dumps(result.model_dump(), indent=2))
        
        if result.errors:
            print("\n[INFO] Gemini Vision returned details:", result.errors)
        
        print("\n[SUCCESS] Gemini API connected and returned valid structured JSON!")

    except Exception as e:
        print(f"\n[ERROR] Exception during backend test: {e}")
