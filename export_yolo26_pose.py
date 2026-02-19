"""
Export YOLOv8n-pose model to ONNX format for Android deployment.

Usage:
    pip install ultralytics
    python export_yolo26_pose.py

This will download yolov8n-pose.pt and export it to ONNX format,
placing the result in app/src/main/assets/yolov8n-pose.onnx
"""

import shutil
from pathlib import Path
from ultralytics import YOLO

def main():
    model = YOLO("yolo26n-pose.pt")

    # 3. Export for Android (and edge devices)
    # This converts the model into a .tflite file optimized for mobile processors
    print("Exporting to TensorFlow Lite...")
    model.export(format="tflite")
    
    # Move the exported file to assets
    src = Path("yolo26n-pose.tflite")
    dst = Path("app/src/main/assets/yolo26n-pose.tflite")
    dst.parent.mkdir(parents=True, exist_ok=True)
    
    shutil.move(str(src), str(dst))
    print(f"✅ Model exported to: {dst}")
    print(f"   File size: {dst.stat().st_size / 1024 / 1024:.1f} MB")

if __name__ == "__main__":
    main()
