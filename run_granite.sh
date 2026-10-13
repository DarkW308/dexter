#!/data/data/com.termux/files/usr/bin/bash
set -e

echo "Waiting for curl download to complete..."
while ps -ef | grep -v grep | grep -q "curl -L -C"; do
  sleep 3
done

echo "Download completed. File details:"
ls -lh ~/needle3-test/granite-3.1-1b-a400m.gguf

echo "Starting llama-server for Granite 3.1 on port 8082..."
nohup llama-server -m ~/needle3-test/granite-3.1-1b-a400m.gguf --port 8082 -t 4 -c 1024 --host 127.0.0.1 > ~/needle3-test/granite_server.log 2>&1 &

echo "Waiting for llama-server to be ready..."
for i in 1 2 3 4 5 6 7 8 9 10 11 12 13 14 15 16 17 18 19 20; do
  if curl -s http://127.0.0.1:8082/health | grep -q '"ok"'; then
    echo "SERVER_READY"
    break
  fi
  sleep 2
done

echo "Running benchmark test suite..."
cd ~/needle3-test
python -u bench_granite.py
