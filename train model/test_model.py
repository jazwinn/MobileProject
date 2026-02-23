from ultralytics import YOLO

# 1. Load your trained model
model = YOLO('gym_machines.pt')

# 2. Run inference
results = model('gym_photo.jpg')

# 3. Get the names mapping from the model itself
# This ensures "0" always maps to "Chest Press machine"
class_names = model.names

for r in results:
    for box in r.boxes:
        class_id = int(box.cls[0])
        label = class_names[class_id]
        confidence = box.conf[0]
        print(f"Detected: {label} with {confidence:.2f} confidence")