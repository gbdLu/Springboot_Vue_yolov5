# fastapi_app.py
# 使用 YOLOv5 原生方式加载模型（修复导入检查问题）

import os
import sys
import uuid
import base64
from io import BytesIO
from PIL import Image
import cv2
import numpy as np
import torch
from fastapi import FastAPI, File, UploadFile, Request
from fastapi.responses import HTMLResponse, JSONResponse
import uvicorn

app = FastAPI(title="林区火情 YOLOv5 识别服务")

# ============ 配置 ============
MODEL_PATH = "runs/train/exp/weights/best.pt"
if not os.path.exists(MODEL_PATH):
    MODEL_PATH = "yolov5s.pt"
    print(f"⚠️ 未找到训练模型，使用预训练模型: {MODEL_PATH}")

CONFIDENCE = 0.25
IOU = 0.45

# ============ 类别映射（3类）============
CLASS_NAMES = {
    0: "fire",
    1: "human",
    2: "smoke"
}

CLASS_COLORS = {
    "fire": "#FF0000",
    "human": "#00FF00",
    "smoke": "#FFA500"
}


# ============ 加载模型 ============
def load_model():
    """加载 YOLOv5 模型"""
    try:
        # 使用 torch.hub.load 加载
        model = torch.hub.load(
            'ultralytics/yolov5',
            'custom',
            path=MODEL_PATH,
            force_reload=False
        )
        model.conf = CONFIDENCE
        model.iou = IOU
        print(f"✅ 模型加载成功: {MODEL_PATH}")
        return model
    except Exception as e:
        print(f"❌ 模型加载失败: {e}")
        return None


model = load_model()


# ============ 推理函数 ============
def run_inference(image_path):
    """执行推理"""
    if model is None:
        return None, "模型未加载"

    try:
        # 使用 YOLOv5 的 AutoShape 直接推理
        results = model(image_path)

        # 解析结果
        detections = []
        df = results.pandas().xyxy[0]

        for _, row in df.iterrows():
            class_id = int(row['class'])
            # 如果模型类别不是我们预期的，尝试映射
            class_name = CLASS_NAMES.get(class_id, row['name'])
            confidence = float(row['confidence'])

            detections.append({
                "class_id": class_id,
                "class_name": class_name,
                "confidence": confidence,
                "bbox": [
                    float(row['xmin']),
                    float(row['ymin']),
                    float(row['xmax']),
                    float(row['ymax'])
                ]
            })

        return detections, None
    except Exception as e:
        import traceback
        traceback.print_exc()
        return None, str(e)


# ============ 工具函数：绘制检测框 ============
def draw_detections(image_path, detections):
    """在图片上绘制检测框，返回 base64 编码的图片"""
    img = cv2.imread(image_path)
    if img is None:
        return None
    img = cv2.cvtColor(img, cv2.COLOR_BGR2RGB)

    for det in detections:
        x1, y1, x2, y2 = det["bbox"]
        class_name = det["class_name"]
        confidence = det["confidence"]

        x1, y1, x2, y2 = int(x1), int(y1), int(x2), int(y2)
        color = CLASS_COLORS.get(class_name, "#FFFFFF")
        color_bgr = tuple(int(color[i:i + 2], 16) for i in (1, 3, 5))
        color_bgr = (color_bgr[2], color_bgr[1], color_bgr[0])

        cv2.rectangle(img, (x1, y1), (x2, y2), color_bgr, 2)
        label = f"{class_name} {confidence:.2f}"
        (label_w, label_h), _ = cv2.getTextSize(label, cv2.FONT_HERSHEY_SIMPLEX, 0.6, 2)
        cv2.rectangle(img, (x1, y1 - label_h - 5), (x1 + label_w + 10, y1), color_bgr, -1)
        cv2.putText(img, label, (x1 + 5, y1 - 5), cv2.FONT_HERSHEY_SIMPLEX, 0.6, (255, 255, 255), 2)

    pil_img = Image.fromarray(img)
    buffered = BytesIO()
    pil_img.save(buffered, format="JPEG", quality=90)
    img_base64 = base64.b64encode(buffered.getvalue()).decode("utf-8")
    return img_base64


# ============ API 接口 ============

