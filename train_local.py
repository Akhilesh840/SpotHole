from ultralytics import YOLO

def main():
    # Load our previously trained custom model to fine-tune on the new data
    model = YOLO('d:/Khadda/Khadda/runs/detect/runs/detect/train_local/weights/best.pt')

    # Train the model locally on the provided dataset
    print("Starting local training (Fine-tuning on combined dataset)...")
    results = model.train(
        data='d:/Khadda/Khadda/Dataset/data.yaml',
        epochs=50,
        imgsz=640,
        project='runs/detect',
        name='train_local_50epochs',
        workers=0
    )
    print("Training complete!")

if __name__ == '__main__':
    main()
