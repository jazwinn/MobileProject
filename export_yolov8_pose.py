"""
Export YOLOv8n-pose model to ONNX format for Android deployment.

Usage:
    pip install ultralytics
    python export_yolov8_pose.py

This will download yolov8n-pose.pt and export it to ONNX format,
placing the result in app/src/main/assets/yolov8n-pose.onnx
"""

import shutil
from pathlib import Path
from ultralytics import YOLO

def main():
    # Load the YOLOv8 nano pose model
    model = YOLO("yolov8n-pose.pt")
    
    # Export to ONNX format with 640x640 input size
    # opset=11 ensures compatibility with wider range of ONNX Runtimes
    model.export(format="onnx", imgsz=640, simplify=True, opset=11)
    
    # Move the exported file to assets
    src = Path("yolov8n-pose.onnx")
    dst = Path("app/src/main/assets/yolov8n-pose.onnx")
    dst.parent.mkdir(parents=True, exist_ok=True)
    
    shutil.move(str(src), str(dst))
    print(f"✅ Model exported to: {dst}")
    print(f"   File size: {dst.stat().st_size / 1024 / 1024:.1f} MB")

if __name__ == "__main__":
    main()