@app.get("/", response_class=HTMLResponse)
async def index(request: Request):
    """返回前端页面"""
    html_content = """
    <!DOCTYPE html>
    <html>
    <head>
        <meta charset="UTF-8">
        <meta name="viewport" content="width=device-width, initial-scale=1.0">
        <title>林区火情识别 - YOLOv5</title>
        <style>
            * { margin: 0; padding: 0; box-sizing: border-box; }
            body {
                font-family: 'Segoe UI', Arial, sans-serif;
                background: #f0f2f5;
                padding: 20px;
                min-height: 100vh;
            }
            .container {
                max-width: 1200px;
                margin: 0 auto;
                background: white;
                border-radius: 16px;
                padding: 30px;
                box-shadow: 0 4px 20px rgba(0,0,0,0.1);
            }
            h1 { text-align: center; color: #1a3a2a; margin-bottom: 10px; font-size: 28px; }
            .subtitle { text-align: center; color: #888; margin-bottom: 25px; font-size: 14px; }
            .upload-area {
                border: 2px dashed #d0d7de;
                border-radius: 12px;
                padding: 40px;
                text-align: center;
                cursor: pointer;
                transition: all 0.3s;
                background: #fafbfc;
            }
            .upload-area:hover { border-color: #1a3a2a; background: #f6f8fa; }
            .upload-area.dragover { border-color: #1a3a2a; background: #e8f0fe; }
            .upload-area input[type="file"] { display: none; }
            .upload-icon { font-size: 48px; color: #1a3a2a; margin-bottom: 10px; }
            .upload-text { font-size: 16px; color: #555; }
            .upload-hint { font-size: 13px; color: #999; margin-top: 5px; }
            .btn-upload {
                display: inline-block;
                margin-top: 15px;
                padding: 12px 40px;
                background: #1a3a2a;
                color: white;
                border: none;
                border-radius: 8px;
                font-size: 16px;
                cursor: pointer;
                transition: background 0.3s;
            }
            .btn-upload:hover { background: #2d5a3d; }
            .btn-upload:disabled { background: #aaa; cursor: not-allowed; }
            .result-container { margin-top: 25px; display: none; }
            .result-container.show { display: block; }
            .image-grid {
                display: grid;
                grid-template-columns: 1fr 1fr;
                gap: 20px;
            }
            .image-box {
                background: #fafbfc;
                border-radius: 12px;
                padding: 15px;
                border: 1px solid #e1e4e8;
            }
            .image-box h3 { margin-bottom: 10px; font-size: 15px; color: #333; display: flex; align-items: center; gap: 8px; }
            .image-box img { width: 100%; border-radius: 8px; background: #f0f0f0; }
            .stats {
                margin-top: 20px;
                display: flex;
                gap: 20px;
                flex-wrap: wrap;
                padding: 15px;
                background: #f8f9fa;
                border-radius: 10px;
            }
            .stat-item { display: flex; align-items: center; gap: 8px; font-size: 14px; }
            .stat-item .dot { width: 12px; height: 12px; border-radius: 50%; display: inline-block; }
            .stat-item .count { font-weight: bold; font-size: 18px; }
            .loading { text-align: center; padding: 20px; display: none; }
            .loading.show { display: block; }
            .spinner {
                width: 40px;
                height: 40px;
                border: 4px solid #e1e4e8;
                border-top: 4px solid #1a3a2a;
                border-radius: 50%;
                animation: spin 1s linear infinite;
                margin: 0 auto 10px;
            }
            @keyframes spin { to { transform: rotate(360deg); } }
            .error-msg { color: #d32f2f; background: #fde8e8; padding: 12px; border-radius: 8px; margin-top: 15px; display: none; }
            .error-msg.show { display: block; }
            .model-status {
                text-align: center;
                font-size: 13px;
                color: #666;
                margin-bottom: 15px;
                padding: 8px;
                background: #f0f7f0;
                border-radius: 6px;
            }
            @media (max-width: 768px) { .image-grid { grid-template-columns: 1fr; } .container { padding: 15px; } }
        </style>
    </head>
    <body>
        <div class="container">
            <h1>🌲 林区火情智能识别</h1>
            <p class="subtitle">基于 YOLOv5 的明火、烟雾、人员检测 | 上传图片自动识别</p>
            <div class="model-status" id="modelStatus">✅ 模型已加载</div>

            <div class="upload-area" id="dropArea">
                <div class="upload-icon">📤</div>
                <div class="upload-text">拖拽图片到这里，或点击选择文件</div>
                <div class="upload-hint">支持 JPG / PNG / BMP，单张上传</div>
                <input type="file" id="fileInput" accept="image/*">
                <button class="btn-upload" id="uploadBtn">开始识别</button>
            </div>

            <div class="loading" id="loading">
                <div class="spinner"></div>
                <p>正在识别中，请稍候...</p>
            </div>

            <div class="error-msg" id="errorMsg"></div>

            <div class="result-container" id="resultContainer">
                <div class="image-grid">
                    <div class="image-box">
                        <h3>📷 原图</h3>
                        <img id="originalImage" src="" alt="原图">
                    </div>
                    <div class="image-box">
                        <h3>🎯 识别结果</h3>
                        <img id="resultImage" src="" alt="识别结果">
                    </div>
                </div>

                <div class="stats" id="statsContainer">
                    <div class="stat-item">
                        <span class="dot" style="background:#FF0000;"></span>
                        明火: <span class="count" id="fireCount">0</span>
                    </div>
                    <div class="stat-item">
                        <span class="dot" style="background:#00FF00;"></span>
                        人员: <span class="count" id="humanCount">0</span>
                    </div>
                    <div class="stat-item">
                        <span class="dot" style="background:#FFA500;"></span>
                        烟雾: <span class="count" id="smokeCount">0</span>
                    </div>
                    <div class="stat-item">
                        🔥 总计: <span class="count" id="totalCount">0</span>
                    </div>
                </div>
            </div>
        </div>

        <script>
            const dropArea = document.getElementById('dropArea');
            const fileInput = document.getElementById('fileInput');
            const uploadBtn = document.getElementById('uploadBtn');
            const loading = document.getElementById('loading');
            const errorMsg = document.getElementById('errorMsg');
            const resultContainer = document.getElementById('resultContainer');
            const originalImage = document.getElementById('originalImage');
            const resultImage = document.getElementById('resultImage');
            const fireCount = document.getElementById('fireCount');
            const humanCount = document.getElementById('humanCount');
            const smokeCount = document.getElementById('smokeCount');
            const totalCount = document.getElementById('totalCount');

            let selectedFile = null;

            dropArea.addEventListener('click', () => fileInput.click());

            fileInput.addEventListener('change', (e) => {
                if (e.target.files.length > 0) {
                    selectedFile = e.target.files[0];
                    const text = dropArea.querySelector('.upload-text');
                    text.textContent = `已选择: ${selectedFile.name}`;
                    const reader = new FileReader();
                    reader.onload = (e) => {
                        originalImage.src = e.target.result;
                    };
                    reader.readAsDataURL(selectedFile);
                }
            });

            dropArea.addEventListener('dragover', (e) => {
                e.preventDefault();
                dropArea.classList.add('dragover');
            });
            dropArea.addEventListener('dragleave', () => {
                dropArea.classList.remove('dragover');
            });
            dropArea.addEventListener('drop', (e) => {
                e.preventDefault();
                dropArea.classList.remove('dragover');
                if (e.dataTransfer.files.length > 0) {
                    selectedFile = e.dataTransfer.files[0];
                    const text = dropArea.querySelector('.upload-text');
                    text.textContent = `已选择: ${selectedFile.name}`;
                    const reader = new FileReader();
                    reader.onload = (e) => {
                        originalImage.src = e.target.result;
                    };
                    reader.readAsDataURL(selectedFile);
                    fileInput.files = e.dataTransfer.files;
                }
            });

            uploadBtn.addEventListener('click', async () => {
                if (!selectedFile) {
                    showError('请先选择一张图片');
                    return;
                }
                if (!selectedFile.type.startsWith('image/')) {
                    showError('请上传图片文件');
                    return;
                }
                if (selectedFile.size > 20 * 1024 * 1024) {
                    showError('图片大小不能超过20MB');
                    return;
                }

                errorMsg.classList.remove('show');
                loading.classList.add('show');
                resultContainer.classList.remove('show');
                uploadBtn.disabled = true;

                const formData = new FormData();
                formData.append('file', selectedFile);

                try {
                    const response = await fetch('/detect/upload', {
                        method: 'POST',
                        body: formData
                    });

                    const data = await response.json();

                    if (data.success) {
                        resultImage.src = 'data:image/jpeg;base64,' + data.result_image;
                        fireCount.textContent = data.fire_count || 0;
                        humanCount.textContent = data.human_count || 0;
                        smokeCount.textContent = data.smoke_count || 0;
                        totalCount.textContent = data.total_count || 0;
                        resultContainer.classList.add('show');
                    } else {
                        showError(data.message || '识别失败');
                    }
                } catch (err) {
                    showError('网络错误: ' + err.message);
                } finally {
                    loading.classList.remove('show');
                    uploadBtn.disabled = false;
                }
            });

            function showError(msg) {
                errorMsg.textContent = '❌ ' + msg;
                errorMsg.classList.add('show');
            }

            // 检查模型状态
            fetch('/health')
                .then(r => r.json())
                .then(data => {
                    if (data.model_loaded) {
                        document.getElementById('modelStatus').textContent = '✅ 模型已加载: ' + data.model_path;
                        document.getElementById('modelStatus').style.background = '#e8f5e9';
                    } else {
                        document.getElementById('modelStatus').textContent = '❌ 模型加载失败，请检查日志';
                        document.getElementById('modelStatus').style.background = '#fde8e8';
                    }
                });
        </script>
    </body>
    </html>
    """
    return HTMLResponse(content=html_content)


