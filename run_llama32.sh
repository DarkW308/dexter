#!/data/data/com.termux/files/usr/bin/bash
set -e

cd ~/needle3-test
echo "Downloading Llama 3.2 1B Instruct GGUF..."
curl -L -C - -o ~/needle3-test/llama-3.2-1b-instruct.gguf https://huggingface.co/bartowski/Llama-3.2-1B-Instruct-GGUF/resolve/main/Llama-3.2-1B-Instruct-Q4_K_M.gguf

echo "Download completed. File details:"
ls -lh ~/needle3-test/llama-3.2-1b-instruct.gguf

echo "Starting llama-server for Llama 3.2 1B on port 8083..."
nohup llama-server -m ~/needle3-test/llama-3.2-1b-instruct.gguf --port 8083 -t 4 -c 1024 --host 127.0.0.1 > ~/needle3-test/llama32_server.log 2>&1 &

echo "Waiting for llama-server to be ready..."
for i in 1 2 3 4 5 6 7 8 9 10 11 12 13 14 15 16 17 18 19 20; do
  if curl -s http://127.0.0.1:8083/health | grep -q '"ok"'; then
    echo "SERVER_READY"
    break
  fi
  sleep 2
done

echo "Running benchmark test suite..."
python -u bench_llama32.py
