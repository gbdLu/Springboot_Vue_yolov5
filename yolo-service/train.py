# train.py（放在 yolo-service 目录下）
import subprocess
import os

# 获取当前脚本所在目录
script_dir = os.path.dirname(os.path.abspath(__file__))
os.chdir(script_dir)  # 切换到脚本所在目录

cmd = [
    "python",
    "yolov5/train.py",
    "--img", "640",
    "--batch", "4",
    "--epochs", "100",
    "--data", "data/dataset.yaml",
    "--weights", "yolov5s.pt",
    "--project", "runs/train",
    "--name", "exp",
    "--exist-ok"
]

subprocess.run(cmd)
print("训练完成！模型保存在 runs/train/exp/weights/best.pt")