#!/data/data/com.termux/files/usr/bin/bash
set -e

cd ~/needle3-test
MODEL_PATH="/data/data/com.termux/files/home/.ollama/models/blobs/sha256-74a4da8c9fdbcd15bd1f6d01d621410d31c6fc00986f5eb687824e7b93d7a9db"

echo "Starting llama-server for Qwen 2.5 0.5B on port 8084..."
nohup llama-server -m "$MODEL_PATH" --port 8084 -t 4 -c 1024 --host 127.0.0.1 > ~/needle3-test/qwen25_server.log 2>&1 &

echo "Waiting for llama-server to be ready..."
for i in 1 2 3 4 5 6 7 8 9 10 11 12 13 14 15 16 17 18 19 20; do
  if curl -s http://127.0.0.1:8084/health | grep -q '"ok"'; then
    echo "SERVER_READY"
    break
  fi
  sleep 2
done

echo "Running benchmark test suite..."
python -u bench_qwen25.py