@app.post("/detect/upload")
async def detect_upload(file: UploadFile = File(...)):
    """上传图片并检测"""
    if model is None:
        return JSONResponse(status_code=503, content={
            "success": False,
            "message": "模型未加载，请检查模型文件是否存在"
        })

    try:
        # 保存上传的图片
        upload_dir = "uploads"
        os.makedirs(upload_dir, exist_ok=True)

        file_ext = os.path.splitext(file.filename)[1]
        file_name = f"{uuid.uuid4().hex}{file_ext}"
        file_path = os.path.join(upload_dir, file_name)

        content = await file.read()
        with open(file_path, "wb") as f:
            f.write(content)

        # 执行推理
        detections, error = run_inference(file_path)
        if error:
            return JSONResponse(status_code=500, content={
                "success": False,
                "message": f"推理失败: {error}"
            })

        if detections is None:
            detections = []

        # 统计各类别数量
        fire_count = sum(1 for d in detections if d["class_id"] == 0)
        human_count = sum(1 for d in detections if d["class_id"] == 1)
        smoke_count = sum(1 for d in detections if d["class_id"] == 2)

        # 绘制检测框
        result_image_base64 = draw_detections(file_path, detections)
        if result_image_base64 is None:
            return JSONResponse(status_code=500, content={
                "success": False,
                "message": "图片处理失败"
            })

        return {
            "success": True,
            "detections": detections,
            "total_count": len(detections),
            "fire_count": fire_count,
            "human_count": human_count,
            "smoke_count": smoke_count,
            "result_image": result_image_base64
        }

    except Exception as e:
        import traceback
        traceback.print_exc()
        return JSONResponse(status_code=500, content={
            "success": False,
            "message": f"识别失败: {str(e)}"
        })


@app.get("/health")
async def health():
    return {
        "status": "healthy" if model is not None else "failed",
        "model_loaded": model is not None,
        "model_path": MODEL_PATH,
        "classes": CLASS_NAMES
    }


if __name__ == "__main__":
    print("=" * 50)
    print("🌲 林区火情 YOLOv5 识别服务")
    print(f"📁 模型: {MODEL_PATH}")
    print(f"📋 类别: {CLASS_NAMES}")
    print(f"📦 模型加载状态: {'✅ 已加载' if model is not None else '❌ 未加载'}")
    print("=" * 50)
    print("🚀 服务启动中...")
    print("🌐 访问: http://localhost:8000")
    print("=" * 50)

    uvicorn.run(
        app,
        host="0.0.0.0",
        port=8000,
        log_level="info"
    )